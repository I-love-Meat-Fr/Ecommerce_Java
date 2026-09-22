package com.ecommerce.cnj70.kyc;

/**
 * TASK #17 — ThirdPartyKycProvider contract.
 *
 * Interface cho phép plug-in nhiều provider KYC:
 * - MockKycProvider: dev/test, luôn pass/reject theo rule đơn giản
 * - VnptKycProvider: mock VNPT eKYC API
 * - FptKycProvider: mock FPT AI eKYC API
 *
 * Mỗi provider:
 * - submit(): gửi dữ liệu đi, trả về referenceId để track
 * - getResult(): lấy kết quả (được gọi khi có callback từ provider)
 */
public interface KycProvider {

    /**
     * Submit KYC data lên provider.
     *
     * @param citizenId    CCCD mã hóa
     * @param taxCode      Tax code mã hóa (null nếu không có)
     * @param bankAccount  Số tài khoản mã hóa
     * @param shopId       Shop đang submit (để provider trace)
     * @return referenceId từ provider
     */
    String submit(String citizenId, String taxCode, String bankAccount, String shopId);

    /**
     * Lấy kết quả KYC từ provider.
     *
     * @param referenceId reference ID đã lưu khi submit
     * @return KycResult chứa status + rejection reason (nếu có)
     */
    KycResult getResult(String referenceId);

    /**
     * Tên provider (MOCK / VNPT / FPT).
     */
    String getProviderName();
}
