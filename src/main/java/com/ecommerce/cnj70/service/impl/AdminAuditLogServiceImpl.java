package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.AuditLogEntryRepository;
import com.ecommerce.cnj70.service.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Phase 3C — Admin AuditLog query service (read-only).
 *
 * <p>Implements {@link AdminAuditLogService}. All operations are
 * read-only — no mutation of audit records is permitted (Phase 3C §40).</p>
 *
 * <h3>Immutable</h3>
 * No update or delete methods exist. Audit records are append-only.</p>
 *
 * <h3>Data source</h3>
 * Reads from MongoDB collection {@code audit_logs}. The collection
 * name is fixed by product spec; do NOT rename to {@code audit_log}
 * (no-s plural) and do NOT migrate records out of {@code audit_logs}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    /**
     * Collection name (single source of truth for the Admin read view).
     * Hard-coded literal to keep the contract obvious — the value must
     * match {@link com.ecommerce.cnj70.document.AuditLog} and
     * {@link com.ecommerce.cnj70.document.AuditLogEntry}.
     */
    private static final String COLLECTION_AUDIT_LOGS = "audit_logs";

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
        return auditLogRepository.findByActionInOrderByCreatedAtDesc(new ArrayList<>(actions), pageable);
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
                fromTs, toTs, new ArrayList<>(actions), pageable);
        } else if (hasActor) {
            return auditLogRepository.findByCreatedAtBetweenAndActorIdOrderByCreatedAtDesc(
                    fromTs, toTs, actorId, pageable);
        } else {
            return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(
                    fromTs, toTs, pageable);
        }
    }

    /**
     * Combined filter — all supplied (non-blank) parameters are AND-ed
     * together and applied against the {@code audit_logs} collection
     * via {@link MongoTemplate}. Null / blank values are ignored.
     *
     * <p>Done at the <b>query</b> layer (no schema or data-source
     * changes). This preserves the {@code audit_logs} collection
     * contract — we are only refining how records are fetched.</p>
     */
    @Override
    public Page<AuditLogEntry> search(LocalDateTime from,
                                       LocalDateTime to,
                                       UserRole role,
                                       String actorId,
                                       Collection<String> actions,
                                       AuditSeverity severity,
                                       String resourceType,
                                       String resourceId,
                                       Pageable pageable) {
        List<Criteria> clauses = new ArrayList<>();

        if (from != null || to != null) {
            LocalDateTime fromTs = (from != null) ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime toTs   = (to   != null) ? to   : LocalDateTime.now();
            clauses.add(Criteria.where("createdAt").gte(fromTs).lte(toTs));
        }
        if (role != null) {
            clauses.add(Criteria.where("role").is(role));
        }
        if (StringUtils.hasText(actorId)) {
            clauses.add(Criteria.where("actorId").is(actorId));
        }
        if (actions != null && !actions.isEmpty()) {
            // action is stored as a String name (e.g. "USER_LOCKED") in
            // AuditLogEntry; use $in so the caller can supply a single
            // action or many.
            clauses.add(Criteria.where("action").in(new ArrayList<>(actions)));
        }
        if (severity != null) {
            // AuditLogEntry.severity is a String (INFO/WARNING/CRITICAL).
            clauses.add(Criteria.where("severity").is(severity.name()));
        }
        if (StringUtils.hasText(resourceType)) {
            clauses.add(Criteria.where("resourceType").is(resourceType));
        }
        if (StringUtils.hasText(resourceId)) {
            clauses.add(Criteria.where("resourceId").is(resourceId));
        }

        Criteria criteria = clauses.isEmpty()
                ? new Criteria()
                : new Criteria().andOperator(clauses.toArray(new Criteria[0]));

        Query query = Query.query(criteria).with(pageable);
        // Total count uses the same criteria but no skip/limit.
        Query countQuery = Query.query(criteria);

        long total = mongoTemplate.count(countQuery, AuditLogEntry.class, COLLECTION_AUDIT_LOGS);

        // Read as raw Documents then convert with _class discriminator fix.
        // This mirrors the pattern used in getDetail() — necessary because
        // seed data (insertAll) and some legacy writes do not set _class,
        // so the plain mongoTemplate.find() would fail to map role/actorEmail.
        List<AuditLogEntry> content = new ArrayList<>();
        for (Document raw : mongoTemplate.find(query, Document.class, COLLECTION_AUDIT_LOGS)) {
            if (!raw.containsKey("_class")) {
                raw.put("_class", AuditLogEntry.class.getName());
            }
            content.add(mongoTemplate.getConverter().read(AuditLogEntry.class, raw));
        }

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public AuditLogEntry getDetail(String id) {
        if (!StringUtils.hasText(id)) {
            throw new ResourceNotFoundException("AuditLog ID không hợp lệ");
        }
        // Phase 7 fix: String _id retrieval — same pattern as Phase 4/5/6 fix.
        // Use raw mongoTemplate query for reliable String _id lookup.
        Document raw = mongoTemplate.getCollection(COLLECTION_AUDIT_LOGS)
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