package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Review;

import java.util.List;

public interface ReviewService {

    /**
     * Tạo review mới.
     *
     * TASK #21 — Trước khi tạo, kiểm tra user đã thực sự mua Product hay chưa
     * (qua OrderService.hasUserPurchasedProduct).
     * Throw BadRequestException nếu user chưa mua Product đó.
     */
    Review createReview(String userId, String productId, int rating, String comment);

    Review updateReview(String reviewId, String userId, int rating, String comment);

    void deleteReview(String reviewId, String userId);

    Review getReviewById(String reviewId);

    List<Review> getReviewsByProductId(String productId);

    List<Review> getReviewsByUserId(String userId);

    /**
     * Check đã review hay chưa (UI dùng để ẩn/hiện form review).
     */
    boolean hasUserReviewedProduct(String userId, String productId);

    /**
     * TASK #21 — Check user có quyền review Product không (đã mua thành công).
     */
    boolean canUserReviewProduct(String userId, String productId);

    double getAverageRatingByProductId(String productId);

    int getReviewCountByProductId(String productId);
}
