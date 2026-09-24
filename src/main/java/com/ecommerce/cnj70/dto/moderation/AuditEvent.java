package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2A — Audit Event DTO (integration seam).
 *
 * <p>Carries audit information from Phase 2A Moderator actions to the
 * AuditLog backend (when connected). Field list mirrors Gate 0 §14
 * AuditLog contract (actorId, actorEmail, role, action, resourceType,
 * resourceId, reason, severity, before, after, ip, createdAt).</p>
 *
 * <p>Phase 2A never persists AuditEvent directly — it always goes
 * through {@code AuditEventWriter}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    private String actorId;
    private String actorEmail;
    private UserRole role;
    private String action;
    private String resourceType;
    private String resourceId;
    private String reason;
    private String severity;
    private String before;
    private String after;
    private String ip;
    private LocalDateTime createdAt;
}
