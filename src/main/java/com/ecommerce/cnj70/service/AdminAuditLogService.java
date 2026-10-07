package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Phase 3C — Admin read-only AuditLog query service.
 *
 * <p>Contract: Admin can VIEW / SEARCH / FILTER / PAGINATE audit events.
 * This service is READ-ONLY. No mutation of audit records is permitted.</p>
 *
 * <h3>Immutable</h3>
 * All methods are read-only. No update or delete operations exist.
 * Audit records are append-only by design.</p>
 *
 * <h3>Data source</h3>
 * Backed by MongoDB collection {@code audit_logs} (single collection,
 * two schemas: {@link com.ecommerce.cnj70.document.AuditLog} for writes,
 * {@link AuditLogEntry} for Admin read queries). The collection name
 * MUST stay {@code audit_logs} — never {@code audit_log}.
 */
public interface AdminAuditLogService {

    Page<AuditLogEntry> listAll(Pageable pageable);

    Page<AuditLogEntry> listByActor(String actorId, Pageable pageable);

    Page<AuditLogEntry> listByRole(UserRole role, Pageable pageable);

    Page<AuditLogEntry> listByAction(Collection<String> actions, Pageable pageable);

    Page<AuditLogEntry> listByResource(String resourceType, String resourceId, Pageable pageable);

    Page<AuditLogEntry> listByDateRange(LocalDateTime from, LocalDateTime to,
                                         UserRole role, String actorId,
                                         Collection<String> actions,
                                         Pageable pageable);

    /**
     * Combined filter — any subset of {@code role}, {@code actorId},
     * {@code action}, {@code severity}, {@code resourceType},
     * {@code resourceId}, {@code from}, {@code to} can be supplied.
     * Null / blank parameters are ignored (no constraint added).
     *
     * <p>Implemented at the query layer via {@code MongoTemplate} so
     * that any combination of filters works without changing the data
     * source.</p>
     */
    Page<AuditLogEntry> search(LocalDateTime from,
                                LocalDateTime to,
                                UserRole role,
                                String actorId,
                                Collection<String> actions,
                                AuditSeverity severity,
                                String resourceType,
                                String resourceId,
                                Pageable pageable);

    AuditLogEntry getDetail(String id);
}