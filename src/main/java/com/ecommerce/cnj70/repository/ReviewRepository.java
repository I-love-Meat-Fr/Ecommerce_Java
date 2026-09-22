package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByProductId(String productId);

    List<Review> findByUserId(String userId);

    Optional<Review> findByProductIdAndUserId(String productId, String userId);

    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    int countByProductId(String productId);

    List<Review> findByProductIdInOrderByCreatedAtDesc(List<String> productIds);

    List<Review> findByProductIdIn(List<String> productIds, Pageable pageable);

    // ===== TASK #15: Review Moderation =====
    List<Review> findByModerationStatus(ReviewModerationStatus status);

    List<Review> findByModerationStatusOrderByReportCountDesc(ReviewModerationStatus status);

    // Paginated queries for Moderator
    Page<Review> findByModerationStatus(ReviewModerationStatus status, Pageable pageable);

    Page<Review> findByModerationStatusIn(List<ReviewModerationStatus> statuses, Pageable pageable);
}
