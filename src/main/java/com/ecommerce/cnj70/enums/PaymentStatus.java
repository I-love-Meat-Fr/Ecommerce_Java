package com.ecommerce.cnj70.enums;

/**
 * Trạng thái thanh toán của Order.
 *
 * <p>Khác với {@link OrderStatus} (workflow xử lý đơn hàng tổng) và
 * {@link ShippingStatus} (vận chuyển theo shop), enum này chỉ phản ánh
 * trạng thái của giao dịch thanh toán — tách biệt hoàn toàn để UI có thể
 * hiển thị song song (vd: Order DELIVERED + payment PAID cho COD, hoặc
 * Order DELIVERED + payment REFUNDED sau khi hoàn tiền).</p>
 *
 * <p><b>Quy tắc chuyển trạng thái:</b></p>
 * <ul>
 *   <li>{@code PENDING} → {@code PAID}: khi nhận callback VNPAY thành công / COD nhận tiền</li>
 *   <li>{@code PENDING} → {@code FAILED}: khi VNPAY callback thất bại / COD hủy</li>
 *   <li>{@code PAID} → {@code REFUNDED}: khi Refund flow hoàn tất (xem {@code RefundStatus.SUCCEEDED})</li>
 *   <li>{@code FAILED/REFUNDED} → terminal — không chuyển tiếp (chỉ tạo Order mới nếu cần)</li>
 * </ul>
 */
public enum PaymentStatus {

    /** Đơn hàng vừa tạo, chưa nhận được tiền (COD mặc định, VNPAY đang chờ callback). */
    PENDING,

    /** Đã nhận tiền (COD đã thu khi giao / VNPAY callback thành công). */
    PAID,

    /** Thanh toán thất bại (VNPAY callback lỗi / timeout / hủy). */
    FAILED,

    /** Đã hoàn tiền cho Customer sau khi Refund flow hoàn tất. */
    REFUNDED
}