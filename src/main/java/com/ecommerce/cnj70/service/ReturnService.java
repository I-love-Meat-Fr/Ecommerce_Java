package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.ReturnRequest;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 3A §23 — Return Service contract.
 *
 * <p><b>INTEGRATION-READY</b>: interface đã sẵn sàng nhưng chưa có production
 * implementation (Phase 3A §28-§29 cấm fake refund/return workflow).
 * Ownership/security enforced tại implementation.</p>
 *
 * <p>Phase 3A chỉ expose:</p>
 * <ul>
 *     <li>Customer request Return từ Complaint (ownership-checked)</li>
 *     <li>Customer/Vendor list Returns của mình</li>
 *     <li>Customer/Vendor/Moderator view detail (ownership-checked)</li>
 * </ul>
 *
 * <p>Vendor approve/reject Return, Moderator escalation, Refund trigger
 * thuộc Phase 3B/3C theo Policy.</p>
 */
public interface ReturnService {

    /**
     * Customer request Return từ một Complaint (hoặc standalone).
     *
     * @param orderId Order gốc (ownership verified against authenticated customer)
     * @param orderItemIds optional productIds trong Order (verified)
     * @param reason lý do (text, không enum)
     * @param evidence danh sách reference URL/metadata
     * @param user authenticated CUSTOMER
     * @return persisted ReturnRequest với status = REQUESTED
     */
    ReturnRequest requestReturn(String orderId, java.util.List<String> orderItemIds,
                                String reason, java.util.List<String> evidence,
                                CustomUserDetails user);

    /** Customer xem Return của mình. */
    Page<ReturnRequest> listMyReturns(CustomUserDetails user, Pageable pageable);

    /** Vendor xem Return của Shop mình. */
    Page<ReturnRequest> listShopReturns(CustomUserDetails user, Pageable pageable);

    /**
     * Universal detail — ownership/scope enforced by implementation.
     * Customer thấy Return của mình; Vendor thấy Return của Shop mình; Moderator/Admin thấy mọi case.
     */
    ReturnRequest getById(String returnId, CustomUserDetails user);
}
