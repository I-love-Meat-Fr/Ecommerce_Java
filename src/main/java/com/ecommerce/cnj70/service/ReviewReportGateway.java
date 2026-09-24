package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.review.ReviewReportSummary;

/**
 * Phase 2B — Review Report Gateway (integration seam).
 *
 * <p>Reads aggregated report data for a given Review. Backed by the
 * ReportCase backend (Phase 2C scope). Phase 2B defines this contract
 * so Moderator UI can display Report Count / Report Reasons / Evidence
 * without depending on the real ReportCase implementation.</p>
 *
 * <p>Production fake FORBIDDEN per Phase 2B §43.</p>
 */
public interface ReviewReportGateway {

    /**
     * @param reviewId the review to look up
     * @return aggregated summary. NEVER null. If backend unavailable,
     *         returns {@link ReviewReportSummary#empty(String)}.
     */
    ReviewReportSummary getReportSummary(String reviewId);
}
