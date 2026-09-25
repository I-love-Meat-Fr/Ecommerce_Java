package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.review.VerifiedPurchaseResult;

/**
 * Phase 2B — Verified Purchase Gateway (integration seam).
 *
 * <p>Indicates whether a Review was posted by a customer who purchased
 * and received the product. Per Phase 2B §19, this is an integration
 * point — the contract is defined, the production source is the Order
 * backend (via {@code OrderBasedVerifiedPurchaseGateway}).</p>
 *
 * <p>Phase 2B does NOT fake this — if backend integration is missing,
 * the no-op provider returns {@code verified=false}.</p>
 */
public interface VerifiedPurchaseGateway {

    /**
     * @param userId    the customer who posted the review
     * @param productId the product being reviewed
     * @return VerifiedPurchaseResult — never null
     */
    VerifiedPurchaseResult check(String userId, String productId);
}
