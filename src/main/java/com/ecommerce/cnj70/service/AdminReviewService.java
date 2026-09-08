package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 15 — Admin Review Moderation Service.
 *
 * Phạm vi đã chốt theo Phase 15 document:
 *   - Admin Review List (search + filter rating + pagination)
 *   - Admin Review Detail
 *   - Admin Delete Violation (hard delete qua MongoRepository, KHÔNG cascade)
 *
 * BLOCKED / DEPENDENCY theo Phase 15 (chờ Contract):
 *   - Hide / Unhide: cần schema field `isHidden` (LOCK 3 / 6)
 *   - Product.rating / reviewCount impact sau Hide/Unhide/Delete (LOCK 4, [DEPENDENCY])
 *   - Customer Visibility cho Hidden Review (LOCK 5, [DEPENDENCY — CUSTOMER OWNER])
 *   - Review Status / Moderation State (LOCK 3)
 */
public interface AdminReviewService {

    /**
     * TASK 15.9 — Admin Review List.
     * Search theo comment (case-insensitive) + filter rating (1-5) + pagination.
     */
    Page<Review> listReviews(Pageable pageable, String q, Integer ratingFilter);

    /**
     * TASK 15.10 — Admin Review Detail.
     */
    Review getReviewById(String id);

    /**
     * TASK 15.13 — Delete Review Violation (hard delete).
     * KHÔNG cascade Product/Order/Customer theo LOCK 2.
     * KHÔNG đụng vào Product.rating / reviewCount (LOCK 4 — chờ Contract).
     */
    void deleteReview(String id);
}
