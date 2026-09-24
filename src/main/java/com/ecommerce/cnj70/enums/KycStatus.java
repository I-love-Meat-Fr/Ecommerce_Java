package com.ecommerce.cnj70.enums;

/**
 * TASK #16 — KYC Status cho Shop verification.
 *
 * <p>Hỗ trợ 2 luồng workflow đồng thời:
 * <ul>
 *   <li><b>Legacy (ShopStatus-based)</b>: PENDING_KYC → PENDING_PROVIDER → APPROVED / KYC_REJECTED / SUSPENDED</li>
 *   <li><b>Multi-step (KycProfile-based)</b>:
 *     NOT_SUBMITTED → PENDING_THIRD_PARTY → PENDING_ADMIN → APPROVED / THIRD_PARTY_REJECTED / ADMIN_REJECTED</li>
 * </ul>
 *
 * <p>Mở rộng để đồng bộ với KYC vendor-module merge đã có sẵn:
 * thêm {@code NOT_SUBMITTED, PENDING_THIRD_PARTY, THIRD_PARTY_REJECTED,
 * PENDING_ADMIN, ADMIN_REJECTED} + method {@link #canSubmit()}.
 *
 * <p>Quy tắc vendor-submit:
 * <ul>
 *   <li>NOT_SUBMITTED → lần submit đầu</li>
 *   <li>THIRD_PARTY_REJECTED → vendor sửa và gửi lại</li>
 *   <li>Các trạng thái khác → KHÔNG được submit (đang chờ 3rd-party / admin, hoặc đã APPROVED)</li>
 * </ul>
 */
public enum KycStatus {

    /** Chưa nộp hồ sơ KYC. */
    NOT_SUBMITTED,

    /** Hồ sơ đã nộp, đang chờ 3rd-party xác minh. */
    PENDING_THIRD_PARTY,

    /** 3rd-party OK, đang chờ admin duyệt. */
    PENDING_ADMIN,

    /** 3rd-party từ chối → vendor sửa lại. */
    THIRD_PARTY_REJECTED,

    /** Admin từ chối → vendor liên hệ hỗ trợ (KHÔNG tự resubmit). */
    ADMIN_REJECTED,

    // ===== Legacy values (giữ để không phá code cũ) =====
    /** Vendor chưa nộp KYC (legacy). */
    PENDING_KYC,
    /** KYC đã gửi sang provider, đang chờ kết quả (legacy). */
    PENDING_PROVIDER,
    /** KYC được duyệt. */
    APPROVED,
    /** KYC bị từ chối (legacy). */
    KYC_REJECTED,
    /** Shop bị tạm ngưng (do vi phạm hoặc KYC hết hạn). */
    SUSPENDED;

    /**
     * Vendor có thể (re)submit hồ sơ ở trạng thái này không?
     * Tương đương {@code KycProfile.canVendorSubmit()}.
     */
    public boolean canSubmit() {
        return this == NOT_SUBMITTED || this == THIRD_PARTY_REJECTED;
    }
}
