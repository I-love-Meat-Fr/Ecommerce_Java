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
     * Lấy danh sách voucher khả dụng (active, còn hạn, còn lượt)
     */
    List<Voucher> getAvailableVouchers();
    
    /**
     * Lấy danh sách voucher khả dụng của một shop
     */
    List<Voucher> getAvailableVouchersByShop(String shopId);
    
    /**
     * Tăng số lượt đã sử dụng voucher
     */
    void incrementUsed(String voucherId);
    
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
