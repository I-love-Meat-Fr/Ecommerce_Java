package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.dto.review.ReviewReportSummary;
import com.ecommerce.cnj70.service.ReviewReportGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Phase 2B — No-op Review Report Provider.
 *
 * <p>ReportCase backend is owned by another team (Phase 2C scope).
 * Active when no real ReportCase is wired in. Returns empty summary —
 * UI degrades gracefully without fake data.</p>
 */
@Slf4j
@Component
@Profile("!report-case")
public class NoopReviewReportGateway implements ReviewReportGateway {

    public NoopReviewReportGateway() {
        log.info("NoopReviewReportGateway active — ReportCase backend not wired in. " +
                "Phase 2B UI will display 'Report Count: not available'.");
    }

    @Override
    public ReviewReportSummary getReportSummary(String reviewId) {
        return ReviewReportSummary.empty(reviewId);
    }
}
