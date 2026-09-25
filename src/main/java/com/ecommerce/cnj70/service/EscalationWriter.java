package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Escalation;

/**
 * Phase 2C — Escalation Writer (integration seam).
 *
 * <p>When a Moderator escalates a ReportCase, the act of escalation
 * is persisted to {@code Escalation} and also broadcast to this
 * seam so the Admin / Escalation backend (Phase 3A scope) can
 * pick it up.</p>
 *
 * <p>Phase 2C ships a default logging-only implementation; swapping
 * in the real Admin Escalation consumer is a Spring profile change
 * (Phase 2C §31).</p>
 */
public interface EscalationWriter {

    /**
     * Called after an Escalation document has been persisted locally.
     * <p>Implementations MUST NOT throw — failures should be logged
     * and surfaced as an admin warning, not surfaced to the user.</p>
     *
     * @param escalation the escalation that was just persisted
     */
    void notify(Escalation escalation);
}
