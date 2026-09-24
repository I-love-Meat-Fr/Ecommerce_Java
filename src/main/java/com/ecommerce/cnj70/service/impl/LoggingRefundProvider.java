package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.RefundRequest;
import com.ecommerce.cnj70.enums.RefundStatus;
import com.ecommerce.cnj70.service.RefundProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Phase 3A §28 — Default log-only RefundProvider.
 *
 * <p><b>INTEGRATION-READY placeholder</b>. Khi production payment provider
 * (Stripe / Momo / Internal Ledger) wire vào bằng {@code @Primary}, bean này
 * sẽ bị thay thế (qua {@link ConditionalOnMissingBean}).</p>
 *
 * <h3>Hard rules (Phase 3A)</h3>
 * <ul>
 *     <li>§29 — KHÔNG set status = SUCCEEDED, KHÔNG cộng balance. Method chỉ log.</li>
 *     <li>§28 — Phase 3A không fake production refund result.</li>
 * </ul>
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "productionRefundProvider")
public class LoggingRefundProvider implements RefundProvider {

    @Override
    public RefundRequest submit(RefundRequest refund) {
        if (refund == null) {
            throw new IllegalArgumentException("refund is null");
        }
        log.warn("[Phase 3A INTEGRATION-READY] RefundProvider.submit: id={} customerId={} amount={} status={}",
                refund.getId(), refund.getCustomerId(),
                refund.getDeclaredAmount(), refund.getStatus());
        // KHÔNG đổi status thành SUCCEEDED. KHÔNG cộng tiền. Đây là log-only no-op.
        // Provider thật sẽ đổi sang PROCESSING/SUCCEEDED thông qua webhook/poll.
        return refund;
    }

    @Override
    public RefundRequest pollStatus(RefundRequest refund) {
        log.warn("[Phase 3A INTEGRATION-READY] RefundProvider.pollStatus: id={} status={}",
                refund != null ? refund.getId() : null,
                refund != null ? refund.getStatus() : null);
        return refund;
    }

    @Override
    public RefundRequest cancel(RefundRequest refund) {
        log.warn("[Phase 3A INTEGRATION-READY] RefundProvider.cancel: id={}", refund != null ? refund.getId() : null);
        // Trong log-only mode, cancel chỉ là no-op.
        if (refund != null
                && (refund.getStatus() == RefundStatus.REQUESTED
                    || refund.getStatus() == RefundStatus.PROCESSING)) {
            refund.setStatus(RefundStatus.CANCELLED);
        }
        return refund;
    }

    @Override
    public BigDecimal deriveDeclaredAmount(String orderId) {
        // Phase 3A §30 — không tự quyết amount. Trả về null để caller biết
        // cần policy/provider quyết định.
        log.warn("[Phase 3A INTEGRATION-READY] RefundProvider.deriveDeclaredAmount: orderId={}", orderId);
        return null;
    }
}
