package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.dto.review.ReviewViolationContext;
import com.ecommerce.cnj70.service.ReviewViolationContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Phase 2B — No-op Violation Context Provider.
 *
 * <p>Active when no real Violation / Auto Moderation Engine for Review
 * is wired in. Returns empty context. Phase 2B never fakes
 * severity/flag/reason (Phase 2B §22).</p>
 */
@Slf4j
@Component
@Profile("!review-violation")
public class NoopReviewViolationContextProvider implements ReviewViolationContextProvider {

    public NoopReviewViolationContextProvider() {
        log.info("NoopReviewViolationContextProvider active — Review Violation backend not wired in. " +
                "Phase 2B UI will display 'Auto Moderation Result: not available'.");
    }

    @Override
    public ReviewViolationContext getContext(String reviewId) {
        return ReviewViolationContext.empty(reviewId);
    }
}
