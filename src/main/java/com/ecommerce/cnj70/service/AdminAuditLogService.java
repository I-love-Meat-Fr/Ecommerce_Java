package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.AuditLogEntry;
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

    AuditLogEntry getDetail(String id);
}
