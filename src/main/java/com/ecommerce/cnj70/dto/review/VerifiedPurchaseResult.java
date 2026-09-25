package com.ecommerce.cnj70.dto.review;

/**
 * Phase 2B — Verified Purchase Result DTO.
 *
 * <p>Returned by {@code VerifiedPurchaseGateway} to indicate whether a
 * Review was posted by a customer who actually purchased and received
 * the product.</p>
 */
public record VerifiedPurchaseResult(
        boolean verified,
        String orderId,
        String orderItemId,
        String deliveredAt
) {
    public static VerifiedPurchaseResult notVerified() {
        return new VerifiedPurchaseResult(false, null, null, null);
    }
}
