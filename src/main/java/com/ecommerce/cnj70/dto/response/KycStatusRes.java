package com.ecommerce.cnj70.dto.response;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatusRes {

    private String profileId;
    private String userId;
    private String status;
    private String statusLabel;
    private String rejectionReason;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;

    // Thông tin đã submit (null nếu chưa nộp)
    private String ownerFullName;
    private String idNumber;
    private String businessName;
    private String taxCode;
    private String bankAccount;
    private String bankName;
    private Integer submitCount;

    // Cho vendor: có thể tạo shop chưa?
    private boolean canCreateShop;

    public static KycStatusRes fromProfile(KycProfile profile) {
        if (profile == null) {
            return KycStatusRes.builder()
                    .status("NOT_SUBMITTED")
                    .statusLabel("Chưa nộp hồ sơ")
                    .canCreateShop(false)
                    .build();
        }

        KycStatus status = profile.getStatus() != null ? profile.getStatus() : KycStatus.NOT_SUBMITTED;

        return KycStatusRes.builder()
                .profileId(profile.getId())
                .userId(profile.getUserId())
                .status(status.name())
                .statusLabel(mapStatusLabel(status))
                .rejectionReason(buildRejectionReason(profile))
                .submittedAt(profile.getSubmittedAt())
                .reviewedAt(profile.getAdminReviewedAt())
                .ownerFullName(profile.getOwnerFullName())
                .idNumber(maskIdNumber(profile.getIdNumber()))
                .businessName(profile.getBusinessName())
                .taxCode(maskTaxCode(profile.getTaxCode()))
                .bankAccount(maskBankAccount(profile.getBankAccount()))
                .bankName(profile.getBankName())
                .submitCount(profile.getSubmitCount())
                .canCreateShop(status == KycStatus.APPROVED)
                .build();
    }

    private static String buildRejectionReason(KycProfile profile) {
        // Ưu tiên lý do từ admin, fallback lý do từ 3rd-party
        if (profile.getAdminNote() != null && !profile.getAdminNote().isBlank()) {
            return "[Admin] " + profile.getAdminNote();
        }
        if (profile.getThirdPartyRejectionReason() != null && !profile.getThirdPartyRejectionReason().isBlank()) {
            return "[3rd-party] " + profile.getThirdPartyRejectionReason();
        }
        return null;
    }

    private static String mapStatusLabel(KycStatus status) {
        return switch (status) {
            case NOT_SUBMITTED       -> "Chưa nộp hồ sơ";
            case PENDING_THIRD_PARTY -> "Đang chờ bên thứ ba xác minh";
            case THIRD_PARTY_REJECTED -> "Bên thứ ba từ chối - vui lòng sửa và gửi lại";
            case PENDING_ADMIN       -> "Đang chờ admin duyệt";
            case APPROVED            -> "Đã xác minh - có thể tạo cửa hàng";
            case ADMIN_REJECTED      -> "Admin từ chối - vui lòng liên hệ hỗ trợ";
            case SUSPENDED           -> "Bị đình chỉ";
            // Legacy mappings — chỉ để hiển thị cho dữ liệu cũ (không tạo mới)
            case PENDING_KYC         -> "Chưa nộp hồ sơ";
            case PENDING_PROVIDER    -> "Đang chờ bên thứ ba xác minh";
            case KYC_REJECTED        -> "Admin từ chối - vui lòng liên hệ hỗ trợ";
        };
    }

    private static String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 12) return idNumber;
        return idNumber.substring(0, 4) + "********" + idNumber.substring(10, 12);
    }

    private static String maskTaxCode(String taxCode) {
        if (taxCode == null || taxCode.length() < 4) return taxCode;
        return "****" + taxCode.substring(taxCode.length() - 4);
    }

    private static String maskBankAccount(String bankAccount) {
        if (bankAccount == null || bankAccount.length() < 6) return bankAccount;
        return bankAccount.substring(0, 3) + "****" + bankAccount.substring(bankAccount.length() - 3);
    }
}
