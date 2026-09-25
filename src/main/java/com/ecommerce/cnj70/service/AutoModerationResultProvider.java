package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.moderation.AutoModerationResult;

/**
 * Phase 2A — Auto Moderation Result Provider (integration seam).
 *
 * <p>Defined as an interface to allow the Auto Moderation backend (owned
 * by the Auto Moderation team) to be plugged in later without modifying
 * Phase 2A Moderator logic.</p>
 *
 * <p>Phase 2A §9 / §11 contract:</p>
 * <ul>
 *     <li>Backend available  → real implementation returns {@code AutoModerationResult}</li>
 *     <li>Backend unavailable → return {@code Optional.empty()}, do NOT fake values</li>
 * </ul>
 *
 * <p>Production fake is FORBIDDEN (Phase 2A §46). Tests may provide a
 * Test Double but only inside the test classpath.</p>
 */
public interface AutoModerationResultProvider {

    /**
     * @param productId the product to look up. Never {@code null}.
     * @return {@link java.util.Optional#of(Object)} when the backend has a result,
     *         {@link java.util.Optional#empty()} when the backend is unavailable
     *         or has no result for the given product. NEVER returns null.
     */
    java.util.Optional<AutoModerationResult> getResult(String productId);
}
