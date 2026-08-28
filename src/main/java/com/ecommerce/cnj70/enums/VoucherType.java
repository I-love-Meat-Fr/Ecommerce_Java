package com.ecommerce.cnj70.enums;

/**
 * Loại voucher: SHOP (vendor tạo cho shop của mình), WEB (admin tạo cho toàn hệ thống)
 */
public enum VoucherType {
    SHOP,  // Voucher của vendor, chỉ áp dụng cho sản phẩm trong shop đó
    WEB    // Voucher của admin, có thể áp dụng cho toàn bộ hoặc sản phẩm cụ thể
}
