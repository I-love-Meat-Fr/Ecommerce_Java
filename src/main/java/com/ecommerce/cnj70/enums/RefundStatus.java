package com.ecommerce.cnj70.enums;

/**
 * Phase 3A §34 — Refund Status.
 *
 * <p>Bám sát Phase 3A §29-§30: KHÔNG tự quyết định refund amount/deadline/fee.
 * Status này chỉ phản ánh trạng thái của Refund lifecycle do Provider (external)
 * quyết định; Phase 3A expose contract để Phase 3B+ wire vào provider thật.</p>
 */
public enum RefundStatus {
    /** Refund vừa được request bởi Complaint/Return workflow. */
    REQUESTED,
    /** Provider đã nhận & đang xử lý. */
    PROCESSING,
    /** Provider xác nhận tiền đã về Customer. */
    SUCCEEDED,
    /** Provider từ chối (insufficient funds, fraud check, etc.). */
    FAILED,
    /** Hủy bởi hệ thống / Customer / Vendor. */
    CANCELLED
}
