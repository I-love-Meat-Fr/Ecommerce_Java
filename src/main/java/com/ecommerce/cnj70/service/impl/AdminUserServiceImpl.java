package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AdminUserService;
import com.ecommerce.cnj70.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditLogService auditLogService;

    @Override
    public Page<User> listUsers(Pageable pageable, String q) {
        return listUsers(pageable, q, null, null);
    }

    @Override
    public Page<User> listUsers(Pageable pageable, String q, UserRole role, AccountStatus status) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasRole = (role != null);
        boolean hasStatus = (status != null);

        // Phase 12: User Search+Filter — chỉ search (không filter)
        if (!hasRole && !hasStatus) {
            if (hasQ) {
                return userRepository.searchByKeyword(q.trim(), pageable);
            }
            return userRepository.findAll(pageable);
        }

        // Không search — chỉ filter
        if (!hasQ) {
            if (hasRole && hasStatus) {
                return userRepository.findByRoleAndStatus(role, status, pageable);
            }
            if (hasRole) {
                return userRepository.findByRole(role, pageable);
            }
            return userRepository.findByStatus(status, pageable);
        }

        // Search + filter: search trên email/fullName + kết hợp role/status
        return searchUsersWithFilter(q.trim(), role, status, pageable);
    }

    /**
     * Tìm User theo keyword kết hợp role/status bằng MongoTemplate.
     */
    private Page<User> searchUsersWithFilter(String q, UserRole role, AccountStatus status, Pageable pageable) {
        Pattern emailPattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
        Pattern namePattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);

        Criteria searchCriteria = new Criteria().orOperator(
                Criteria.where("email").regex(emailPattern),
                Criteria.where("fullName").regex(namePattern)
        );

        long total = 0;
        Map<String, User> merged = new LinkedHashMap<>();

        if (role != null && status != null) {
            Criteria c = new Criteria().andOperator(searchCriteria,
                    Criteria.where("role").is(role.name()),
                    Criteria.where("status").is(status.name()));
            Query q1 = Query.query(c).with(pageable);
            long t1 = mongoTemplate.count(Query.query(c), User.class);
            total += t1;
            for (User u : mongoTemplate.find(q1, User.class)) merged.putIfAbsent(u.getId(), u);
        } else if (role != null) {
            Criteria c = new Criteria().andOperator(searchCriteria,
                    Criteria.where("role").is(role.name()));
            Query q1 = Query.query(c).with(pageable);
            long t1 = mongoTemplate.count(Query.query(c), User.class);
            total += t1;
            for (User u : mongoTemplate.find(q1, User.class)) merged.putIfAbsent(u.getId(), u);
        } else if (status != null) {
            Criteria c = new Criteria().andOperator(searchCriteria,
                    Criteria.where("status").is(status.name()));
            Query q1 = Query.query(c).with(pageable);
            long t1 = mongoTemplate.count(Query.query(c), User.class);
            total += t1;
            for (User u : mongoTemplate.find(q1, User.class)) merged.putIfAbsent(u.getId(), u);
        }

        List<User> content = List.copyOf(merged.values());
        return new PageImpl<>(content, pageable, Math.max(total, content.size()));
    }

    @Override
    public User getUserById(String id) {
        // Phase 3 — findById(ObjectId) của Spring Data MongoDB không match với String _id
        // (đã verify runtime ở session trước). Dùng raw Document query để đảm bảo match.
        org.bson.Document raw = mongoTemplate.getCollection("users")
                .find(new org.bson.Document("_id", id))
                .first();
        if (raw == null) {
            throw new ResourceNotFoundException("User", "id", id);
        }
        // Phase 3 — khi _id là String, Spring Data converter KHÔNG tự map vào User.id
        // (mặc định _id là ObjectId). Set thẳng _id vào Document trước khi read.
        raw.put("_id", id);
        // Đảm bảo có _class discriminator để converter nhận diện class
        if (!raw.containsKey("_class")) {
            raw.put("_class", User.class.getName());
        }
        User user = mongoTemplate.getConverter().read(User.class, raw);
        if (user.getId() == null) {
            user.setId(id);
        }
        return user;
    }

    /**
     * Phase 3 — Update User status dùng raw MongoDB updateOne để tránh
     * `mongoTemplate.save` insert duplicate (khi _id String mapping sai).
     */
    private void updateUserStatusRaw(String userId, AccountStatus newStatus) {
        org.bson.Document filter = new org.bson.Document("_id", userId);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("status", newStatus.name())
                        .append("updatedAt", java.time.LocalDateTime.now()));
        mongoTemplate.getCollection("users").updateOne(filter, update);
    }

    @Override
    public void lockUser(String id, String currentUserId) {
        User user = getUserById(id);

        // Phase 3 §1.2 / §3.9 — Self-Action Protection cho cả ADMIN và MODERATOR.
        // Admin không được tự khóa chính mình; Moderator cũng không được tự khóa chính mình.
        if (currentUserId != null
                && currentUserId.equals(user.getId())
                && (user.getRole() == UserRole.ADMIN
                    || user.getRole() == UserRole.MODERATOR)) {
            throw new BusinessException(
                    "Bạn không thể tự khóa tài khoản "
                            + user.getRole().name()
                            + " của chính mình");
        }

        AccountStatus beforeStatus = user.getStatus();

        if (beforeStatus == AccountStatus.LOCKED) {
            log.info("AdminUserService.lockUser: user {} already LOCKED, skip", id);
            return;
        }

        user.setStatus(AccountStatus.LOCKED);
        // Phase 3 — dùng raw updateOne để tránh save() insert duplicate
        // khi _id String mapping không match.
        updateUserStatusRaw(id, AccountStatus.LOCKED);

        // ===== TASK #24: AuditLog =====
        try {
            auditLogService.logWarning(
                    AuditAction.USER_LOCKED,
                    "USER",
                    id,
                    currentUserId,
                    null,
                    "ADMIN",
                    "Admin khóa tài khoản: " + user.getEmail() + " (" + beforeStatus + " → LOCKED)"
            );
        } catch (RuntimeException auditEx) {
            log.warn("AuditLog write failed for lockUser({}): {}", id, auditEx.getMessage());
        }

        log.info("AdminUserService.lockUser: user {} locked by {}", id, currentUserId);
    }

    @Override
    public void unlockUser(String id) {
        User user = getUserById(id);

        // Phase 3 §3.9 — Self-Action Protection cho unlock (nếu Admin/Moderator unlock chính mình
        // vẫn OK vì đó là restore access; nhưng không có self-action block cho unlock).
        AccountStatus beforeStatus = user.getStatus();

        if (beforeStatus == AccountStatus.ACTIVE) {
            log.info("AdminUserService.unlockUser: user {} already ACTIVE, skip", id);
            return;
        }

        user.setStatus(AccountStatus.ACTIVE);
        // Phase 3 — dùng raw updateOne để tránh save() insert duplicate.
        updateUserStatusRaw(id, AccountStatus.ACTIVE);

        // ===== TASK #24: AuditLog =====
        try {
            auditLogService.logInfo(
                    AuditAction.USER_UNLOCKED,
                    "USER",
                    id,
                    null,
                    null,
                    "ADMIN",
                    "Admin mở khóa tài khoản: " + user.getEmail() + " (" + beforeStatus + " → ACTIVE)"
            );
        } catch (RuntimeException auditEx) {
            log.warn("AuditLog write failed for unlockUser({}): {}", id, auditEx.getMessage());
        }

        log.info("AdminUserService.unlockUser: user {} unlocked", id);
    }

    @Override
    public AccountStatus getCurrentStatus(String id) {
        return getUserById(id).getStatus();
    }

    @Override
    public void updateUserStatus(String id, AccountStatus newStatus, String currentUserId) {
        if (newStatus == null) {
            throw new BusinessException("Trạng thái mới không được để trống");
        }

        User user = getUserById(id);
        AccountStatus beforeStatus = user.getStatus();

        // Phase 3 §3.7 — Self-Action Protection cho Edit Status.
        // Không cho đổi status của chính mình nếu status hiện tại = ACTIVE
        // (Admin/Moderator không được tự khóa/vô hiệu hóa chính mình qua Edit).
        if (currentUserId != null
                && currentUserId.equals(user.getId())
                && beforeStatus == AccountStatus.ACTIVE
                && newStatus != AccountStatus.ACTIVE
                && (user.getRole() == UserRole.ADMIN
                    || user.getRole() == UserRole.MODERATOR)) {
            throw new BusinessException(
                    "Bạn không thể tự đổi trạng thái "
                            + user.getRole().name()
                            + " của chính mình khi đang ACTIVE");
        }

        // Phase 3 §3.8 — Validation: no-op
        if (beforeStatus == newStatus) {
            throw new BusinessException(
                    "Trạng thái mới giống trạng thái hiện tại ("
                            + beforeStatus + ") — không có thay đổi");
        }

        user.setStatus(newStatus);
        // Phase 3 — dùng raw updateOne để tránh save() insert duplicate.
        updateUserStatusRaw(id, newStatus);

        // ===== TASK #24: AuditLog =====
        try {
            auditLogService.logInfo(
                    AuditAction.ADMIN_ACTION,
                    "USER",
                    id,
                    currentUserId,
                    null,
                    "ADMIN",
                    "Admin đổi trạng thái user " + user.getEmail()
                            + " (" + beforeStatus + " → " + newStatus + ")"
            );
        } catch (RuntimeException auditEx) {
            log.warn("AuditLog write failed for updateUserStatus({}): {}", id, auditEx.getMessage());
        }

        log.info("AdminUserService.updateUserStatus: user {} status {} → {} by {}",
                id, beforeStatus, newStatus, currentUserId);
    }
}
