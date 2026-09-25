package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.AuditLog;
import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.AuditLogEntryRepository;
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
    // ===== BUG FIX: Mirror write vào AuditLogEntry =====
    // Admin /admin/audit HTML page đọc từ collection `audit_logs` qua AuditLogEntry schema
    // (AdminAuditLogServiceImpl + AdminAuditLogController + audit-list.html / audit-detail.html).
    // AuditLogService trước đây chỉ ghi vào AuditLog (schema khác) → /admin/audit hiển thị trống
    // vì field mapping không khớp. Mirror write đảm bảo cả 2 schema đều có data, không phá
    // vỡ API hiện tại (vẫn trả AuditLog cho caller như cũ).
    private final AuditLogEntryRepository auditLogEntryRepository;

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

        // ===== BUG FIX: Mirror ghi sang AuditLogEntry (cùng collection `audit_logs`) =====
        // Đảm bảo Admin /admin/audit đọc được dữ liệu đúng schema.
        try {
            AuditLogEntry mirror = AuditLogEntry.builder()
                    .actorId(actorId)
                    .actorEmail(actorUsername)
                    .role(parseRole(actorRole))
                    .action(action != null ? action.name() : null)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .reason(reason)
                    .severity(severity != null ? severity.name() : null)
                    .createdAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : java.time.LocalDateTime.now())
                    .build();
            auditLogEntryRepository.save(mirror);
        } catch (Exception ex) {
            // Không để audit mirror phá vỡ business flow chính
            log.warn("AuditLogEntry mirror write failed (action={} resourceId={}): {}",
                    action, resourceId, ex.getMessage());
        }

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

    private static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) return null;
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
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
