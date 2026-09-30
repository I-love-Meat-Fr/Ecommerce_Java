package com.ecommerce.cnj70.moderation;

/**
 * Phân loại ngữ cảnh mà {@link ModerationChecker} đang xử lý.
 *
 * <p>Dùng để:</p>
 * <ul>
 *   <li>{@link ModerationPipelineService} quyết định áp dụng {@link ModerationChecker}
 *       nào cho context hiện tại (vd: blacklist áp dụng cho cả Product &amp; Review;
 *       duplicate-content có thể khác nhau giữa 2 loại).</li>
 *   <li>{@link ModerationDecision#getTargetStatus()} hoặc
 *       {@link ModerationDecision#getReviewStatus()} biết cách map verdict
 *       sang enum trạng thái phù hợp (ProductStatus vs ReviewModerationStatus).</li>
 * </ul>
 */
public enum ModerationTargetType {
    PRODUCT,
    REVIEW
}
