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
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", id));
    }

    @Override
    public void lockUser(String id, String currentUserId) {
        User user = getUserById(id);

        if (user.getRole() == UserRole.ADMIN
                && currentUserId != null
                && currentUserId.equals(user.getId())) {
            throw new BusinessException(
                    "Bạn không thể tự khóa tài khoản ADMIN của chính mình");
        }

        AccountStatus beforeStatus = user.getStatus();

        if (beforeStatus == AccountStatus.LOCKED) {
            log.info("AdminUserService.lockUser: user {} already LOCKED, skip", id);
            return;
        }

        user.setStatus(AccountStatus.LOCKED);
        userRepository.save(user);

        // ===== TASK #24: AuditLog =====
        auditLogService.logWarning(
                AuditAction.USER_LOCKED,
                "USER",
                id,
                currentUserId,
                null,
                "ADMIN",
                "Admin khóa tài khoản: " + user.getEmail() + " (" + beforeStatus + " → LOCKED)"
        );

        log.info("AdminUserService.lockUser: user {} locked by {}", id, currentUserId);
    }

    @Override
    public void unlockUser(String id) {
        User user = getUserById(id);

        AccountStatus beforeStatus = user.getStatus();

        if (beforeStatus == AccountStatus.ACTIVE) {
            log.info("AdminUserService.unlockUser: user {} already ACTIVE, skip", id);
            return;
        }

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);

        // ===== TASK #24: AuditLog =====
        auditLogService.logInfo(
                AuditAction.USER_UNLOCKED,
                "USER",
                id,
                null,
                null,
                "ADMIN",
                "Admin mở khóa tài khoản: " + user.getEmail() + " (" + beforeStatus + " → ACTIVE)"
        );

        log.info("AdminUserService.unlockUser: user {} unlocked", id);
    }

    @Override
    public AccountStatus getCurrentStatus(String id) {
        return getUserById(id).getStatus();
    }
}
