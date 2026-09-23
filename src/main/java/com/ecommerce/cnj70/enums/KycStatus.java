package com.ecommerce.cnj70.enums;

public enum KycStatus {
    /** Chưa nộp hồ sơ */
    NOT_SUBMITTED,

    /** Đã nộp, đang chờ 3rd-party xác minh */
    PENDING_THIRD_PARTY,

    /** 3rd-party từ chối, vendor có thể sửa và gửi lại */
    THIRD_PARTY_REJECTED,

    /** 3rd-party OK, đang chờ admin duyệt */
    PENDING_ADMIN,

    /** Admin đã duyệt → vendor được tạo shop */
    APPROVED,

    /** Admin từ chối, vendor không thể tạo shop */
    ADMIN_REJECTED,

    /** Bị đình chỉ vĩnh viễn */
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
