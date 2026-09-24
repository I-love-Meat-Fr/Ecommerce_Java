package com.ecommerce.cnj70.enums;

public enum ShippingStatus {
    /** Chờ shop xử lý / đóng gói */
    PENDING,
    /** Đã đóng gói, chờ đơn vị vận chuyển đến lấy */
    PACKED,
    /** Đơn vị vận chuyển đã nhận hàng */
    PICKED_UP,
    /** Đang vận chuyển */
    IN_TRANSIT,
    /** Đã giao đến khách hàng */
    DELIVERED,
    /** Giao hàng thất bại */
    FAILED,
    /** Hoàn hàng */
    RETURNED
}
