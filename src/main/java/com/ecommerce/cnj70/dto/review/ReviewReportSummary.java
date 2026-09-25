package com.ecommerce.cnj70.dto.review;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 2B — Review Report Summary (integration seam).
 *
 * <p>Aggregated read-only DTO. Source: ReportCase backend (Phase 2C
 * scope — not implemented in Phase 2B).</p>
 *
 * <p>Phase 2B never creates fake reports. If ReportCase backend is not
 * available, the gateway returns an empty summary.</p>
 */
public record ReviewReportSummary(
        String reviewId,
        long reportCount,
        List<String> reportReasons,
        LocalDateTime firstReportedAt,
        LocalDateTime lastReportedAt
) {
    public static ReviewReportSummary empty(String reviewId) {
        return new ReviewReportSummary(reviewId, 0L, List.of(), null, null);
    }
}
