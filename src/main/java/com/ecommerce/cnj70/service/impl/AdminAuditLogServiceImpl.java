package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.AuditLogEntryRepository;
import com.ecommerce.cnj70.service.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * Phase 3C — Admin AuditLog query service (read-only).
 *
 * <p>Implements {@link AdminAuditLogService}. All operations are
 * read-only — no mutation of audit records is permitted (Phase 3C §40).</p>
 *
 * <h3>Immutable</h3>
 * No update or delete methods exist. Audit records are append-only.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AuditLogEntryRepository auditLogRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<AuditLogEntry> listAll(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Page<AuditLogEntry> listByActor(String actorId, Pageable pageable) {
        if (!StringUtils.hasText(actorId)) {
            return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable);
    }

    @Override
    public Page<AuditLogEntry> listByRole(UserRole role, Pageable pageable) {
        if (role == null) {
            return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return auditLogRepository.findByRoleOrderByCreatedAtDesc(role, pageable);
    }

    @Override
    public Page<AuditLogEntry> listByAction(Collection<String> actions, Pageable pageable) {
        if (actions == null || actions.isEmpty()) {
            return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return auditLogRepository.findByActionInOrderByCreatedAtDesc(new java.util.ArrayList<>(actions), pageable);
    }

    @Override
    public Page<AuditLogEntry> listByResource(String resourceType, String resourceId, Pageable pageable) {
        if (!StringUtils.hasText(resourceType) || !StringUtils.hasText(resourceId)) {
            return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
                resourceType, resourceId, pageable);
    }

    @Override
    public Page<AuditLogEntry> listByDateRange(LocalDateTime from, LocalDateTime to,
                                                 UserRole role, String actorId,
                                                 Collection<String> actions,
                                                 Pageable pageable) {
        LocalDateTime fromTs = (from != null) ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime toTs   = (to   != null) ? to   : LocalDateTime.now();

        boolean hasRole    = role != null;
        boolean hasActor  = StringUtils.hasText(actorId);
        boolean hasAction = actions != null && !actions.isEmpty();

        if (hasRole) {
            return auditLogRepository.findByCreatedAtBetweenAndRoleInOrderByCreatedAtDesc(
                    fromTs, toTs, Collections.singleton(role), pageable);
        } else if (hasAction) {
        return auditLogRepository.findByCreatedAtBetweenAndActionInOrderByCreatedAtDesc(
                fromTs, toTs, new java.util.ArrayList<>(actions), pageable);
        } else if (hasActor) {
            return auditLogRepository.findByCreatedAtBetweenAndActorIdOrderByCreatedAtDesc(
                    fromTs, toTs, actorId, pageable);
        } else {
            return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromTs, toTs, pageable);
        }
    }

    @Override
    public AuditLogEntry getDetail(String id) {
        if (!StringUtils.hasText(id)) {
            throw new ResourceNotFoundException("AuditLog ID không hợp lệ");
        }
        // Phase 7 fix: String _id retrieval — same pattern as Phase 4/5/6 fix.
        // Use raw mongoTemplate query for reliable String _id lookup.
        Document raw = mongoTemplate.getCollection("audit_logs")
                .find(new Document("_id", id))
                .first();
        if (raw == null) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy AuditLog với ID: " + id);
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", AuditLogEntry.class.getName());
        }
        return mongoTemplate.getConverter().read(AuditLogEntry.class, raw);
    }
}
