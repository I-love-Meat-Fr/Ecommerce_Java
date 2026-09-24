package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.VoucherApplicationGateway;
import com.ecommerce.cnj70.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Phase 4B — Default Voucher application gateway (facade over VoucherService).
 *
 * <p>Backed by atomic Mongo operations defined in
 * {@link VoucherService#tryIncrementUsed} and
 * {@link VoucherService#tryDecrementUsed}.</p>
 *
 * <p>Per Phase 4B §30 / §34, voucher consumption is coupled to the order
 * lifecycle: only consumed after the order is successfully created; released
 * on cancellation/refund. The Order Backend (Phase 5 / Anh Quân) is the
 * integration point.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoucherApplicationGatewayImpl implements VoucherApplicationGateway {

    private final VoucherService voucherService;
    private final AuditEventWriter auditEventWriter;

    @Override
    public boolean reserveVoucher(String voucherId, String orderId) {
        boolean ok = voucherService.tryIncrementUsed(voucherId);
        emitVoucherOrderAudit(voucherId, orderId, ok ? "RESERVED" : "RESERVE_FAILED",
                ok ? "Voucher reserved for order" : "Voucher reservation rejected (exhausted/inactive)");
        return ok;
    }

    @Override
    public boolean releaseVoucher(String voucherId, String orderId) {
        boolean ok = voucherService.tryDecrementUsed(voucherId);
        emitVoucherOrderAudit(voucherId, orderId, ok ? "RELEASED" : "RELEASE_FAILED",
                ok ? "Voucher released back to pool (order cancelled/refunded)"
                   : "Voucher release no-op (already at 0 / missing)");
        return ok;
    }

    private void emitVoucherOrderAudit(String voucherId, String orderId, String action, String reason) {
        try {
            AuditEvent ev = AuditEvent.builder()
                    .role(UserRole.CUSTOMER)
                    .action("VOUCHER_" + action)
                    .resourceType("VOUCHER")
                    .resourceId(voucherId)
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .build();
            // For order-related events, embed orderId in 'after' field so
            // it's recoverable from the audit log.
            ev.setAfter("orderId=" + orderId);
            auditEventWriter.write(ev);
        } catch (Exception ex) {
            log.warn("Voucher/order audit emit failed: {}", ex.getMessage());
        }
    }
}
