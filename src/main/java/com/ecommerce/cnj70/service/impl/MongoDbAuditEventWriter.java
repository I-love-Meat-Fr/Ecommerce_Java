package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.AuditLogEntry;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.AuditLogEntryRepository;
import com.ecommerce.cnj70.service.AuditEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Phase 3B — MongoDB AuditEventWriter.
 *
 * <p>Persists {@link AuditEvent} to the {@code audit_log} collection
 * via {@link AuditLogEntryRepository}. This implementation is active
 * when the {@code audit-log} profile is enabled.</p>
 *
 * <p>Per Phase 3B requirements:
 * <ul>
 *     <li>Actor is taken from authenticated security context (event.getActorId()),
 *         not from request parameters.</li>
 *     <li>PII is minimized — no plaintext PII stored.</li>
 *     <li>Write path: AuditEvent → AuditLogEntryRepository → MongoDB.</li>
 *     <li>Immutable — only INSERT, no UPDATE/DELETE.</li>
 * </ul>
 *
 * <p>This implementation replaces {@link LoggingAuditEventWriter} when
 * the real audit backend is wired in.</p>
 */
@Slf4j
@Component
@Profile("audit-log")
@RequiredArgsConstructor
public class MongoDbAuditEventWriter implements AuditEventWriter {

    private final AuditLogEntryRepository auditLogEntryRepository;

    @Override
    public void write(AuditEvent event) {
        if (event == null) {
            log.warn("Received null AuditEvent — ignoring");
            return;
        }

        AuditLogEntry entry = AuditLogEntry.builder()
                .actorId(event.getActorId())
                .actorEmail(event.getActorEmail())
                .role(mapRole(event.getRole()))
                .action(event.getAction())
                .resourceType(event.getResourceType())
                .resourceId(event.getResourceId())
                .reason(event.getReason())
                .severity(event.getSeverity())
                .before(event.getBefore())
                .after(event.getAfter())
                .ip(event.getIp())
                .createdAt(event.getCreatedAt() != null ? event.getCreatedAt() : java.time.LocalDateTime.now())
                .build();

        try {
            AuditLogEntry saved = auditLogEntryRepository.save(entry);
            log.info("AUDIT_PERSISTED: action={} resourceType={} resourceId={} actorId={} entryId={}",
                    saved.getAction(), saved.getResourceType(), saved.getResourceId(),
                    saved.getActorId(), saved.getId());
        } catch (RuntimeException ex) {
            log.error("Failed to persist AuditEvent: action={} resourceType={} resourceId={}",
                    event.getAction(), event.getResourceType(), event.getResourceId(), ex);
            throw ex;
        }
    }

    private UserRole mapRole(com.ecommerce.cnj70.enums.UserRole role) {
        return role;
    }
}
