package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.enums.ProductStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Group 1 — Pipeline State Machine cho Product Moderation.
 *
 * <p>Enum tách biệt rõ ràng khỏi {@link ProductStatus} (enum hiển thị public)
 * để state machine có thêm các trạng thái nội bộ không cần lộ ra ngoài:</p>
 *
 * <ul>
 *   <li>{@link #PENDING_AUTO} — vừa submit, đang chạy pipeline.</li>
 *   <li>{@link #AUTO_PASSED} — pipeline PASS, chờ flip sang ACTIVE.</li>
 *   <li>{@link #AUTO_REJECTED} — pipeline HARD_REJECT.</li>
 *   <li>{@link #PENDING_MANUAL} — pipeline SUSPICIOUS, Moderator xử lý.</li>
 *   <li>{@link #APPROVED} — Moderator/Admin duyệt tay, đã live.</li>
 *   <li>{@link #REJECTED} — Moderator/Admin từ chối.</li>
 *   <li>{@link #ESCALATED} — Moderator chuyển Admin.</li>
 * </ul>
 *
 * <p>State machine đảm bảo <b>Vendor không bypass</b> được pipeline: từ
 * {@link #PENDING_AUTO} chỉ có thể đi tới {@code AUTO_PASSED} /
 * {@code AUTO_REJECTED} / {@code PENDING_MANUAL}. Vendor không thể set
 * thẳng sang APPROVED.</p>
 */
public enum ProductModerationState {

    PENDING_AUTO,
    AUTO_PASSED,
    AUTO_REJECTED,
    PENDING_MANUAL,
    APPROVED,
    REJECTED,
    ESCALATED;

    /**
     * Bảng transition hợp lệ. Mọi cặp (from, to) KHÔNG có trong map này
     * đều bị {@link ProductModerationStateMachine} từ chối.
     *
     * <p>Key = from state, Value = tập các to state hợp lệ.</p>
     */
    private static final Map<ProductModerationState, Set<ProductModerationState>> ALLOWED =
            Map.of(
                    PENDING_AUTO, EnumSet.of(AUTO_PASSED, AUTO_REJECTED, PENDING_MANUAL),
                    AUTO_PASSED,  EnumSet.of(APPROVED, PENDING_MANUAL),
                    AUTO_REJECTED, EnumSet.of(REJECTED, PENDING_MANUAL),
                    PENDING_MANUAL, EnumSet.of(APPROVED, REJECTED, ESCALATED),
                    ESCALATED,     EnumSet.of(APPROVED, REJECTED),
                    APPROVED,      EnumSet.noneOf(ProductModerationState.class),
                    REJECTED,      EnumSet.noneOf(ProductModerationState.class)
            );

    /**
     * Kiểm tra transition {@code this} → {@code target} có hợp lệ không.
     * Method này chỉ đọc bảng ALLOWED — không side effect.
     */
    public boolean canTransitionTo(ProductModerationState target) {
        if (target == null) return false;
        return ALLOWED.getOrDefault(this, EnumSet.noneOf(ProductModerationState.class))
                .contains(target);
    }

    /**
     * Trả về tập state có thể đi tới từ {@code this}.
     */
    public Set<ProductModerationState> allowedNextStates() {
        return EnumSet.copyOf(ALLOWED.getOrDefault(this, EnumSet.noneOf(ProductModerationState.class)));
    }

    /**
     * Map sang {@link ProductStatus} (enum public-facing) tương ứng.
     * Hai enum tách biệt vì một số state nội bộ (vd ESCALATED) cần xử lý
     * riêng trước khi đổi sang ProductStatus.
     */
    public ProductStatus toProductStatus() {
        return switch (this) {
            case PENDING_AUTO -> ProductStatus.PENDING_AUTO;
            case AUTO_PASSED, APPROVED -> ProductStatus.ACTIVE;
            case AUTO_REJECTED, REJECTED -> ProductStatus.REJECTED_AUTO;
            case PENDING_MANUAL, ESCALATED -> ProductStatus.MANUAL_REVIEW;
        };
    }
}
