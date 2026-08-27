package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Voucher;

import java.util.List;

public interface VoucherService {

    // Lưu voucher mới
    Voucher save(Voucher voucher);

    // Tìm voucher theo code
    Voucher findByCode(String code);

    // Tìm voucher theo id
    Voucher findById(String id);

    // Lấy danh sách voucher công khai (active + còn hạn + còn lượt)
    List<Voucher> findAvailable();

    // Lấy voucher WEB đang khả dụng (toàn sàn)
    List<Voucher> findAvailableWeb();

    // Lấy voucher của 1 shop đang khả dụng
    List<Voucher> findAvailableByShopId(String shopId);

    // Lấy tất cả voucher của 1 shop
    List<Voucher> findByShopId(String shopId);

    // Tăng số lượt đã dùng
    void incrementUsed(String voucherId);

    // Tăng số lượt đã dùng (atomic)
    boolean incrementUsedIfAvailable(String voucherId);
}
