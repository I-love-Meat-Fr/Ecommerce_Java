package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * C1 — Kết quả tổng hợp của Auto Moderation Pipeline
 * (xem {@code AutoModerationService.runProductChecks(Product)}).
 *
 * <p>Pipeline quyết định {@code targetStatus} cuối cùng dựa trên verdicts
 * của các {@link AutoCheckStrategy}:
 * <ul>
 *   <li>Tất cả {@code PASS} → {@code ACTIVE}</li>
 *   <li>Có ít nhất 1 {@code HARD_REJECT} → {@code REJECTED_AUTO}
 *       (short-circuit)</li>
 *   <li>Có ít nhất 1 {@code SOFT_FLAG} (và không có HARD_REJECT) →
 *       {@code MANUAL_REVIEW}</li>
 * </ul>
 *
 * <p>Các {@code autoFlags} và {@code reasons} chỉ populate khi
 * {@code MANUAL_REVIEW} / {@code REJECTED_AUTO}, dùng để:
 * <ul>
 *   <li>Tạo {@code ReportCase} cho Moderator (source=AUTO)</li>
 *   <li>Ghi AuditLog</li>
 *   <li>Hiển thị lý do cho vendor khi bị reject/manual review</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoModerationResult {

    /**
     * Trạng thái target mà Pipeline recommend cho Product.
     * Caller phải set Product.status = targetStatus và save.
     */
    private ProductStatus targetStatus;

    /** Product đã được evaluate (cùng instance với input — chỉ hỗ trợ inspect). */
    private Product product;

    /**
     * Các autoFlag đã được format {@code "<checkId>:<flagCode>"}.
     * Dùng làm {@code ReportCase.autoFlags}.
     */
    @Builder.Default
    private List<String> autoFlags = new ArrayList<>();

    /** Lý do từ các check (mỗi check đóng góp 1 reason). */
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    /**
     * Convenience: tất cả check đều PASS — Product live.
     */
    public static AutoModerationResult pass(Product product) {
        return AutoModerationResult.builder()
                .product(product)
                .targetStatus(ProductStatus.ACTIVE)
                .autoFlags(new ArrayList<>())
                .reasons(new ArrayList<>())
                .build();
    }

    /**
     * Có SOFT_FLAG — pipeline đẩy vào MANUAL_REVIEW.
     */
    public static AutoModerationResult manualReview(Product product,
                                                     List<String> flags,
                                                     List<String> reasons) {
        return AutoModerationResult.builder()
                .product(product)
                .targetStatus(ProductStatus.MANUAL_REVIEW)
                .autoFlags(flags != null ? flags : new ArrayList<>())
                .reasons(reasons != null ? reasons : new ArrayList<>())
                .build();
    }

    /**
     * Có HARD_REJECT — pipeline auto reject.
     */
    public static AutoModerationResult autoReject(Product product,
                                                   List<String> flags,
                                                   List<String> reasons) {
        return AutoModerationResult.builder()
                .product(product)
                .targetStatus(ProductStatus.REJECTED_AUTO)
                .autoFlags(flags != null ? flags : new ArrayList<>())
                .reasons(reasons != null ? reasons : new ArrayList<>())
                .build();
    }

    /** True nếu pipeline recommend ACTIVE (tất cả check PASS). */
    public boolean isPassed() {
        return targetStatus == ProductStatus.ACTIVE;
    }

    /** True nếu pipeline đẩy sang Manual Review. */
    public boolean isManualReview() {
        return targetStatus == ProductStatus.MANUAL_REVIEW;
    }

    /** True nếu pipeline auto reject. */
    public boolean isAutoRejected() {
        return targetStatus == ProductStatus.REJECTED_AUTO;
    }
}
