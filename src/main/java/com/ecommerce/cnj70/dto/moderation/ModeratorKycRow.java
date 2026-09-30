package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.KycStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Row hiển thị cho moderator KYC queue.
 * <p>Được build từ {@link KycProfile} + {@link User} (vendor) — không cần Shop
 * vì vendor mới đăng ký chưa có Shop document, nhưng vẫn phải xuất hiện
 * trong queue để moderator duyệt.</p>
 */
@Data
@Builder
public class ModeratorKycRow {

    /** Profile ID — dùng làm ID trong URL detail/approve/reject. */
    private String profileId;

    /** User ID của vendor (lookup từ KycProfile.userId). */
    private String userId;

    /** Tên shop / doanh nghiệp (từ KycProfile.businessName, fallback ownerFullName / user.fullName). */
    private String shopName;

    /** Tên chủ shop (từ KycProfile.ownerFullName, fallback User.fullName). */
    private String ownerName;

    /** Email liên hệ (User.email). */
    private String ownerEmail;

    /** SĐT liên hệ (User.phone). */
    private String ownerPhone;

    /** Trạng thái KYC hiện tại. */
    private KycStatus status;

    /** Ngày nộp gần nhất (ưu tiên lastResubmittedAt, fallback submittedAt). */
    private LocalDateTime submittedAt;

    /** Lý do từ chối (3rd-party hoặc admin). */
    private String rejectionReason;

    /** Số lần submit (để moderator biết vendor đã resubmit). */
    private Integer submitCount;

    /** Có Shop document chưa (true = Shop đã tạo, false = vendor chỉ mới submit KYC). */
    private boolean shopCreated;
}
