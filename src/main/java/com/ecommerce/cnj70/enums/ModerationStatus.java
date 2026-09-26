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
 *   PENDING_MANUAL → moderator queue
 *   APPROVED       → ProductStatus = ACTIVE   (visible to customer)
 *   REJECTED       → ProductStatus = HIDDEN   (terminal for moderator)
 *   ESCALATED      → ProductStatus = HIDDEN   (admin will review)
 *
 *   AUTO_PASSED    → moderator queue (informational — backend of Tuan)
 *   AUTO_REJECTED  → moderator queue (informational — backend of Tuan)
 * </pre>
 *
 * <p>This enum does NOT replace {@link ProductStatus}. Existing code that
 * depends on {@code ProductStatus} continues to work without changes.</p>
 */
public enum ModerationStatus {
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
