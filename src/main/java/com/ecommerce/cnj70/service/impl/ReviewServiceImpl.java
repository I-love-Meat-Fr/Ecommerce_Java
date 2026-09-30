package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.ReportCaseCreateReq;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ReportTargetType;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.VerifiedPurchaseException;
import com.ecommerce.cnj70.moderation.ModerationContext;
import com.ecommerce.cnj70.moderation.ModerationDecision;
import com.ecommerce.cnj70.moderation.ModerationPipelineResult;
import com.ecommerce.cnj70.moderation.ModerationPipelineService;
import com.ecommerce.cnj70.moderation.ReviewModerationContext;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    /** Yêu cầu #1 — Auto Moderation Pipeline cho Review. */
    private final ModerationPipelineService moderationPipelineService;

    @Override
    @Transactional
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

        // ===== Yêu cầu #2 — Verified Purchase validation =====
        // Chỉ Customer đã mua và ĐÃ NHẬN HÀNG thành công (Order DELIVERED hoặc COMPLETED)
        // mới được phép tạo Review. Đây là rào chặn trước khi Review lọt vào
        // Auto Moderation Pipeline (yêu cầu #1).
        if (!orderService.hasUserReceivedProduct(userId, productId)) {
            throw new VerifiedPurchaseException();
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
                .moderationStatus(ReviewModerationStatus.VISIBLE)
                .build();

        Review savedReview = reviewRepository.save(review);

        // ===== Yêu cầu #1 — Chạy Auto Moderation Pipeline =====
        // Pipeline chạy SAU khi save (đã có reviewId) để có thể tạo ReportCase
        // với targetId hợp lệ.
        ModerationPipelineResult pipelineResult =
                runReviewModerationPipeline(savedReview, product);

        // Lưu lại review sau khi pipeline apply status
        Review finalized = reviewRepository.save(savedReview);

        updateProductRating(product.getId());

        // ===== PHASE J — Audit log REVIEW_CREATED (user-facing) =====
        try {
            auditLogService.logInfo(
                    AuditAction.REVIEW_CREATED,
                    "REVIEW",
                    finalized.getId(),
                    userId,
                    user.getFullName(),
                    "CUSTOMER",
                    "Customer tạo review cho productId=" + finalized.getProductId()
                            + " rating=" + finalized.getRating()
                            + " | pipeline=" + pipelineResult.getSeverity()
                            + " flags=" + pipelineResult.getAutoFlags());
        } catch (Exception ex) {
            log.warn("Failed to write REVIEW_CREATED audit for review {}: {}",
                    finalized.getId(), ex.getMessage());
        }

        return finalized;
    }

    /**
     * Chạy {@link ModerationPipelineService} cho Review vừa tạo và apply kết quả:
     *
     * <ul>
     *   <li>{@code PASS} → giữ {@code moderationStatus = VISIBLE},
     *       {@code pipelineModerationStatus = AUTO_PASSED} (đồng bộ với Product).</li>
     *   <li>{@code SUSPICIOUS} → đặt {@code pipelineModerationStatus = PENDING_MANUAL}
     *       và tạo {@code ReportCase} cho Moderator duyệt tay.</li>
     *   <li>{@code FATAL} → đặt {@code moderationStatus = HIDDEN},
     *       {@code pipelineModerationStatus = AUTO_REJECTED}.</li>
     * </ul>
     */
    private ModerationPipelineResult runReviewModerationPipeline(Review review, Product product) {
        ModerationContext ctx = new ReviewModerationContext(review,
                ModerationContext.SharedState.builder().build(),
                LocalDateTime.now());

        ModerationPipelineResult result;
        try {
            result = moderationPipelineService.run(ctx);
        } catch (Exception ex) {
            // Fail-safe: pipeline lỗi → coi như PASS để không chặn user.
            log.error("[Review] Moderation pipeline threw for review id={}: {} — defaulting to PASS",
                    review.getId(), ex.getMessage(), ex);
            result = ModerationPipelineResult.pass();
        }

        log.info("[Review] Pipeline result for review id={} userId={}: severity={} flags={} reasons={}",
                review.getId(), review.getUserId(),
                result.getSeverity(),
                result.getAutoFlags(),
                result.getReasons());

        // Apply kết quả lên document
        switch (result.getSeverity()) {
            case PASS -> {
                review.setModerationStatus(ReviewModerationStatus.VISIBLE);
                review.setPipelineModerationStatus(ModerationStatus.AUTO_PASSED);
            }
            case SUSPICIOUS -> {
                review.setModerationStatus(ReviewModerationStatus.VISIBLE);
                review.setPipelineModerationStatus(ModerationStatus.PENDING_MANUAL);
                review.setModerationReason(formatReason(result));
                createAutoReportCaseForReview(review, product, result);
            }
            case FATAL -> {
                review.setModerationStatus(ReviewModerationStatus.HIDDEN);
                review.setPipelineModerationStatus(ModerationStatus.AUTO_REJECTED);
                review.setModerationReason(formatReason(result));
                review.setHidden(true);
                review.setHiddenReason(formatReason(result));
            }
        }

        return result;
    }

    private static String formatReason(ModerationPipelineResult result) {
        if (result.getReasons() == null || result.getReasons().isEmpty()) {
            return "Auto Moderation flagged";
        }
        return String.join(" | ", result.getReasons());
    }

    /**
     * Tạo ReportCase cho Moderator duyệt tay khi pipeline verdict = SUSPICIOUS.
     * Idempotent: nếu đã có case PENDING cho cùng target, ReportCaseService tự skip.
     */
    private void createAutoReportCaseForReview(Review review, Product product,
                                               ModerationPipelineResult result) {
        try {
            String description = "Auto Moderation phát hiện dấu hiệu đáng ngờ trong Review:\n"
                    + String.join("\n", result.getReasons());
            ReportCaseCreateReq req = ReportCaseCreateReq.builder()
                    .targetType(ReportTargetType.REVIEW)
                    .targetId(review.getId())
                    .reason("Auto Moderation flag")
                    .description(description)
                    .autoFlags(new ArrayList<>(result.getAutoFlags()))
                    .source("AUTO")
                    .priority(5)
                    .build();
            reportCaseService.createCase(req, null, "SYSTEM");
        } catch (Exception ex) {
            log.warn("[Review] createAutoReportCase failed for review id={}: {}",
                    review.getId(), ex.getMessage());
        }
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

        // ===== PHASE J — Audit log REVIEW_DELETED (user-facing) =====
        try {
            auditLogService.logWarning(
                    AuditAction.REVIEW_DELETED,
                    "REVIEW",
                    reviewId,
                    userId,
                    null,
                    "CUSTOMER",
                    "Customer tự xóa review của mình (productId=" + productId + ")");
        } catch (Exception ex) {
            log.warn("Failed to write REVIEW_DELETED audit for review {}: {}",
                    reviewId, ex.getMessage());
        }
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

        // Sync hidden flag for legacy template/admin UI compatibility.
        review.setHidden(true);
        review.setHiddenReason(reason);
        review.setModerationActorId(moderatorId);

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

        // Sync hidden flag for legacy template/admin UI compatibility.
        review.setHidden(false);
        review.setHiddenReason(null);

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
