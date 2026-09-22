package com.ecommerce.cnj70.enums;

/**
 * TASK #16 — KYC Status cho Shop verification.
 *
 * Luồng: PENDING_KYC → PENDING_PROVIDER (đang xác minh) → APPROVED / KYC_REJECTED / SUSPENDED
 *
 * - PENDING_KYC:      Vendor chưa nộp KYC
 * - PENDING_PROVIDER: KYC đã gửi sang provider, đang chờ kết quả
 * - APPROVED:         KYC được duyệt, shop verified
 * - KYC_REJECTED:     KYC bị từ chối, vendor có thể nộp lại
 * - SUSPENDED:        Shop bị tạm ngưng (do vi phạm hoặc KYC hết hạn)
 */
public enum KycStatus {
    PENDING_KYC,
    PENDING_PROVIDER,
    APPROVED,
    KYC_REJECTED,
    SUSPENDED
}
