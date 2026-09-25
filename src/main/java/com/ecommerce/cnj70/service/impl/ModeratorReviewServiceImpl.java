package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.dto.review.ReviewModerationContext;
import com.ecommerce.cnj70.dto.review.ReviewModerationDecision;
import com.ecommerce.cnj70.dto.review.ReviewModerationQueueItem;
import com.ecommerce.cnj70.dto.review.ReviewReportSummary;
import com.ecommerce.cnj70.dto.review.ReviewViolationContext;
import com.ecommerce.cnj70.dto.review.VerifiedPurchaseResult;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.ModeratorReviewService;
import com.ecommerce.cnj70.service.ReviewReportGateway;
import com.ecommerce.cnj70.service.ReviewViolationContextProvider;
import com.ecommerce.cnj70.service.VerifiedPurchaseGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 2B — Moderator Review Service Implementation.
 *
 * <p>See {@link ModeratorReviewService} for the contract and rules.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeratorReviewServiceImpl implements ModeratorReviewService {

    /** States that still need moderator attention (queue candidates). */
    private static final Set<ModerationStatus> QUEUE_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(ModerationStatus.PENDING_MANUAL,
                       ModerationStatus.AUTO_PASSED,
                       ModerationStatus.AUTO_REJECTED));

    /** Review.content is never modified — moderation metadata only. */

    private final ReviewRepository reviewRepository;
    private final ModerationHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;
    private final VerifiedPurchaseGateway verifiedPurchaseGateway;
    private final ReviewReportGateway reviewReportGateway;
    private final ReviewViolationContextProvider reviewViolationContextProvider;
    private final AuditEventWriter auditEventWriter;

    // ======================== QUEUE ========================

    @Override
    public Page<ReviewModerationQueueItem> listQueue(String q, ModerationStatus moderationStatus,
                                                     Integer rating, Pageable pageable) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasStatus = moderationStatus != null;
        boolean hasRating = rating != null;

        Page<Review> reviews;

        if (!hasQ && !hasStatus && !hasRating) {
            reviews = reviewRepository.findByPipelineModerationStatusIn(QUEUE_STATUSES, pageable);
        } else if (hasQ && !hasStatus && !hasRating) {
            reviews = reviewRepository.findByCommentContainingIgnoreCaseAndPipelineModerationStatusIn(
                    q.trim(), QUEUE_STATUSES, pageable);
        } else if (!hasQ && hasStatus && !hasRating) {
            if (!QUEUE_STATUSES.contains(moderationStatus)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            reviews = reviewRepository.findByPipelineModerationStatusIn(
                    Collections.singleton(moderationStatus), pageable);
        } else if (!hasQ && !hasStatus && hasRating) {
            reviews = reviewRepository.findByRatingAndPipelineModerationStatusIn(rating, QUEUE_STATUSES, pageable);
        } else {
            // Combined filters — narrow to the safe sets
            Set<ModerationStatus> effectiveStatuses = hasStatus
                    ? Collections.singleton(moderationStatus)
                    : QUEUE_STATUSES;
            if (hasStatus && !QUEUE_STATUSES.contains(moderationStatus)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            int effectiveRating = rating;
            reviews = reviewRepository.findByCommentContainingIgnoreCaseAndRatingAndPipelineModerationStatusIn(
                    hasQ ? q.trim() : "", effectiveRating, effectiveStatuses, pageable);
        }

        List<ReviewModerationQueueItem> items = reviews.getContent().stream()
                .map(this::toQueueItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, reviews.getTotalElements());
    }

    private ReviewModerationQueueItem toQueueItem(Review review) {
        String productName = null;
        String shopId = null;
        String shopName = null;
        String vendorName = null;

        if (review.getProductId() != null) {
            Product product = productRepository.findById(review.getProductId()).orElse(null);
            if (product != null) {
                productName = product.getName();
                shopId = product.getShopId();
                shopName = product.getShopName();
            }
        }
        if (shopId != null) {
            Shop shop = shopRepository.findById(shopId).orElse(null);
            if (shop != null && shop.getOwnerId() != null) {
                User vendor = userRepository.findById(shop.getOwnerId()).orElse(null);
                if (vendor != null) {
                    vendorName = vendor.getFullName();
                }
            }
        }

        ReviewReportSummary reportSummary = reviewReportGateway.getReportSummary(review.getId());
        ReviewViolationContext violationContext = reviewViolationContextProvider.getContext(review.getId());

        return ReviewModerationQueueItem.builder()
                .reviewId(review.getId())
                .productId(review.getProductId())
                .productName(productName)
                .customerId(review.getUserId())
                .customerName(review.getUserName())
                .shopId(shopId)
                .shopName(shopName)
                .vendorName(vendorName)
                .rating(review.getRating())
                .commentPreview(review.getComment() != null && review.getComment().length() > 80
                        ? review.getComment().substring(0, 80) + "…"
                        : review.getComment())
                .pipelineModerationStatus(review.getPipelineModerationStatus())
                .hidden(review.isHidden())
                .reportCount(reportSummary.reportCount())
                .violationContext(violationContext)
                .createdAt(review.getCreatedAt())
                .build();
    }

    // ======================== DETAIL ========================

    @Override
    public ReviewModerationContext getContext(String reviewId) {
        Review review = loadAndGuard(reviewId);

        String productName = null;
        String shopId = null;
        String shopName = null;
        String vendorId = null;
        String vendorName = null;

        if (review.getProductId() != null) {
            Product product = productRepository.findById(review.getProductId()).orElse(null);
            if (product != null) {
                productName = product.getName();
                shopId = product.getShopId();
                shopName = product.getShopName();
            }
        }
        if (shopId != null) {
            Shop shop = shopRepository.findById(shopId).orElse(null);
            if (shop != null) {
                vendorId = shop.getOwnerId();
                if (vendorId != null) {
                    User vendor = userRepository.findById(vendorId).orElse(null);
                    if (vendor != null) {
                        vendorName = vendor.getFullName();
                    }
                }
            }
        }

        User customer = review.getUserId() != null
                ? userRepository.findById(review.getUserId()).orElse(null)
                : null;

        VerifiedPurchaseResult verifiedPurchase = verifiedPurchaseGateway.check(
                review.getUserId(), review.getProductId());
        ReviewReportSummary reportSummary = reviewReportGateway.getReportSummary(review.getId());
        ReviewViolationContext violationContext = reviewViolationContextProvider.getContext(review.getId());

        return ReviewModerationContext.builder()
                .reviewId(review.getId())
                .productId(review.getProductId())
                .productName(productName)
                .customerId(review.getUserId())
                .customerName(customer != null ? customer.getFullName() : review.getUserName())
                .customerEmail(customer != null ? customer.getEmail() : null)
                .shopId(shopId)
                .shopName(shopName)
                .vendorId(vendorId)
                .vendorName(vendorName)
                .rating(review.getRating())
                .comment(review.getComment())
                .pipelineModerationStatus(review.getPipelineModerationStatus())
                .hidden(review.isHidden())
                .hiddenReason(review.getHiddenReason())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .verifiedPurchase(verifiedPurchase)
                .reportSummary(reportSummary)
                .violationContext(violationContext)
                .build();
    }

    // ======================== APPROVE ========================

    @Override
    @Transactional
    public ReviewModerationDecision approve(String reviewId, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Review review = loadAndGuard(reviewId);
        ModerationStatus before = review.getPipelineModerationStatus();

        if (isFinalState(before)) {
            throw new ConflictException(
                    "Review đã ở trạng thái cuối, không thể duyệt (status=" + before + ")");
        }

        review.setPipelineModerationStatus(ModerationStatus.APPROVED);
        syncLegacyModerationStatus(review, ModerationStatus.APPROVED);
        review.setModerationActorId(moderator.getId());
        review.setModerationAt(LocalDateTime.now());
        review.setModerationReason(null);
        reviewRepository.save(review);

        ReviewModerationDecision decision = ReviewModerationDecision.builder()
                .reviewId(reviewId)
                .action("APPROVE")
                .newStatus(ModerationStatus.APPROVED)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã duyệt đánh giá (ID: " + reviewId + ")")
                .build();

        recordHistory(review, decision, before, null);
        emitAudit(review, decision, before, clientIp);

        log.info("Moderator approve review: reviewId={} moderatorId={} before={} after={}",
                reviewId, moderator.getId(), before, ModerationStatus.APPROVED);
        return decision;
    }

    // ======================== REJECT ========================

    @Override
    @Transactional
    public ReviewModerationDecision reject(String reviewId, String reason, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Review review = loadAndGuard(reviewId);
        ModerationStatus before = review.getPipelineModerationStatus();

        validateReason(reason);

        if (isFinalState(before)) {
            throw new ConflictException(
                    "Review đã ở trạng thái cuối, không thể từ chối (status=" + before + ")");
        }

        review.setPipelineModerationStatus(ModerationStatus.REJECTED);
        syncLegacyModerationStatus(review, ModerationStatus.REJECTED);
        review.setModerationActorId(moderator.getId());
        review.setModerationAt(LocalDateTime.now());
        review.setModerationReason(reason);
        reviewRepository.save(review);

        ReviewModerationDecision decision = ReviewModerationDecision.builder()
                .reviewId(reviewId)
                .action("REJECT")
                .newStatus(ModerationStatus.REJECTED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã từ chối đánh giá (ID: " + reviewId + ")")
                .build();

        recordHistory(review, decision, before, reason);
        emitAudit(review, decision, before, clientIp);

        log.info("Moderator reject review: reviewId={} moderatorId={} reason.length={}",
                reviewId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== HIDE ========================

    @Override
    @Transactional
    public ReviewModerationDecision hide(String reviewId, String reason, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Review review = loadAndGuard(reviewId);
        ModerationStatus before = review.getPipelineModerationStatus();

        validateReason(reason);

        if (review.isHidden()) {
            throw new ConflictException("Review đã ở trạng thái HIDDEN, không thể hide lại");
        }

        review.setHidden(true);
        review.setHiddenReason(reason);
        review.setModerationActorId(moderator.getId());
        review.setModerationAt(LocalDateTime.now());
        review.setModerationReason(reason);
        reviewRepository.save(review);

        ReviewModerationDecision decision = ReviewModerationDecision.builder()
                .reviewId(reviewId)
                .action("HIDE")
                .newStatus(before) // moderationStatus not changed on Hide
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã ẩn đánh giá (ID: " + reviewId + ")")
                .build();

        recordHistory(review, decision, before, reason);
        emitAudit(review, decision, before, clientIp);

        log.info("Moderator hide review: reviewId={} moderatorId={} reason.length={}",
                reviewId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== UNHIDE ========================

    @Override
    @Transactional
    public ReviewModerationDecision unhide(String reviewId, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Review review = loadAndGuard(reviewId);
        ModerationStatus before = review.getPipelineModerationStatus();

        if (!review.isHidden()) {
            throw new ConflictException("Review không ở trạng thái HIDDEN, không thể unhide");
        }

        review.setHidden(false);
        review.setHiddenReason(null);
        review.setModerationActorId(moderator.getId());
        review.setModerationAt(LocalDateTime.now());
        review.setModerationReason(null);
        reviewRepository.save(review);

        ReviewModerationDecision decision = ReviewModerationDecision.builder()
                .reviewId(reviewId)
                .action("UNHIDE")
                .newStatus(before)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã hiện đánh giá (ID: " + reviewId + ")")
                .build();

        recordHistory(review, decision, before, null);
        emitAudit(review, decision, before, clientIp);

        log.info("Moderator unhide review: reviewId={} moderatorId={}", reviewId, moderator.getId());
        return decision;
    }

    // ======================== ESCALATE ========================

    @Override
    @Transactional
    public ReviewModerationDecision escalate(String reviewId, String reason, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Review review = loadAndGuard(reviewId);
        ModerationStatus before = review.getPipelineModerationStatus();

        validateReason(reason);

        if (isFinalState(before)) {
            throw new ConflictException(
                    "Review đã ở trạng thái cuối, không thể leo thang (status=" + before + ")");
        }

        review.setPipelineModerationStatus(ModerationStatus.ESCALATED);
        syncLegacyModerationStatus(review, ModerationStatus.ESCALATED);
        review.setModerationActorId(moderator.getId());
        review.setModerationAt(LocalDateTime.now());
        review.setModerationReason(reason);
        reviewRepository.save(review);

        ReviewModerationDecision decision = ReviewModerationDecision.builder()
                .reviewId(reviewId)
                .action("ESCALATE")
                .newStatus(ModerationStatus.ESCALATED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã leo thang đánh giá (ID: " + reviewId + ") lên Admin")
                .build();

        recordHistory(review, decision, before, reason);
        emitAudit(review, decision, before, clientIp);

        log.info("Moderator escalate review: reviewId={} moderatorId={} reason.length={}",
                reviewId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== HELPERS ========================

    private Review loadAndGuard(String reviewId) {
        if (!StringUtils.hasText(reviewId)) {
            throw new BadRequestException("Review ID không hợp lệ");
        }
        // Phase 4 fix: support BOTH String _id and ObjectId _id.
        // Bug history: reviewRepository.findById(String) silently fails to match a
        // String _id stored in MongoDB because Spring Data's query translator
        // sends `{"id": "<hex>"}` (Java field name) instead of `{"_id": "<hex>"}`.
        // Workaround: query the collection directly via MongoTemplate. String
        // lookup FIRST, then ObjectId.
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            org.bson.Document raw = mongoTemplate.getCollection("reviews")
                    .find(new org.bson.Document("_id", reviewId))
                    .first();
            if (raw == null && reviewId.length() == 24 && reviewId.matches("[0-9a-fA-F]+")) {
                org.bson.types.ObjectId oid = new org.bson.types.ObjectId(reviewId);
                raw = mongoTemplate.getCollection("reviews")
                        .find(new org.bson.Document("_id", oid))
                        .first();
            }
            if (raw != null) {
                if (!raw.containsKey("_class")) {
                    raw.put("_class", Review.class.getName());
                }
                review = mongoTemplate.getConverter().read(Review.class, raw);
                if (review.getId() == null) {
                    review.setId(raw.get("_id") instanceof org.bson.types.ObjectId
                            ? raw.get("_id").toString()
                            : (String) raw.get("_id"));
                }
            }
        }
        if (review == null) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + reviewId);
        }
        return review;
    }

    private static boolean isFinalState(ModerationStatus status) {
        return status == ModerationStatus.APPROVED
                || status == ModerationStatus.REJECTED
                || status == ModerationStatus.ESCALATED;
    }

    /**
     * Phase 1 — Source-of-truth reconciliation. The legacy {@code Review.moderationStatus}
     * field (ReviewModerationStatus) is still read by the Moderator dashboard counts
     * ({@code reviewService.getReviewsByModerationStatus(...)}). The new pipeline
     * ({@code pipelineModerationStatus}) is the canonical state, but to keep dashboard
     * counts consistent we sync the legacy field whenever the pipeline transitions to a
     * terminal state. HIDE keeps the legacy status untouched (it is independent — see
     * {@code Review.hidden}).
     */
    private static void syncLegacyModerationStatus(Review review, ModerationStatus newPipeline) {
        if (review == null || newPipeline == null) return;
        ReviewModerationStatus legacy = switch (newPipeline) {
            case APPROVED -> ReviewModerationStatus.VISIBLE;
            case REJECTED -> ReviewModerationStatus.DELETED;
            case ESCALATED -> ReviewModerationStatus.REPORTED;
            default -> null;
        };
        if (legacy != null) {
            review.setModerationStatus(legacy);
        }
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Reason is required and must not be blank");
        }
    }

    private void recordHistory(Review review, ReviewModerationDecision decision,
                                ModerationStatus before, String reason) {
        ModerationHistory history = ModerationHistory.builder()
                .resourceType("REVIEW")
                .resourceId(review.getId())
                .resourceName(review.getUserName() != null
                        ? "Review by " + review.getUserName()
                        : "Review " + review.getId())
                .action(com.ecommerce.cnj70.enums.ModerationAction.valueOf(decision.getAction()))
                .beforeStatus(before)
                .afterStatus(decision.getNewStatus())
                .reason(reason)
                .moderatorId(decision.getModeratorId())
                .moderatorEmail(decision.getModeratorEmail())
                .moderatorRole(UserRole.MODERATOR)
                .build();
        try {
            historyRepository.save(history);
        } catch (RuntimeException ex) {
            log.error("Failed to persist ModerationHistory for reviewId={} action={}",
                    review.getId(), decision.getAction(), ex);
        }
    }

    private void emitAudit(Review review, ReviewModerationDecision decision,
                            ModerationStatus before, String clientIp) {
        AuditEvent event = AuditEvent.builder()
                .actorId(decision.getModeratorId())
                .actorEmail(decision.getModeratorEmail())
                .role(UserRole.MODERATOR)
                .action("REVIEW_" + decision.getAction())
                .resourceType("REVIEW")
                .resourceId(review.getId())
                .reason(decision.getReason())
                .before(before != null ? before.name() : "NULL")
                .after(decision.getNewStatus() != null ? decision.getNewStatus().name() : "HIDDEN")
                .ip(clientIp)
                .createdAt(decision.getDecidedAt())
                .build();
        try {
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.error("Failed to emit AuditEvent for reviewId={} action={}",
                    review.getId(), decision.getAction(), ex);
        }
    }

    /** Reused from Phase 2A — locks out LOCKED/non-MODERATOR accounts. */
    private static final class ModeratorGuard {
        static void verify(CustomUserDetails moderator) {
            if (moderator == null) {
                throw new UnauthorizedException("Yêu cầu đăng nhập Moderator");
            }
            if (!"MODERATOR".equals(moderator.getRole())) {
                throw new UnauthorizedException("Tài khoản không có quyền Moderator");
            }
            if (!moderator.isEnabled() || !moderator.isAccountNonLocked()) {
                throw new UnauthorizedException("Tài khoản Moderator đã bị khóa hoặc chưa kích hoạt");
            }
        }
    }
}
