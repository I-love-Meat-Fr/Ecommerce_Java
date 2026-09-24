package com.ecommerce.cnj70.enums;

/**
 * Phase 3 — Complaint status (state machine).
 *
 * <p>State transitions:</p>
 * <pre>
 * OPEN ── Vendor Response ──▶ VENDOR_RESPONDED ──┬──▶ RESOLVED (Level 0 agreement)
 *                                                └──▶ ESCALATED (Level 0 fail)
 *                                                        │
 *                                                        ▼
 *                                                  MODERATOR_REVIEW
 *                                                        │
 *                                          ┌─────────────┼─────────────┐
 *                                          ▼             ▼             ▼
 *                                       RESOLVED    ESCALATED_L2   (more evidence)
 *                                                       │
 *                                                       ▼
 *                                                 ADMIN_REVIEW
 *                                                       │
 *                                                       ▼
 *                                                  RESOLVED / CLOSED
 * </pre>
 *
 * <p>Terminal states: {@link #RESOLVED}, {@link #CLOSED}.</p>
 */
public enum ComplaintStatus {
    OPEN,
    VENDOR_RESPONDED,
    ESCALATED,
    MODERATOR_REVIEW,
    ESCALATED_L2,
    ADMIN_REVIEW,
    RESOLVED,
    CLOSED,
    REJECTED;

    public boolean isTerminal() {
        return this == RESOLVED || this == CLOSED || this == REJECTED;
    }

    public boolean isOpenLevel() {
        return this == OPEN || this == VENDOR_RESPONDED;
    }

    public boolean isInModeration() {
        return this == ESCALATED || this == MODERATOR_REVIEW;
    }

    public boolean isInAdmin() {
        return this == ESCALATED_L2 || this == ADMIN_REVIEW;
    }
}
