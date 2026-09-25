package com.ecommerce.cnj70.dto.review;

import com.ecommerce.cnj70.enums.ModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2B — Review Moderation Context.
 *
 * <p>Aggregated DTO passed to the Review Detail view. Composed of:
 * the Review entity fields plus Moderator-relevant context (Product /
 * Shop / Vendor / Customer / Verified Purchase / Report Summary /
 * Violation Context).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationContext {
    private String reviewId;
    private String productId;
    private String productName;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String shopId;
    private String shopName;
    private String vendorId;
    private String vendorName;
    private int rating;
    private String comment;
    private ModerationStatus pipelineModerationStatus;
    private boolean hidden;
    private String hiddenReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Integration seams
    private VerifiedPurchaseResult verifiedPurchase;
    private ReviewReportSummary reportSummary;
    private ReviewViolationContext violationContext;
}
