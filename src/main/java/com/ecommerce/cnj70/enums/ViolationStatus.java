package com.ecommerce.cnj70.enums;

/**
 * Phase 3C — Violation lifecycle status.
 *
 * <p>Mirrors what the Violation backend returns. Phase 3C does not
 * create new statuses; it uses exactly what the backend contract defines.
 * If the backend uses different labels, they are used verbatim.</p>
 *
 * <p>Per Phase 3C §26 — these are PROCESSING states, distinct from
 * {@link ViolationAction} (the enforcement outcome) and
 * {@link ViolationSeverity} (the severity level).</p>
 */
public enum ViolationStatus {
    /** Violation detected, awaiting Moderator / Admin review. */
    OPEN,
    /** Violation is under review by Moderator or Admin. */
    UNDER_REVIEW,
    /** Violation resolved — enforcement applied or issue addressed. */
    RESOLVED,
    /** Vendor appealed the violation decision. */
    APPEALED,
    /** Violation dismissed after review (no enforcement). */
    DISMISSED;

    public boolean isTerminal() {
        return this == RESOLVED || this == DISMISSED;
    }
}
