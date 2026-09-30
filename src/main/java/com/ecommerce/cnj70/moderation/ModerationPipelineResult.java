package com.ecommerce.cnj70.moderation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Kết quả tổng hợp của {@link ModerationPipelineService} sau khi chạy toàn bộ
 * chain {@link ModerationChecker}.
 *
 * <p>Quy tắc tổng hợp:</p>
 * <ul>
 *   <li>Có ít nhất 1 {@link ModerationDecision.Decision#FATAL} →
 *       <b>short-circuit</b> ngay, kết quả là FATAL (với Product →
 *       {@link ProductStatus#REJECTED_AUTO}; với Review → HIDDEN).</li>
 *   <li>Không có FATAL nhưng có ≥ 1 SUSPICIOUS → tổng hợp là SUSPICIOUS
 *       (Product → MANUAL_REVIEW; Review → tạo ReportCase).</li>
 *   <li>Tất cả PASS → kết quả là PASS (Product → ACTIVE/AUTO_PASSED;
 *       Review → VISIBLE).</li>
 * </ul>
 *
 * <p>Class này cung cấp {@link #applyToProduct(Product)} và
 * {@link #applyToReview(Review)} để caller dễ dàng apply kết quả lên document
 * mà không cần tự if/else.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationPipelineResult {

    /** Severity tổng hợp cuối cùng. */
    private ModerationDecision.Severity severity;

    /** Các flagCode từ tất cả checker đã chạy. */
    @Builder.Default
    private List<String> autoFlags = new ArrayList<>();

    /** Tất cả reason message từ các checker. */
    @Builder.Default
    private List<String> reasons = new ArrayList<>();

    /** Danh sách decision chi tiết (để audit / debug). */
    @Builder.Default
    private List<ModerationDecision> decisions = new ArrayList<>();

    // ===== Convenience =====

    public boolean isPass() {
        return severity == ModerationDecision.Severity.PASS;
    }

    public boolean isSuspicious() {
        return severity == ModerationDecision.Severity.SUSPICIOUS;
    }

    public boolean isFatal() {
        return severity == ModerationDecision.Severity.FATAL;
    }

    // ===== Factory =====

    public static ModerationPipelineResult pass() {
        return ModerationPipelineResult.builder()
                .severity(ModerationDecision.Severity.PASS)
                .autoFlags(new ArrayList<>())
                .reasons(new ArrayList<>())
                .decisions(new ArrayList<>())
                .build();
    }
}
