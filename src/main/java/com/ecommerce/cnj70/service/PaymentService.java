package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Payment;
import com.ecommerce.cnj70.dto.vietqr.VietQRPaymentResponse;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service interface cho Payment operations.
 * 
 * <p>Hỗ trợ nhiều phương thức thanh toán:
 * <ul>
 *   <li>COD - Thanh toán khi nhận hàng</li>
 *   <li>VNPAY - Thanh toán qua cổng VNPAY</li>
 *   <li>BANK_QR - Quét mã QR qua VietQR</li>
 * </ul>
 */
public interface PaymentService {

    /**
     * Tạo payment record cho đơn hàng.
     *
     * @param orderId       Order ID
     * @param amount        Số tiền
     * @param paymentMethod Phương thức thanh toán
     * @return Payment record
     */
    Payment createPayment(String orderId, BigDecimal amount, PaymentMethod paymentMethod);

    /**
     * Tạo QR code thanh toán VietQR cho đơn hàng.
     *
     * @param orderId Order ID
     * @param amount  Số tiền
     * @return VietQRPaymentResponse chứa QR code và thông tin
     */
    VietQRPaymentResponse createVietQRPayment(String orderId, BigDecimal amount);

    /**
     * Xác nhận thanh toán cho đơn hàng (sau webhook).
     *
     * @param orderId Order ID
     * @return true nếu thành công
     */
    boolean confirmPayment(String orderId);

    /**
     * Xác nhận thanh toán thất bại.
     *
     * @param orderId Order ID
     * @param message Thông báo lỗi
     */
    void failPayment(String orderId, String message);

    /**
     * Cập nhật trạng thái payment.
     *
     * @param orderId   Order ID
     * @param newStatus Trạng thái mới
     */
    void updatePaymentStatus(String orderId, PaymentStatus newStatus);

    /**
     * Lấy payment record theo order ID.
     *
     * @param orderId Order ID
     * @return Optional<Payment>
     */
    Optional<Payment> getPaymentByOrderId(String orderId);

    /**
     * Lấy payment record theo order ID và phương thức.
     *
     * @param orderId       Order ID
     * @param paymentMethod Phương thức thanh toán
     * @return Optional<Payment>
     */
    Optional<Payment> getPaymentByOrderIdAndMethod(String orderId, PaymentMethod paymentMethod);

    /**
     * Kiểm tra đơn hàng đã thanh toán chưa.
     *
     * @param orderId Order ID
     * @return true nếu đã thanh toán
     */
    boolean isPaid(String orderId);

    /**
     * Tạo payment record cho COD.
     *
     * @param orderId Order ID
     * @param amount  Số tiền
     * @return Payment record
     */
    default Payment createCODPayment(String orderId, BigDecimal amount) {
        return createPayment(orderId, amount, PaymentMethod.COD);
    }

    /**
     * Xác nhận thanh toán COD khi giao hàng thành công.
     *
     * @param orderId Order ID
     */
    default void confirmCODPayment(String orderId) {
        updatePaymentStatus(orderId, PaymentStatus.PAID);
    }
}
