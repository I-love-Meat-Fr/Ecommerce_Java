package com.ecommerce.cnj70.enums;

/**
 * Phương thức thanh toán được hỗ trợ trên hệ thống.
 *
 * <ul>
 *   <li>{@link #COD} — Thanh toán khi nhận hàng (Cash On Delivery)</li>
 *   <li>{@link #VNPAY} — Cổng thanh toán VNPAY (Phase 3B+)</li>
 *   <li>{@link #BANK_QR} — Chuyển khoản ngân hàng qua mã QR VietQR.
 *       Khi KH chọn phương thức này, hệ thống hiển thị QR code tương ứng với
 *       số tiền cần thanh toán để KH quét bằng app ngân hàng. Đơn hàng được
 *       giữ ở trạng thái PENDING cho tới khi admin/vendor xác nhận đã nhận tiền
 *       (hoặc tích hợp webhook auto-reconcile).</li>
 * </ul>
 */
public enum PaymentMethod {
    COD,
    VNPAY,
    BANK_QR
}
