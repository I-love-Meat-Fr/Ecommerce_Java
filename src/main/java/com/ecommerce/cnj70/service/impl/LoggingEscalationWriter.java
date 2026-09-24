package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.service.EscalationWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Phase 2C — Logging-only Escalation Writer.
 *
 * <p>Active when the real Admin Escalation consumer (Phase 3A) is not
 * yet wired in. Logs the escalation for audit purposes only — does
 * NOT create fake admin tasks, does NOT trigger enforcement. This
 * matches Phase 2C §22 (Moderator never performs Admin enforcement).</p>
 */
@Slf4j
@Component
@Profile("!escalation-writer")
public class LoggingEscalationWriter implements EscalationWriter {

    public LoggingEscalationWriter() {
        log.info("LoggingEscalationWriter active — Phase 3A Admin Escalation consumer not wired in. " +
                "Escalations will be persisted to the 'escalations' collection only (no Admin notification).");
    }

    @Override
    public void notify(Escalation escalation) {
        log.info("Escalation persisted (Phase 3A consumer not wired): " +
                        "caseId={} resource={}/{} severity={} moderator={}",
                escalation.getReportCaseId(),
                escalation.getResourceType(),
                escalation.getResourceId(),
                escalation.getSeverity(),
                escalation.getModeratorEmail());
    }
}
