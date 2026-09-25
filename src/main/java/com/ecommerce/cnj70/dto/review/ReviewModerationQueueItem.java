package com.ecommerce.cnj70.dto.review;

import com.ecommerce.cnj70.enums.ModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2B — Review Moderation Queue Item.
 *
 * <p>One row in the Moderator Review queue. Aggregates minimal data so
 * the queue page does not need N+1 lookups for vendor/customer names.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationQueueItem {
    private String reviewId;
    private String productId;
    private String productName;
    private String customerId;
    private String customerName;
    private String customerEmail;
    private String shopId;
    private String shopName;
    private String vendorName;
    private int rating;
    private String commentPreview;
    private ModerationStatus pipelineModerationStatus;
    private boolean hidden;
    private long reportCount;
    private ReviewViolationContext violationContext;
    private LocalDateTime createdAt;
}
