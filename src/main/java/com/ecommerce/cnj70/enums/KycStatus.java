package com.ecommerce.cnj70.enums;

/**
 * TASK #16 / Phase 4 - KYC Status (consolidated).
 *
<<<<<<< HEAD
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
=======
 * Luồng trạng thái:
 *   NOT_SUBMITTED → PENDING_THIRD_PARTY → PENDING_ADMIN → APPROVED
 *                                       → THIRD_PARTY_REJECTED → (vendor resubmit) → PENDING_THIRD_PARTY
 *                  ADMIN_REJECTED → (vendor resubmit) → PENDING_THIRD_PARTY
 *                  SUSPENDED → (admin unsuspend) → PENDING_ADMIN
 *
 * - NOT_SUBMITTED:        Vendor chưa nộp hồ sơ
 * - PENDING_THIRD_PARTY:  Hồ sơ đã gửi sang provider, đang chờ xác minh tự động
 * - THIRD_PARTY_REJECTED: Bên thứ ba từ chối, vendor có thể sửa và gửi lại
 * - PENDING_ADMIN:        Bên thứ ba OK, đang chờ admin duyệt
 * - APPROVED:             Admin duyệt, vendor được tạo Shop
 * - ADMIN_REJECTED:       Admin từ chối, vendor có thể sửa và gửi lại
 * - SUSPENDED:            Shop bị đình chỉ (vi phạm hoặc KYC hết hạn)
 *
 * Đây là enum DUY NHẤT dùng cho toàn hệ thống:
 *  - VendorKycService / VendorKycServiceImpl: dùng cho flow nộp KYC của vendor
 *  - AdminKycService / AdminKycServiceImpl:   dùng cho Admin duyệt
 *  - ThirdPartyKycVerifier:                  cập nhật trạng thái từ phía provider
 *  - KycProfile.status / User.kycStatus:     denormalized để check nhanh
 *
 * Lưu ý: Shop.kycStatus vẫn tồn tại cho backward-compatible với KycService
 * (legacy) nhưng KycService sẽ được migrate sang dùng KycStatus mới này.
 */
public enum KycStatus {
    /** Vendor chưa nộp hồ sơ */
    NOT_SUBMITTED,

    /** Hồ sơ đã gửi sang provider, đang chờ xác minh tự động */
    PENDING_THIRD_PARTY,

    /** Bên thứ ba từ chối, vendor có thể sửa và gửi lại */
    THIRD_PARTY_REJECTED,

    /** Bên thứ ba OK, đang chờ admin duyệt */
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
    PENDING_ADMIN,

    /** 3rd-party từ chối → vendor sửa lại. */
    THIRD_PARTY_REJECTED,

<<<<<<< HEAD
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
=======
    /** Admin từ chối, vendor có thể sửa và gửi lại */
    ADMIN_REJECTED,

    /** Bị đình chỉ vĩnh viễn (do vi phạm) */
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
    SUSPENDED;

    /**
     * Vendor có thể (re)submit hồ sơ ở trạng thái này không?
     * Tương đương {@code KycProfile.canVendorSubmit()}.
     */
    public boolean canSubmit() {
        return this == NOT_SUBMITTED || this == THIRD_PARTY_REJECTED;
    }
<<<<<<< HEAD
=======

    /**
     * Vendor đã vượt qua cả 3rd-party lẫn admin → được tạo shop
     */
    public boolean isApprovedForShop() {
        return this == APPROVED;
    }

    /**
     * Vendor đã có hồ sơ (bất kỳ trạng thái nào)
     */
    public boolean hasProfile() {
        return this != NOT_SUBMITTED;
    }
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
}
