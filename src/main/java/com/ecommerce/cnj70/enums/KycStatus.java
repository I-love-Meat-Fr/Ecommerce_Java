package com.ecommerce.cnj70.enums;

/**
 * TASK #16 / Phase 4 - KYC Status (consolidated).
 *
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
    PENDING_ADMIN,

    /** Admin đã duyệt → vendor được tạo shop */
    APPROVED,

    /** Admin từ chối, vendor có thể sửa và gửi lại */
    ADMIN_REJECTED,

    /** Bị đình chỉ vĩnh viễn (do vi phạm) */
    SUSPENDED;

    /**
     * Vendor có thể submit form mới
     */
    public boolean canSubmit() {
        return this == NOT_SUBMITTED
            || this == THIRD_PARTY_REJECTED
            || this == ADMIN_REJECTED; // ADMIN_REJECTED → vendor được sửa và gửi lại
    }

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
}
