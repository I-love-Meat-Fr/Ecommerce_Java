package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * TASK #15 — Moderator Service.
 *
 * Service cho Moderator kiểm duyệt:
 * - Review queue (REPORTED, HIDDEN reviews)
 * - KYC queue (pending/rejected shops)
 */
public interface ModeratorService {

    /**
     * Lấy danh sách Review theo trạng thái moderation (phân trang).
     */
    Page<Review> getReviewsByModerationStatusPaged(ReviewModerationStatus status, Pageable pageable);

    /**
     * Lấy các Review REPORTED gần đây (cho dashboard).
     */
    Page<Review> getRecentReportedReviews(Pageable pageable);

    /**
     * Lấy danh sách Shop theo KYC status.
     */
    List<Shop> getShopsByKycStatus(KycStatus status);

    /**
     * Lấy danh sách Shop theo KYC status (phân trang).
     */
    Page<Shop> getShopsByKycStatusPaged(KycStatus status, Pageable pageable);

    /**
     * Lấy danh sách KycProfile theo status (phân trang).
     * <p>Source of truth cho KYC queue của Moderator: bao gồm vendor đã submit KYC
     * nhưng chưa tạo Shop. Trước đây queue query {@code ShopRepository.findByKycStatus()}
     * nên bỏ sót vendor mới (chưa có Shop document).</p>
     */
    Page<KycProfile> getKycProfilesByStatusPaged(KycStatus status, Pageable pageable);

    /**
     * Lấy danh sách KycProfile theo nhóm status (không phân trang) — dùng cho dashboard counts.
     */
    List<KycProfile> getKycProfilesByStatus(List<KycStatus> statuses);

    /**
     * Moderator duyệt KYC Shop (chuyển từ PENDING_THIRD_PARTY / PENDING_ADMIN / THIRD_PARTY_REJECTED → APPROVED).
     */
    Shop approveKyc(String shopId, String moderatorEmail, String note);

    /**
     * Moderator từ chối KYC Shop (chuyển sang ADMIN_REJECTED).
     */
    Shop rejectKyc(String shopId, String moderatorEmail, String note);

    /**
     * Moderator duyệt KYC dựa trên KycProfile id (vendor chưa có Shop).
     * Cập nhật KycProfile.status + User.kycStatus + Shop.kycStatus (nếu Shop tồn tại).
     */
    KycProfile approveKycProfile(String profileId, String moderatorEmail, String note);

    /**
     * Moderator từ chối KYC dựa trên KycProfile id.
     */
    KycProfile rejectKycProfile(String profileId, String moderatorEmail, String note);
}
