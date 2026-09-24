package com.ecommerce.cnj70.service.automation;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * C1 — Kết quả của một {@link AutoCheckStrategy} đơn lẻ.
 *
 * <p>3 outcome (xem {@link Outcome}):
 * <ul>
 *   <li>{@code PASS} — check không phát hiện vấn đề</li>
 *   <li>{@code SOFT_FLAG} — phát hiện dấu hiệu đáng ngờ, cần Moderator review</li>
 *   <li>{@code HARD_REJECT} — phát hiện vi phạm rõ ràng, auto reject</li>
 * </ul>
 *
 * <p>HARD_REJECT khiến orchestrator short-circuit và trả về
 * {@code REJECTED_AUTO} ngay (không chạy tiếp các check sau).
 *
 * <p>SOFT_FLAG khiến orchestrator ghi nhận nhưng tiếp tục chạy check khác để
 * thu thập đủ flags/reasons; cuối pipeline (sau khi đã chạy hết) mới quyết
 * định {@code MANUAL_REVIEW}.
 */
@Getter
@AllArgsConstructor
public class AutoCheckVerdict {

    /** Mức kết quả của check. */
    public enum Outcome {
        /** Check pass — không có vấn đề. */
        PASS,
        /** Check phát hiện vấn đề nhẹ — pipeline sẽ về MANUAL_REVIEW. */
        SOFT_FLAG,
        /** Check phát hiện vi phạm rõ ràng — pipeline sẽ về REJECTED_AUTO. */
        HARD_REJECT
    }

    private final Outcome outcome;

    /** Mã cờ ngắn gọn (vd {@code "BLACKLIST_HIT"}, {@code "FORBIDDEN_CAT"}).
     *  Null khi {@code PASS}. Được combine với {@link AutoCheckStrategy#id()}
     *  để ra autoFlag trên ReportCase: {@code "<id>:<flagCode>"}. */
    private final String flagCode;

    /** Thông điệp lý do cho vendor / moderator. Null khi {@code PASS}. */
    private final String message;

    public static AutoCheckVerdict pass() {
        return new AutoCheckVerdict(Outcome.PASS, null, null);
    }

    public static AutoCheckVerdict softFlag(String flagCode, String message) {
        if (flagCode == null || flagCode.isBlank()) {
            throw new IllegalArgumentException("flagCode phải khác null/rỗng khi SOFT_FLAG");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message phải khác null/rỗng khi SOFT_FLAG");
        }
        return new AutoCheckVerdict(Outcome.SOFT_FLAG, flagCode, message);
    }

    public static AutoCheckVerdict hardReject(String flagCode, String message) {
        if (flagCode == null || flagCode.isBlank()) {
            throw new IllegalArgumentException("flagCode phải khác null/rỗng khi HARD_REJECT");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message phải khác null/rỗng khi HARD_REJECT");
        }
        return new AutoCheckVerdict(Outcome.HARD_REJECT, flagCode, message);
    }

    public boolean isPass() {
        return outcome == Outcome.PASS;
    }

    public boolean isSoftFlag() {
        return outcome == Outcome.SOFT_FLAG;
    }

    public boolean isHardReject() {
        return outcome == Outcome.HARD_REJECT;
    }
}
