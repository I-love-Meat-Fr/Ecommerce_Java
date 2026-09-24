package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;

import java.util.List;

/**
 * Service interface cho Voucher
 */
public interface VoucherService {

    /**
     * Tạo voucher mới
     */
    Voucher createVoucher(VoucherFormReq request, String shopId, String shopName, String createdBy);

    /**
     * Tạo voucher WEB (admin tạo)
     */
    Voucher createWebVoucher(VoucherFormReq request, String createdBy);

    /**
     * Cập nhật voucher
     */
    Voucher updateVoucher(String voucherId, VoucherFormReq request);

    /**
     * Xóa voucher (soft delete - set active = false)
     */
    void deleteVoucher(String voucherId);

    /**
     * Phase 11 — Activate WEB Voucher (set active = true).
     * Chỉ áp dụng cho voucher type = WEB.
     */
    Voucher activateWebVoucher(String voucherId);

    /**
     * Phase 11 — Deactivate WEB Voucher (set active = false).
     * Chỉ áp dụng cho voucher type = WEB.
     */
    Voucher deactivateWebVoucher(String voucherId);

    /**
     * Phase 11 — Cập nhật WEB Voucher (chỉ áp dụng cho type = WEB).
     */
    Voucher updateWebVoucher(String voucherId, VoucherFormReq request);

    /**
     * Phase 11 — Soft delete WEB Voucher (set active = false).
     * Chỉ áp dụng cho voucher type = WEB.
     */
    void deleteWebVoucher(String voucherId);

    /**
     * Tìm voucher theo ID
     */
    Voucher getVoucherById(String id);

    /**
     * Tìm voucher theo code
     */
    Voucher getVoucherByCode(String code);

    /**
     * Lấy danh sách voucher của một shop
     */
    List<Voucher> getVouchersByShop(String shopId);

    /**
     * Lấy tất cả voucher WEB (của admin)
     */
    List<Voucher> getWebVouchers();

    /**
     * Phase 12: Admin Voucher Search+Filter+Pagination.
     * @param active null = tất cả (ALL), true = chỉ active, false = inactive
     */
    org.springframework.data.domain.Page<Voucher> getWebVouchers(
            org.springframework.data.domain.Pageable pageable, String q, Boolean active);

    /**
     * Lấy danh sách voucher khả dụng (active, còn hạn, còn lượt)
     */
    List<Voucher> getAvailableVouchers();

    /**
     * Phase 12: Lấy WEB Voucher khả dụng cho trang công khai Customer.
     * Chỉ trả type=WEB, active=true, còn hạn, còn lượt.
     * SHOP Voucher KHÔNG hiển thị trên website.
     */
    List<Voucher> getAvailableWebVouchersForCustomer();

    /**
     * Lấy danh sách voucher khả dụng của một shop
     */
    List<Voucher> getAvailableVouchersByShop(String shopId);

    /**
     * Đếm số voucher khả dụng (active, còn hạn, còn lượt)
     */
    long countAvailableVouchers();

    /**
     * Tăng số lượt đã sử dụng voucher (Phase 11 — non-atomic, deprecated).
     * Use {@link #tryIncrementUsed(String)} (Phase 4B atomic) instead.
     */
    @Deprecated
    void incrementUsed(String voucherId);

    /**
     * Phase 4B — Atomic usedCount increment with race-safe quantity guard.
     *
     * <p>Performs an atomic conditional update:
     * {@code UPDATE vouchers WHERE _id = ? AND active = true AND used < quantity}
     * then {@code $inc: { used: 1 }}.</p>
     *
     * <p>Returns {@code true} if the increment succeeded, {@code false} if
     * the voucher was exhausted, deactivated, or doesn't exist.</p>
     *
     * <p>This is the correct primitive for Order/Checkout integration —
     * voucher is only consumed if and when the order is successfully created.</p>
     */
    boolean tryIncrementUsed(String voucherId);

    /**
     * Phase 4B — Atomic decrement usedCount (cancel/reversal seam).
     *
     * <p>Decrements {@code used} if {@code used > 0}. Used when an order
     * is cancelled/refunded and the voucher should be returned to the pool.</p>
     *
     * <p>Returns {@code true} if the decrement succeeded.</p>
     */
    boolean tryDecrementUsed(String voucherId);

    /**
     * Kiểm tra voucher có khả dụng không
     */
    boolean isVoucherValid(String code);

    /**
     * Validate voucher cho checkout
     * Trả về voucher nếu hợp lệ, throw exception nếu không
     */
    Voucher validateForCheckout(String code, String shopId, String productId);
}
