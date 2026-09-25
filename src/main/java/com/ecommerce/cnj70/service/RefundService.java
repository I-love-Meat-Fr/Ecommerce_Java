package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.RefundRequest;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 3A §28 — Refund Service contract.
 *
 * <p><b>INTEGRATION-READY</b>. Chỉ tạo Refund record + delegate sang
 * {@link RefundProvider}. KHÔNG tự quyết amount, KHÔNG tự cộng balance.</p>
 *
 * <p>Phase 3A scope: Customer request refund từ Complaint/Return; ownership-checked
 * list/get. Approve / Reject / Settle thuộc Provider + Phase 3B.</p>
 */
public interface RefundService {

    /**
     * Customer / Vendor / Moderator request Refund cho Order (qua Complaint hoặc Return).
     *
     * @param complaintId optional
     * @param returnId    optional
     * @param orderId     bắt buộc (ownership verified)
     * @param declaredAmount optional — Phase 3A §30 cho phép null; Provider quyết
     * @param user        authenticated actor
     * @return persisted RefundRequest (status = REQUESTED) sau khi RefundProvider.submit được gọi
     */
    RefundRequest requestRefund(String complaintId, String returnId, String orderId,
                                 java.math.BigDecimal declaredAmount, CustomUserDetails user);

    /** Customer xem Refund của mình. */
    Page<RefundRequest> listMyRefunds(CustomUserDetails user, Pageable pageable);

    /** Vendor xem Refund của Shop mình. */
    Page<RefundRequest> listShopRefunds(CustomUserDetails user, Pageable pageable);

    /** Universal detail — ownership-checked. */
    RefundRequest getById(String refundId, CustomUserDetails user);

    /**
     * Phase 3A — poll status (gọi RefundProvider.pollStatus). Hook cho Phase 3C Scheduler.
     */
    RefundRequest pollStatus(String refundId, CustomUserDetails user);
}
