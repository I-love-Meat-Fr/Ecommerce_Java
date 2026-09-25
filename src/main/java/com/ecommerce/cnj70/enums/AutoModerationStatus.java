package com.ecommerce.cnj70.enums;

/**
 * Phase 2A — Auto Moderation Engine status (integration seam).
 *
 * <p>Auto Moderation Engine is owned by the Auto Moderation team (external
 * integration point). This enum is consumed via
 * {@code AutoModerationResultProvider} interface — Phase 2A never
 * instantiates values; it only displays what the integration returns.</p>
 *
 * <p>Phase 2A does NOT compute these values; doing so would fake
 * production behavior which is forbidden per Phase 2A §9 + §47.</p>
 */
public enum AutoModerationStatus {
    AUTO_PASSED,
    AUTO_REJECTED,
    PENDING_MANUAL
}
