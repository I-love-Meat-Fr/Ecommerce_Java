package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.service.AuditEventWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Phase 2A — Log-only AuditEventWriter.
 *
 * <p>Active when no real AuditLog backend is wired in. Logs audit events
 * at INFO level. This is NOT a production fake — it is a documented
 * fallback that records the event in the application log so it is not
 * silently dropped.</p>
 *
 * <p>Once the AuditLog backend (Phase 3A / §3C) provides a real
 * implementation, this no-op will be replaced by a bean with profile
 * {@code !audit-log}.</p>
 */
@Slf4j
@Component
@Profile("!audit-log")
public class LoggingAuditEventWriter implements AuditEventWriter {

    @Override
    public void write(AuditEvent event) {
        if (event == null) {
            return;
        }
        log.info("AUDIT action={} actor={} role={} resourceType={} resourceId={} reason={}",
                event.getAction(),
                event.getActorEmail(),
                event.getRole(),
                event.getResourceType(),
                event.getResourceId(),
                event.getReason());
    }
}
