package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.AuditLog;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.repository.AuditLogRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLog log(AuditAction action, String resourceType, String resourceId,
                        String actorId, String actorUsername, String actorRole,
                        AuditSeverity severity, String reason, Map<String, Object> metadata) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .actorId(actorId)
                .actorUsername(actorUsername)
                .actorRole(actorRole)
                .severity(severity != null ? severity : AuditSeverity.INFO)
                .reason(reason)
                .metadata(metadata != null ? metadata : Map.of())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);

        // Log ra console với mức severity tương ứng
        String msg = String.format(
                "[AUDIT] %s | actor=%s(%s) | resource=%s:%s | severity=%s | reason=%s",
                action, actorUsername, actorRole, resourceType, resourceId, severity, reason);
        switch (severity) {
            case CRITICAL -> log.error(msg);
            case WARNING -> log.warn(msg);
            default -> log.info(msg);
        }

        return saved;
    }

    @Override
    public AuditLog logInfo(AuditAction action, String resourceType, String resourceId,
                            String actorId, String actorUsername, String actorRole,
                            String reason) {
        return log(action, resourceType, resourceId, actorId, actorUsername, actorRole,
                AuditSeverity.INFO, reason, Map.of());
    }

    @Override
    public AuditLog logInfo(AuditAction action, String resourceType, String resourceId,
                            String actorId, String actorUsername, String actorRole,
                            String reason, Map<String, Object> metadata) {
        return log(action, resourceType, resourceId, actorId, actorUsername, actorRole,
                AuditSeverity.INFO, reason, metadata);
    }

    @Override
    public AuditLog logWarning(AuditAction action, String resourceType, String resourceId,
                               String actorId, String actorUsername, String actorRole,
                               String reason) {
        return log(action, resourceType, resourceId, actorId, actorUsername, actorRole,
                AuditSeverity.WARNING, reason, Map.of());
    }

    @Override
    public AuditLog logWarning(AuditAction action, String resourceType, String resourceId,
                               String actorId, String actorUsername, String actorRole,
                               String reason, Map<String, Object> metadata) {
        return log(action, resourceType, resourceId, actorId, actorUsername, actorRole,
                AuditSeverity.WARNING, reason, metadata);
    }

    @Override
    public AuditLog logCritical(AuditAction action, String resourceType, String resourceId,
                                String actorId, String actorUsername, String actorRole,
                                String reason) {
        return log(action, resourceType, resourceId, actorId, actorUsername, actorRole,
                AuditSeverity.CRITICAL, reason, Map.of());
    }

    @Override
    public AuditLog logCritical(AuditAction action, String resourceType, String resourceId,
                                String actorId, String actorUsername, String actorRole,
                                String reason, Map<String, Object> metadata) {
        return log(action, resourceType, resourceId, actorId, actorUsername, actorRole,
                AuditSeverity.CRITICAL, reason, metadata);
    }

    @Override
    public Page<AuditLog> findByResource(String resourceType, String resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId, pageable);
    }

    @Override
    public Page<AuditLog> findByActor(String actorId, Pageable pageable) {
        return auditLogRepository.findByActorId(actorId, pageable);
    }

    @Override
    public Page<AuditLog> findByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }

    @Override
    public Page<AuditLog> findBySeverity(AuditSeverity severity, Pageable pageable) {
        return auditLogRepository.findBySeverity(severity, pageable);
    }
}
