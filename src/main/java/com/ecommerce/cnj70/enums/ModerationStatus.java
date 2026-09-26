package com.ecommerce.cnj70.enums;

/**
 * Phase 2A — Product Moderation Status.
 *
 * <p>Orthogonal to {@link ProductStatus}. {@link ProductStatus} controls
 * public-facing visibility (DRAFT/ACTIVE/OUT_OF_STOCK/HIDDEN) while
 * {@code ModerationStatus} tracks the workflow state in the moderator
 * pipeline.</p>
 *
 * <p>Mapping (Phase 2A §10):</p>
 * <pre>
 *   PENDING_AUTO    → product just submitted, waiting on Auto Moderation engine
 *   PENDING_MANUAL  → moderator queue
 *   AUTO_PASSED     → auto engine passed; moderator may still inspect
 *   AUTO_REJECTED   → auto engine flagged; moderator may still inspect
 *   APPROVED        → moderator approved → ProductStatus = ACTIVE   (visible)
 *   REJECTED        → moderator rejected → ProductStatus = HIDDEN   (terminal for moderator)
 *   ESCALATED       → moderator escalated → ProductStatus = HIDDEN  (admin reviews)
 * </pre>
 *
 * <p>This enum does NOT replace {@link ProductStatus}. Existing code that
 * depends on {@code ProductStatus} continues to work without changes.</p>
 */
public enum ModerationStatus {
    /** Phase 4 — product just submitted; Auto Moderation engine has not returned a result yet. */
    PENDING_AUTO,
    PENDING_MANUAL,
    AUTO_PASSED,
    AUTO_REJECTED,
    APPROVED,
    REJECTED,
    ESCALATED,
    // --- BACKWARD-COMPAT: documents created before Phase 2A refactor may hold this value ---
    @Deprecated
    PENDING_AUTO
}
