package com.ecommerce.cnj70.moderation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Yêu cầu #1 — Duplicate Content Checker (mock logic).
 *
 * <p>Phát hiện hai dạng spam/duplicate:</p>
 * <ol>
 *   <li><b>Duplicate product</b>: Vendor đăng sản phẩm mà ảnh trùng với sản phẩm
 *       đã tồn tại (cùng URL ảnh trong collection {@code products}).</li>
 *   <li><b>Review spam</b>: cùng một user đăng N review có comment trùng nhau
 *       trong khoảng thời gian ngắn (vd 3 review cùng nội dung trong 10 phút).</li>
 * </ol>
 *
 * <p>Mức độ: cả 2 dạng đều {@link ModerationDecision.Severity#SUSPICIOUS}
 * (không FATAL — vì trùng ảnh có thể hợp lệ nếu vendor bán cùng catalog).</p>
 *
 * <p>Class này reuse {@link ProductRepository} (URL-based dup) và
 * {@link ReviewRepository} (text dup theo user) — không cần binary hash.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateContentChecker implements ModerationChecker {

    public static final String ID = "DUPLICATE_CONTENT";
    public static final String FLAG_PRODUCT = "DUPLICATE_PRODUCT";
    public static final String FLAG_REVIEW_SPAM = "REVIEW_SPAM";

    /** Ngưỡng duplicate text của cùng user trong cùng khoảng thời gian. */
    private static final int REVIEW_SPAM_THRESHOLD = 3;
    private static final Duration REVIEW_SPAM_WINDOW = Duration.ofMinutes(10);

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public int order() {
        // Chạy sau blacklist, trước các checker nặng khác.
        return 20;
    }

    @Override
    public boolean supports(ModerationContext context) {
        // Hỗ trợ cả Product (ảnh trùng) và Review (text spam).
        return context != null
                && (context.getTargetType() == ModerationTargetType.PRODUCT
                 || context.getTargetType() == ModerationTargetType.REVIEW);
    }

    @Override
    public ModerationDecision check(ModerationContext context) {
        if (context == null) {
            return ModerationDecision.pass();
        }

        return switch (context.getTargetType()) {
            case PRODUCT -> checkProductDuplicate(context);
            case REVIEW -> checkReviewSpam(context);
        };
    }

    // ===== Product duplicate =====

    private ModerationDecision checkProductDuplicate(ModerationContext context) {
        if (!(context instanceof ProductModerationContext pctx)) {
            return ModerationDecision.pass();
        }
        Product product = pctx.getProduct();
        if (product == null) {
            return ModerationDecision.pass();
        }
        List<String> imageUrls = product.getImageUrls();
        if (imageUrls == null || imageUrls.isEmpty()) {
            return ModerationDecision.pass();
        }

        String currentId = product.getId();
        for (String url : imageUrls) {
            if (url == null || url.isBlank()) continue;
            var match = productRepository.findFirstByImageUrlsContainingAndIdNot(url, currentId);
            if (match.isPresent()) {
                Product other = match.get();
                String msg = "Sản phẩm chứa ảnh trùng với sản phẩm khác (id=" + other.getId()
                        + ", name='" + other.getName() + "')";
                log.info("[DuplicateContent] SUSPICIOUS product id={} dup-image with id={}",
                        currentId, other.getId());
                return ModerationDecision.suspicious(ID, FLAG_PRODUCT, msg);
            }
        }

        return ModerationDecision.pass();
    }

    // ===== Review spam (same user, similar text, short window) =====

    private ModerationDecision checkReviewSpam(ModerationContext context) {
        if (!(context instanceof ReviewModerationContext rctx)) {
            return ModerationDecision.pass();
        }
        Review review = rctx.getReview();
        if (review == null || review.getComment() == null || review.getComment().isBlank()) {
            return ModerationDecision.pass();
        }
        String userId = rctx.getAuthorId();
        if (userId == null || userId.isBlank()) {
            return ModerationDecision.pass();
        }

        LocalDateTime windowStart = rctx.getSubmittedAt().minus(REVIEW_SPAM_WINDOW);
        List<Review> recentReviews = reviewRepository.findByUserId(userId);

        long similarCount = recentReviews.stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(windowStart))
                // ignore chính review hiện tại (nếu là update path, id có thể trùng)
                .filter(r -> !r.getId().equals(review.getId()))
                .filter(r -> r.getComment() != null
                        && r.getComment().trim().equalsIgnoreCase(review.getComment().trim()))
                .count();

        if (similarCount >= REVIEW_SPAM_THRESHOLD) {
            String msg = "User " + userId + " đã đăng " + similarCount
                    + " review có nội dung trùng trong vòng "
                    + REVIEW_SPAM_WINDOW.toMinutes() + " phút";
            log.info("[DuplicateContent] SUSPICIOUS review spam user={} count={}",
                    userId, similarCount);
            return ModerationDecision.suspicious(ID, FLAG_REVIEW_SPAM, msg);
        }

        return ModerationDecision.pass();
    }
}
