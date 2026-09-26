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

    /**
     * TASK #26 — Lấy review hiển thị trên product-detail page, có tính đến
     * trạng thái moderation + viewer identity.
     *
     * <p>Quy tắc:</p>
     * <ul>
     *   <li><b>VISIBLE / REPORTED</b>: hiển thị cho tất cả mọi người.</li>
     *   <li><b>HIDDEN</b>: chỉ hiển thị cho owner của review.</li>
     *   <li><b>DELETED</b>: không hiển thị cho bất kỳ ai (kể cả owner — owner xem qua /my-reviews).</li>
     * </ul>
     *
     * @param productId id sản phẩm
     * @param viewerId  user đang xem (null nếu khách vãng lai)
     */
    List<Review> getVisibleReviewsByProductId(String productId, String viewerId);

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

    // ===== TASK #26: Review Images =====

    /**
     * TASK #26 — Thêm ảnh vào Review. Chỉ owner mới được thực hiện.
     *
     * @param reviewId review cần thêm ảnh
     * @param userId    owner của review (phải khớp với review.userId)
     * @param imageUrl  URL đã upload (format: /uploads/{uuid}.{ext})
     * @return review sau khi cập nhật
     * @throws com.ecommerce.cnj70.exception.BadRequestException nếu không phải owner
     *         hoặc đã đạt giới hạn 5 ảnh
     */
    Review addReviewImage(String reviewId, String userId, String imageUrl);

    /**
     * TASK #26 — Xóa ảnh khỏi Review. Chỉ owner mới được thực hiện.
     *
     * @param reviewId  review cần xóa ảnh
     * @param userId    owner của review
     * @param imageUrl  URL ảnh cần xóa (phải tồn tại trong review.images)
     * @return review sau khi cập nhật
     * @throws com.ecommerce.cnj70.exception.BadRequestException nếu không phải owner
     *         hoặc ảnh không tồn tại trong review
     */
    Review removeReviewImage(String reviewId, String userId, String imageUrl);
}
