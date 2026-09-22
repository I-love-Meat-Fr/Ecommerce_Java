package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.request.KycSubmitRequest;
import com.ecommerce.cnj70.dto.response.KycStatusResponse;

/**
 * TASK #16 — KycService Orchestrator.
 *
 * Quản lý vòng đời KYC của Vendor (Shop):
 * 1. Vendor nộp KYC → mã hóa PII → gửi sang provider
 * 2. Provider xử lý → callback → cập nhật Shop.kycStatus
 * 3. Admin/Moderator xem KYC status + quyết định
 *
 * Provider contract:
 * - MockKycProvider: dev/test (luôn pass/reject theo rule cứng)
 * - VnptKycProvider: mock VNPT KYC (có thể thay bằng provider thật)
 * - FptKycProvider: mock FPT KYC
 */
public interface KycService {

    /**
     * Vendor nộp KYC: mã hóa PII data và gửi sang provider.
     *
     * @param shopId  Shop đang nộp KYC
     * @param request CCCD, taxCode, bankAccount (plaintext)
     * @return KycStatusResponse chứa referenceId + trạng thái hiện tại
     */
    KycStatusResponse submitKyc(String shopId, KycSubmitRequest request);

    /**
     * Lấy KYC status hiện tại của Shop (không giải mã PII).
     */
    KycStatusResponse getKycStatus(String shopId);

    /**
     * Vendor xem KYC đã nộp (chỉ trạng thái, không có PII plaintext).
     */
    KycStatusResponse getVendorKycStatus(String vendorUserId);

    /**
     * Kiểm tra shop có KYC APPROVED hay không.
     * Dùng khi cần xác minh nhanh (ví dụ: trước khi publish product).
     */
    boolean isKycApproved(String shopId);

    /**
     * TASK #18 — Xử lý callback từ KYC Provider.
     * Cập nhật Shop.kycStatus + AuditLog khi có kết quả từ provider.
     *
     * @param referenceId     Reference ID từ provider
     * @param approved        true = APPROVED, false = REJECTED
     * @param rejectionReason Lý do từ chối (nếu REJECTED)
     */
    void processKycCallback(String referenceId, boolean approved, String rejectionReason);
}
