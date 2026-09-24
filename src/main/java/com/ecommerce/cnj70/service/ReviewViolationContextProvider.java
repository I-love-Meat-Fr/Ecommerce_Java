package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.review.ReviewViolationContext;

/**
 * Phase 2B — Review Violation Context Provider (integration seam).
 *
 * <p>Reads Auto Moderation / Violation Engine results for a Review.
 * Phase 2B displays these — never recomputes them (Phase 2B §22).</p>
 */
public interface ReviewViolationContextProvider {

    ReviewViolationContext getContext(String reviewId);
}
