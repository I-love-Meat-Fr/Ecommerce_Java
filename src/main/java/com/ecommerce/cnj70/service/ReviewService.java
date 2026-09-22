package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;

import java.util.List;

public interface ReviewService {

    Review createReview(String userId, String productId, int rating, String comment);

    Review updateReview(String reviewId, String userId, int rating, String comment);

    void deleteReview(String reviewId, String userId);

    Review getReviewById(String reviewId);

    List<Review> getReviewsByProductId(String productId);

    List<Review> getReviewsByUserId(String userId);

    boolean hasUserReviewedProduct(String userId, String productId);

    /**
     * TASK #14/#20/#21 — Kiểm tra user có quyền review Product không (đã nhận hàng).
     */
    boolean canUserReviewProduct(String userId, String productId);

    double getAverageRatingByProductId(String productId);

    int getReviewCountByProductId(String productId);

    // ===== TASK #15: Review Moderation =====

    /**
     * TASK #15 — Customer báo cáo Review.
     */
    void reportReview(String reviewId, String reporterId, String reason);

    /**
     * TASK #15 — Moderator ẩn Review.
     */
    Review hideReview(String reviewId, String moderatorId, String reason);

    /**
     * TASK #15 — Admin xóa Review (vĩnh viễn).
     */
    void deleteReviewByModerator(String reviewId, String adminId, String reason);

    /**
     * TASK #15 — Moderator khôi phục Review (HIDDEN/REPORTED → VISIBLE).
     */
    Review restoreReview(String reviewId, String moderatorId);

    /**
     * TASK #15 — Lấy reviews theo moderation status (cho Moderator queue).
     */
    List<Review> getReviewsByModerationStatus(ReviewModerationStatus status);
}
