package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 3C — Admin read-only query repository for {@link AuditLogEntry}.
 *
 * <p>Uses the {@code audit_logs} collection. The data is written by
 * {@code AuditEventWriter} integration seam; this repository is the
 * Admin read view.</p>
 */
@Repository
public interface AuditLogEntryRepository extends MongoRepository<AuditLogEntry, String> {

    Page<AuditLogEntry> findByActorIdOrderByCreatedAtDesc(String actorId, Pageable pageable);

    Page<AuditLogEntry> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLogEntry> findByRoleOrderByCreatedAtDesc(UserRole role, Pageable pageable);

    Page<AuditLogEntry> findByRoleOrderByCreatedAtDesc(String role, Pageable pageable);

    Page<AuditLogEntry> findByActionInOrderByCreatedAtDesc(List<String> actions, Pageable pageable);

    Page<AuditLogEntry> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            String resourceType, String resourceId, Pageable pageable);

    Page<AuditLogEntry> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<AuditLogEntry> findByCreatedAtBetweenAndActorIdOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, String actorId, Pageable pageable);

    Page<AuditLogEntry> findByCreatedAtBetweenAndActionInOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, java.util.List<String> actions, Pageable pageable);

    Page<AuditLogEntry> findByCreatedAtBetweenAndRoleInOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to, java.util.Collection<UserRole> roles, Pageable pageable);
}
