package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

public interface AdminKycService {

    /**
     * Danh sách hồ sơ KYC cho admin duyệt (mặc định lấy PENDING_ADMIN)
     */
    Page<KycProfile> listPending(Pageable pageable, String search);

    Page<KycProfile> listByStatus(KycStatus status, String search, Pageable pageable);

    /**
     * Chi tiết 1 hồ sơ (kèm thông tin User)
     */
    Map<String, Object> getDetail(String profileId);

    /**
     * Admin duyệt → APPROVED → vendor được tạo shop
     */
    KycProfile approve(String profileId, UserDetails admin, String note);

    /**
     * Admin từ chối → ADMIN_REJECTED
     */
    KycProfile reject(String profileId, UserDetails admin, String note);

    /**
     * Đình chỉ hồ sơ đã APPROVED → SUSPENDED
     */
    KycProfile suspend(String profileId, UserDetails admin, String note);

    /**
     * Thống kê cho dashboard admin
     */
    Map<String, Long> getStats();

    /**
     * Helper: lấy User từ KycProfile (dùng cho template)
     */
    User getUserForProfile(KycProfile profile);
}
