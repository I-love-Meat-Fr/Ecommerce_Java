package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByProductId(String productId);

    List<Review> findByUserId(String userId);

    Optional<Review> findByProductIdAndUserId(String productId, String userId);

    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    int countByProductId(String productId);

    List<Review> findByProductIdInOrderByCreatedAtDesc(List<String> productIds);

    List<Review> findByProductIdIn(List<String> productIds, Pageable pageable);

    // ===== TASK #15: Review Moderation (legacy - ReviewModerationStatus) =====
    List<Review> findByModerationStatus(ReviewModerationStatus status);

    List<Review> findByModerationStatusOrderByReportCountDesc(ReviewModerationStatus status);

    Page<Review> findByModerationStatus(ReviewModerationStatus status, Pageable pageable);

    Page<Review> findByModerationStatusIn(List<ReviewModerationStatus> statuses, Pageable pageable);

    // ===== Phase 2B: Review Moderation Pipeline (ModerationStatus) =====
    // Sử dụng field pipelineModerationStatus trên Review
    Page<Review> findByPipelineModerationStatusIn(Set<ModerationStatus> statuses, Pageable pageable);

    Page<Review> findByCommentContainingIgnoreCaseAndPipelineModerationStatusIn(
            String comment, Set<ModerationStatus> statuses, Pageable pageable);

    Page<Review> findByRatingAndPipelineModerationStatusIn(
            int rating, Set<ModerationStatus> statuses, Pageable pageable);

    Page<Review> findByCommentContainingIgnoreCaseAndRatingAndPipelineModerationStatusIn(
            String comment, int rating, Set<ModerationStatus> statuses, Pageable pageable);

    long countByPipelineModerationStatusIn(java.util.Collection<ModerationStatus> statuses);
}
