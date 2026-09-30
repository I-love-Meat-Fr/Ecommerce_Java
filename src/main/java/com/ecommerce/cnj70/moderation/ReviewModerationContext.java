package com.ecommerce.cnj70.moderation;

import com.ecommerce.cnj70.document.Review;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Implementation của {@link ModerationContext} cho {@code Review}.
 *
 * <p>Review chỉ có một field văn bản chính là {@link Review#getComment()}.
 * Review không có ảnh (theo schema hiện tại) nên {@link #getImageUrls()} trả
 * về danh sách rỗng.</p>
 *
 * <p>{@link #getAuthorId()} là {@code Review.userId} — dùng để check spam:
 * cùng user đăng N review trong thời gian ngắn.</p>
 */
@Getter
public class ReviewModerationContext implements ModerationContext {

    private final String targetId;
    private final ModerationTargetType targetType = ModerationTargetType.REVIEW;
    private final String authorId; // userId
    private final String text;
    private final List<String> imageUrls; // luôn rỗng cho Review
    private final LocalDateTime submittedAt;
    private final SharedState sharedState;
    private final Review review; // raw reference

    public ReviewModerationContext(Review review,
                                   SharedState sharedState,
                                   LocalDateTime submittedAt) {
        if (review == null) {
            throw new IllegalArgumentException("review không được null");
        }
        this.review = review;
        this.targetId = review.getId();
        this.authorId = review.getUserId();
        this.text = review.getComment();
        this.imageUrls = Collections.emptyList();
        this.submittedAt = submittedAt != null ? submittedAt : LocalDateTime.now();
        this.sharedState = sharedState != null ? sharedState : SharedState.builder().build();
    }
}
