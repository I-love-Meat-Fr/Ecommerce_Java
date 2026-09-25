package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
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
@Document(collection = "shops")
public class Shop {

    @Id
    private String id;

    private String ownerId;

    @Indexed(unique = true)
    private String shopName;

    private String description;

    private String logoUrl;

    private String bannerUrl;

    @Builder.Default
    private ShopStatus status = ShopStatus.PENDING;

    @Builder.Default
    private boolean active = true;

    // ===== TASK #16 KYC: KYC workflow status =====
    // Shop status (APPROVED/REJECTED/SUSPENDED) quản lý hoạt động bán hàng
    // KycStatus quản lý trạng thái xác minh danh tính Vendor.
    // Lưu ý: source-of-truth mới là KycProfile; Shop.kycStatus chỉ là
    // denormalized mirror để backward-compatible với KycService/Moderator
    // và các query cũ. Được sync bởi VendorKycService và AdminKycService.
    @Builder.Default
    private KycStatus kycStatus = KycStatus.NOT_SUBMITTED;

    /**
     * Reference ID từ KYC provider (VNPT/FPT/MOCK).
     * Dùng để trace khi callback/webhook nhận kết quả.
     */
    private String kycReferenceId;

    /** Thời điểm KYC được duyệt thành công (APPROVED). */
    private LocalDateTime kycApprovedAt;

    /** Lý do từ chối KYC (nếu KYC_REJECTED). */
    private String kycRejectionReason;

    /** Thời điểm nộp KYC gần nhất. */
    private LocalDateTime kycSubmittedAt;

    /** Version của KYC document đã submit (để detect duplicate submission). */
    private Integer kycDocumentVersion;

    // ===== TASK #19 PII Security: AES-256-GCM encrypted PII fields (backup cho User) =====
    // Primary PII storage: User.encryptedCitizenId
    // Shop giữ bản sao để Admin/Moderator xem nhanh (đã mã hóa, không plaintext)
    private String encryptedCitizenId;
    private String encryptedTaxCode;
    private String encryptedBankAccount;

    // ===== Admin Shop lifecycle audit (brought in by feature/vendors-module) =====
    /** Lý do admin từ chối shop (khi status = REJECTED) */
    private String rejectionReason;

    /** Lý do admin ngừng hoạt động shop (khi active = false) */
    private String deactivationReason;

    /** Admin thực hiện từ chối / ngừng hoạt động */
    private String actionBy;

    /** Thời điểm admin thực hiện thao tác */
    private LocalDateTime actionAt;

    /**
     * Phase 3A — Admin who most recently enforced on this shop
     * (Suspend / Restrict / Reinstate).
     */
    private String enforcementActorId;

    /**
     * Phase 3A — Reason supplied with the most recent enforcement action.
     */
    private String enforcementReason;

    /**
     * Phase 3A — Timestamp of the most recent enforcement action.
     */
    private LocalDateTime enforcementAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public boolean isVerified() {
        return status == ShopStatus.APPROVED;
    }

    public boolean isKycApproved() {
        return kycStatus == KycStatus.APPROVED;
    }

    public ShopStatus getStatus() {
        return status != null ? status : ShopStatus.PENDING;
    }

    public void setVerified(boolean verified) {
        this.status = verified ? ShopStatus.APPROVED : ShopStatus.PENDING;
    }
}
