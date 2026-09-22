package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.ReportCaseCreateReq;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.ReportTargetType;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.OrderService;
import com.ecommerce.cnj70.service.ReportCaseService;
import com.ecommerce.cnj70.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private static final int AUTO_REPORT_THRESHOLD = 3;

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final AuditLogService auditLogService;
    private final ReportCaseService reportCaseService;

    @Override
    public Review createReview(String userId, String productId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating phải từ 1 đến 5 sao");
        }

        if (comment == null || comment.trim().isEmpty()) {
            throw new BadRequestException("Nội dung đánh giá không được để trống");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // ===== TASK #14/#20: bắt buộc đã nhận Product (DELIVERED) mới được review =====
        // Trước đây: check hasUserPurchasedProduct → Order không CANCELLED → vẫn review khi chưa nhận hàng
        // Bây giờ: phải có Order DELIVERED chứa productId của user mới được review
        if (!orderService.hasUserReceivedProduct(userId, productId)) {
            throw new BadRequestException(
                    "Bạn chỉ có thể đánh giá sản phẩm sau khi đã nhận được hàng");
        }

        if (reviewRepository.findByProductIdAndUserId(productId, userId).isPresent()) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này rồi");
        }

        Review review = Review.builder()
                .productId(productId)
                .userId(userId)
                .userName(user.getFullName())
                .userAvatar(user.getAvatarUrl())
                .rating(rating)
                .comment(comment.trim())
                .build();

        Review savedReview = reviewRepository.save(review);

        updateProductRating(product.getId());

        return savedReview;
    }

    @Override
    public Review updateReview(String reviewId, String userId, int rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền sửa đánh giá này");
        }

        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating phải từ 1 đến 5 sao");
        }

        if (comment == null || comment.trim().isEmpty()) {
            throw new BadRequestException("Nội dung đánh giá không được để trống");
        }

        review.setRating(rating);
        review.setComment(comment.trim());

        Review updatedReview = reviewRepository.save(review);

        updateProductRating(review.getProductId());

        return updatedReview;
    }

    @Override
    public void deleteReview(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa đánh giá này");
        }

        String productId = review.getProductId();
        reviewRepository.delete(review);

        updateProductRating(productId);
    }

    @Override
    public Review getReviewById(String reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
    }

    @Override
    public List<Review> getReviewsByProductId(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    public List<Review> getReviewsByUserId(String userId) {
        return reviewRepository.findByUserId(userId);
    }

    @Override
    public boolean hasUserReviewedProduct(String userId, String productId) {
        return reviewRepository.findByProductIdAndUserId(productId, userId).isPresent();
    }

    /**
     * TASK #14/#20/#21 — Kiểm tra user có quyền review hay không.
     * Điều kiện: đã nhận Product (Order DELIVERED) + chưa review.
     */
    @Override
    public boolean canUserReviewProduct(String userId, String productId) {
        if (!orderService.hasUserReceivedProduct(userId, productId)) {
            return false;
        }
        return !hasUserReviewedProduct(userId, productId);
    }

    @Override
    public double getAverageRatingByProductId(String productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }

    @Override
    public int getReviewCountByProductId(String productId) {
        return reviewRepository.countByProductId(productId);
    }

    private void updateProductRating(String productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            double avgRating = getAverageRatingByProductId(productId);
            int reviewCount = getReviewCountByProductId(productId);
            product.setRating(avgRating);
            product.setReviewCount(reviewCount);
            productRepository.save(product);
        }
    }

    // ===== TASK #15: Review Moderation =====

    @Override
    public void reportReview(String reviewId, String reporterId, String reason) {
        if (reporterId == null || reporterId.isBlank()) {
            throw new BadRequestException("Không xác định được người report");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Lý do report không được để trống");
        }

        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        // Không cho tự report chính mình
        if (review.getUserId().equals(reporterId)) {
            throw new BadRequestException("Bạn không thể tự report đánh giá của chính mình");
        }

        review.setReportCount(review.getReportCount() + 1);

        // Tự động chuyển sang REPORTED nếu đạt threshold
        if (review.getReportCount() >= AUTO_REPORT_THRESHOLD) {
            review.setModerationStatus(ReviewModerationStatus.REPORTED);
            review.setModerationReason("Tự động chuyển sang REPORTED: " + review.getReportCount() + " reports");
            review.setModeratedAt(LocalDateTime.now());
            review.setModeratedBy("SYSTEM");

            auditLogService.logWarning(
                    AuditAction.REVIEW_REPORTED,
                    "REVIEW",
                    reviewId,
                    "SYSTEM",
                    "SYSTEM",
                    "SYSTEM",
                    "Review tự động bị đánh dấu REPORTED: " + review.getReportCount() +
                            " reports cho review của userId=" + review.getUserId() +
                            ", productId=" + review.getProductId()
            );
        }

        reviewRepository.save(review);

        // Audit log cho report
        auditLogService.logInfo(
                AuditAction.REVIEW_REPORTED,
                "REVIEW",
                reviewId,
                reporterId,
                null,
                "CUSTOMER",
                "Customer report review. Lý do: " + reason +
                        ". Tổng report: " + review.getReportCount()
        );

        // ===== TASK #26: Tạo ReportCase cho Moderator Queue =====
        try {
            String reporterUsername = userRepository.findById(reporterId)
                    .map(User::getFullName)
                    .orElse(reporterId);

            ReportCaseCreateReq createReq = ReportCaseCreateReq.builder()
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(reviewId)
                    .reason(reason)
                    .description("Customer report review. Tổng report: " + review.getReportCount())
                    .source("USER")
                    .priority(review.getReportCount() >= AUTO_REPORT_THRESHOLD ? 10 : 0)
                    .build();

            reportCaseService.createCase(createReq, reporterId, reporterUsername);
            log.info("[Review] Created ReportCase for review {}", reviewId);
        } catch (Exception ex) {
            // Không fail cả flow nếu tạo ReportCase lỗi (idempotent: đã có case PENDING thì ignore)
            log.warn("[Review] Failed to create ReportCase for review {}: {}", reviewId, ex.getMessage());
        }
    }

    @Override
    public Review hideReview(String reviewId, String moderatorId, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        ReviewModerationStatus before = review.getModerationStatus();

        review.setModerationStatus(ReviewModerationStatus.HIDDEN);
        review.setModerationReason(reason);
        review.setModeratedBy(moderatorId);
        review.setModeratedAt(LocalDateTime.now());

        Review saved = reviewRepository.save(review);

        auditLogService.logWarning(
                AuditAction.REVIEW_HIDDEN,
                "REVIEW",
                reviewId,
                moderatorId,
                null,
                "MODERATOR",
                "Moderator ẩn review: productId=" + review.getProductId() +
                        ", userId=" + review.getUserId() + ". Lý do: " + reason +
                        " (trước: " + before + " → HIDDEN)"
        );

        return saved;
    }

    @Override
    public void deleteReviewByModerator(String reviewId, String adminId, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        review.setModerationStatus(ReviewModerationStatus.DELETED);
        review.setModerationReason(reason);
        review.setModeratedBy(adminId);
        review.setModeratedAt(LocalDateTime.now());

        reviewRepository.save(review);

        // Audit log
        auditLogService.logCritical(
                AuditAction.REVIEW_DELETED,
                "REVIEW",
                reviewId,
                adminId,
                null,
                "ADMIN",
                "Admin xóa review: productId=" + review.getProductId() +
                        ", userId=" + review.getUserId() + ". Lý do: " + reason
        );
    }

    @Override
    public Review restoreReview(String reviewId, String moderatorId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        review.setModerationStatus(ReviewModerationStatus.VISIBLE);
        review.setModerationReason(null);
        review.setModeratedBy(moderatorId);
        review.setModeratedAt(LocalDateTime.now());
        review.setReportCount(0);

        Review saved = reviewRepository.save(review);

        auditLogService.logInfo(
                AuditAction.REVIEW_CREATED,
                "REVIEW",
                reviewId,
                moderatorId,
                null,
                "MODERATOR",
                "Moderator khôi phục review: productId=" + review.getProductId() +
                        ", userId=" + review.getUserId()
        );

        return saved;
    }

    @Override
    public List<Review> getReviewsByModerationStatus(ReviewModerationStatus status) {
        return reviewRepository.findByModerationStatus(status);
    }
}
