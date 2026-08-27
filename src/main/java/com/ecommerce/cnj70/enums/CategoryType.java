package com.ecommerce.cnj70.enums;

import lombok.Getter;

/**
 * Enum 10 danh mục đồ công nghệ.
 * Vendor chỉ được bán sản phẩm thuộc các danh mục này.
 * Dùng để gắn nhãn nhóm sản phẩm trong báo cáo/thống kê.
 */
@Getter
public enum CategoryType {

    LAPTOP("Laptop"),
    SMARTPHONE("Điện thoại"),
    TABLET("Máy tính bảng"),
    SMARTWATCH("Đồng hồ thông minh"),
    HEADPHONE("Tai nghe"),
    ACCESSORY("Phụ kiện"),
    CAMERA("Camera"),
    MONITOR("Màn hình"),
    PC_LINH_KIEN("PC / Linh kiện"),
    GAMING_GEAR("Gaming gear");

    private final String displayName;

    CategoryType(String displayName) {
        this.displayName = displayName;
    }
}