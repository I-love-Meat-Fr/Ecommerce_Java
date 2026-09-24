package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Hồ sơ KYC - tách thành collection riêng (1 user ↔ 1 kycProfile)
 * Flow:
 *   1. Vendor submit → status = PENDING_THIRD_PARTY
 *   2. 3rd-party verify → PENDING_ADMIN hoặc THIRD_PARTY_REJECTED
 *   3. Admin duyệt → APPROVED (được tạo shop) hoặc ADMIN_REJECTED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "kyc_profiles")
public class KycProfile {

    @Id
    private String id;

    /** userId của vendor, 1 user chỉ có 1 profile (unique index) */
    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private KycStatus status = KycStatus.NOT_SUBMITTED;

    // ============ Thông tin chủ shop ============
    private String ownerFullName;
    private String idNumber;
    private String idFrontImageUrl;
    private String idBackImageUrl;

    // ============ Thông tin doanh nghiệp ============
    private String businessName;
    private String taxCode;
    private String businessLicenseUrl;

    // ============ Thông tin ngân hàng ============
    private String bankAccount;
    private String bankName;
    private String bankBranch;

    // ============ Kết quả từ 3rd-party ============
    private String thirdPartyResult;          // APPROVED / REJECTED
    private String thirdPartyRejectionReason;
    private String thirdPartyReferenceId;
    private LocalDateTime thirdPartyVerifiedAt;

    // ============ Kết quả từ Admin ============
    private String adminReviewerId;
    private String adminReviewerName;
    private String adminNote;                 // Ghi chú của admin (lý do reject)
    private LocalDateTime adminReviewedAt;

    // ============ Audit ============
    private LocalDateTime submittedAt;        // Lần submit đầu tiên
    private LocalDateTime lastResubmittedAt;  // Lần submit gần nhất
    private Integer submitCount;              // Số lần đã submit

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public boolean isAdminApproved() {
        return status == KycStatus.APPROVED;
    }

    public boolean canVendorSubmit() {
        return status == null
                || status == KycStatus.NOT_SUBMITTED
                || status == KycStatus.THIRD_PARTY_REJECTED;
    }
}
