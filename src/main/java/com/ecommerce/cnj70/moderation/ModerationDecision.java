package com.ecommerce.cnj70.moderation;

import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Quyết định trả về từ một {@link ModerationChecker} đơn lẻ.
 *
 * <p>Có 3 mức:</p>
 * <ul>
 *   <li>{@link Severity#PASS} — check không phát hiện vấn đề.</li>
 *   <li>{@link Severity#SUSPICIOUS} — phát hiện dấu hiệu đáng ngờ (nhẹ):
 *       với Product → {@link ProductStatus#MANUAL_REVIEW};
 *       với Review → tạo {@code ReportCase} chờ Moderator duyệt.</li>
 *   <li>{@link Severity#FATAL} — phát hiện vi phạm rõ ràng (nặng):
 *       với Product → {@link ProductStatus#REJECTED_AUTO};
 *       với Review → {@link ReviewModerationStatus#HIDDEN} (hoặc DELETED).</li>
 * </ul>
 *
 * <p>Khác với {@code AutoCheckVerdict} cũ (chỉ dành cho Product), class này
 * dùng chung cho cả 2 loại đối tượng. Flag code (vd {@code "BLACKLIST_HIT"})
 * giúp truy vết + tạo {@code ReportCase.autoFlags}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationDecision {

    /** Mức độ vi phạm. */
    public enum Severity {
        /** Pass — không phát hiện vấn đề. */
        PASS,
        /** Đáng ngờ — đẩy sang MANUAL_REVIEW (Product) hoặc tạo ReportCase (Review). */
        SUSPICIOUS,
        /** Vi phạm rõ ràng — auto reject/hide. */
        FATAL
    }

    /** Mức độ. */
    private Severity severity;

    /**
     * Mã cờ ngắn gọn để log + tạo ReportCase.autoFlags.
     * Ví dụ: {@code "BLACKLIST_HIT"}, {@code "DUPLICATE_CONTENT"}.
     * Null khi {@link #severity} = PASS.
     */
    private String flagCode;

    /** Thông điệp lý do (tiếng Việt, hiển thị cho vendor / moderator). */
    private String message;

    /**
     * Mã checker đã phát hiện (vd {@code "BLACKLIST_WORD"}, {@code "DUPLICATE_CONTENT"}).
     * Null khi PASS.
     */
    private String checkerId;

    /** Các flagCode con (nếu checker phát hiện nhiều vấn đề). */
    @Builder.Default
    private List<String> additionalFlags = new ArrayList<>();

    // ===== Factory methods =====

    public static ModerationDecision pass() {
        return ModerationDecision.builder()
                .severity(Severity.PASS)
                .build();
    }

    public static ModerationDecision suspicious(String checkerId, String flagCode, String message) {
        requireCheckerFields(checkerId, flagCode, message);
        return ModerationDecision.builder()
                .severity(Severity.SUSPICIOUS)
                .checkerId(checkerId)
                .flagCode(flagCode)
                .message(message)
                .build();
    }

    public static ModerationDecision fatal(String checkerId, String flagCode, String message) {
        requireCheckerFields(checkerId, flagCode, message);
        return ModerationDecision.builder()
                .severity(Severity.FATAL)
                .checkerId(checkerId)
                .flagCode(flagCode)
                .message(message)
                .build();
    }

    private static void requireCheckerFields(String checkerId, String flagCode, String message) {
        if (checkerId == null || checkerId.isBlank()) {
            throw new IllegalArgumentException("checkerId phải khác null/rỗng");
        }
        if (flagCode == null || flagCode.isBlank()) {
            throw new IllegalArgumentException("flagCode phải khác null/rỗng");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message phải khác null/rỗng");
        }
    }

    // ===== Convenience =====

    public boolean isPass() {
        return severity == Severity.PASS;
    }

    public boolean isSuspicious() {
        return severity == Severity.SUSPICIOUS;
    }

    public boolean isFatal() {
        return severity == Severity.FATAL;
    }
}
