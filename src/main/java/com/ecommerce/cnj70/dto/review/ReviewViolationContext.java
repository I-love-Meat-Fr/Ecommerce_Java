package com.ecommerce.cnj70.dto.review;

import java.util.List;

/**
 * Phase 2B — Review Violation Context (integration seam).
 *
 * <p>Auto Moderation / Violation Engine result for a Review. Phase 2B
 * only displays what the integration provides. Auto Moderation
 * detection is owned by another team — Phase 2B never recomputes
 * severity / flags / reasons.</p>
 */
public record ReviewViolationContext(
        String reviewId,
        String status,
        List<String> flags,
        String severity,
        String reason,
        List<EvidenceItem> evidence
) {
    public record EvidenceItem(String type, String value, String source) {}

    public static ReviewViolationContext empty(String reviewId) {
        return new ReviewViolationContext(reviewId, "NOT_AVAILABLE", List.of(), null, null, List.of());
    }
}
