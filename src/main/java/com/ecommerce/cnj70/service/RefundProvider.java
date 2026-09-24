package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.RefundRequest;

import java.math.BigDecimal;

/**
 * Phase 3A §28 — Refund Provider Integration Seam.
 *
 * <p>Đây là integration contract giữa {@link RefundRequest} domain và external
 * financial backend (Stripe, Momo, Internal Ledger, Banking…).</p>
 *
 * <h3>Hard rules (Phase 3A)</h3>
 * <ul>
 *     <li>§29 — Tuyệt đối không fake: không tự cộng balance, không tự set
 *         {@code status = SUCCEEDED} khi provider chưa xác nhận.</li>
 *     <li>§30 — Provider quyết định settledAmount. Code chỉ truyền declaredAmount
 *         và chờ provider phản hồi.</li>
 *     <li>§28 — Nếu provider chưa có → default impl là log-only no-op (an toàn).</li>
 * </ul>
 *
 * <p>Production implementation sẽ được wire vào bởi payment team (Phase 3B/Phase 4)
 * thông qua {@code @Primary} hoặc {@code @ConditionalOnProperty}.</p>
 */
public interface RefundProvider {

    /**
     * Submit Refund request tới external provider.
     *
     * <p>Implementation KHÔNG được set {@code status = SUCCEEDED} hoặc cộng tiền
     * nếu provider chưa xác nhận. Sau khi submit, status là {@code PROCESSING}
     * hoặc {@code REQUESTED} tuỳ provider phản hồi.</p>
     *
     * @param refund RefundRequest đã được persist (status = REQUESTED)
     * @return RefundRequest đã được cập nhật status + providerTransactionId
     */
    RefundRequest submit(RefundRequest refund);

    /**
     * Query provider cho status hiện tại (poll-based fallback khi webhook chưa sẵn).
     *
     * @param refund RefundRequest có providerTransactionId
     * @return RefundRequest với status/settledAmount/providerMessage cập nhật
     */
    RefundRequest pollStatus(RefundRequest refund);

    /**
     * Cancel một Refund request đang ở REQUESTED/PROCESSING.
     *
     * @return RefundRequest với status = CANCELLED nếu provider cho phép
     */
    RefundRequest cancel(RefundRequest refund);

    /**
     * Phase 3A §30 — helper: derive declaredAmount nếu cần. Phase 3A không tự
     * tính (Phase 3A §6 — không tự đặt business rule). Implementation có thể
     * delegate sang Provider hoặc dùng Order.totalAmount theo policy.
     *
     * @param orderId Order liên quan
     * @return declaredAmount (BigDecimal) hoặc null nếu provider không thể quyết
     */
    BigDecimal deriveDeclaredAmount(String orderId);
}
