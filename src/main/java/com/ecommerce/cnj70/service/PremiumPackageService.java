package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.PremiumPackage;
import com.ecommerce.cnj70.document.VendorSubscription;
import com.ecommerce.cnj70.dto.request.PremiumPackageFormReq;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;
import java.util.Optional;

/**
 * Service quản lý Premium Package và Vendor Subscription.
 *
 * <h3>Phân quyền</h3>
 * <ul>
 *   <li>Admin: CRUD package, set active/inactive</li>
 *   <li>Vendor: xem package, mua, hủy, xem sub hiện tại</li>
 * </ul>
 */
public interface PremiumPackageService {

    // ========== Admin operations ==========

    PremiumPackage createPackage(PremiumPackageFormReq request);

    PremiumPackage updatePackage(String packageId, PremiumPackageFormReq request);

    /** Soft delete: set {@code active=false}. */
    void deletePackage(String packageId);

    List<PremiumPackage> getAllPackages();

    List<PremiumPackage> getAllPackages(Boolean activeFilter);

    PremiumPackage getPackageById(String packageId);

    // ========== Vendor operations ==========

    /** Lấy các gói còn bán (active=true) cho vendor. */
    List<PremiumPackage> getActivePackages();

    /**
     * Vendor mua Premium. Nếu shop đã có sub ACTIVE → cộng dồn endDate.
     * Tự động set {@code shop.premiumActive=true} và {@code shop.premiumExpiresAt=newEndDate}.
     */
    VendorSubscription purchasePackage(String packageId, UserDetails userDetails);

    /** Vendor chủ động hủy sub ACTIVE. Clear shop flags. */
    VendorSubscription cancelSubscription(UserDetails userDetails);

    /** Sub ACTIVE hiện tại của shop vendor (null nếu chưa từng mua). */
    Optional<VendorSubscription> getCurrentSubscription(UserDetails userDetails);

    /** Lịch sử mua/gia hạn của shop. */
    List<VendorSubscription> getSubscriptionHistory(UserDetails userDetails);

    // ========== Internal — Scheduler ==========

    /** Sub ACTIVE đã quá hạn cần expire. */
    List<VendorSubscription> findExpiredSubscriptions(java.time.LocalDateTime cutoff);

    /**
     * Đánh dấu 1 sub là EXPIRED, và clear shop flag (chỉ khi shop đang
     * reference đúng sub này để tránh over-write khi vendor vừa gia hạn).
     */
    void markExpired(String subscriptionId);
}
