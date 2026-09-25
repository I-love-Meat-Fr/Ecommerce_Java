package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.AuditLog;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * TASK #23/#24 — AuditLogService.
 *
 * Ghi log mọi compliance action: moderation, KYC, admin, escalation, auto flag.
 */
public interface AuditLogService {

    /**
     * Ghi log action.
     */
    AuditLog log(AuditAction action, String resourceType, String resourceId,
                 String actorId, String actorUsername, String actorRole,
                 AuditSeverity severity, String reason, Map<String, Object> metadata);

    /** Convenience: log INFO action */
    AuditLog logInfo(AuditAction action, String resourceType, String resourceId,
                     String actorId, String actorUsername, String actorRole,
                     String reason);

    /** Convenience: log INFO action with metadata */
    AuditLog logInfo(AuditAction action, String resourceType, String resourceId,
                     String actorId, String actorUsername, String actorRole,
                     String reason, Map<String, Object> metadata);

    /** Convenience: log WARNING action */
    AuditLog logWarning(AuditAction action, String resourceType, String resourceId,
                        String actorId, String actorUsername, String actorRole,
                        String reason);

    /** Convenience: log WARNING action with metadata */
    AuditLog logWarning(AuditAction action, String resourceType, String resourceId,
                        String actorId, String actorUsername, String actorRole,
                        String reason, Map<String, Object> metadata);

    /** Convenience: log CRITICAL action */
    AuditLog logCritical(AuditAction action, String resourceType, String resourceId,
                         String actorId, String actorUsername, String actorRole,
                         String reason);

    /** Convenience: log CRITICAL action with metadata */
    AuditLog logCritical(AuditAction action, String resourceType, String resourceId,
                         String actorId, String actorUsername, String actorRole,
                         String reason, Map<String, Object> metadata);

    /** Truy vết lịch sử action của một resource */
    Page<AuditLog> findByResource(String resourceType, String resourceId, Pageable pageable);

    /** Truy vết lịch sử action của một actor */
    Page<AuditLog> findByActor(String actorId, Pageable pageable);

    /** Filter theo action */
    Page<AuditLog> findByAction(AuditAction action, Pageable pageable);

    /** Filter theo severity */
    Page<AuditLog> findBySeverity(AuditSeverity severity, Pageable pageable);
}
