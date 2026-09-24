package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.UserRole;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;

    private String fullName;

    private String phone;

    private String address;

    private UserRole role;

    @Builder.Default
    private AccountStatus status = AccountStatus.UNVERIFIED;

    private String shopId;

    private String avatarUrl;

    // ===== TASK #19 PII Security: AES-256-GCM encrypted fields =====
    // Lưu dạng Base64(iv || ciphertext || authTag). KHÔNG lưu plaintext.
    // Giải mã chỉ trong KycService/Admin service khi cần hiển thị.
    private String encryptedCitizenId;    // CCCD/CMND
    private String encryptedTaxCode;       // Mã số thuế (nếu có)
    private String encryptedBankAccount;   // Số tài khoản ngân hàng

    // ===== TASK #22: Terms + Privacy acceptance tracking =====

    /** Thời điểm User accept Điều khoản sử dụng. Null = chưa accept. */
    private LocalDateTime acceptedTermsAt;

    /** Version của Terms đã accept (tăng mỗi khi Admin update Terms). */
    private Integer acceptedTermsVersion;

    /** Thời điểm User accept Chính sách bảo mật. */
    private LocalDateTime acceptedPrivacyAt;

    /** Version của Privacy đã accept. */
    private Integer acceptedPrivacyVersion;

    /**
     * User đã opt-in nhận email marketing hay chưa.
     * - null = chưa quyết định (legacy users)
     * - true = đã opt-in
     * - false = đã opt-out (mặc định)
     */
    private Boolean marketingOptIn;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Vendor đã hoàn tất xác minh KYC chưa? */
    public boolean isKycApproved() {
        return kycStatus == KycStatus.APPROVED;
    }
}

