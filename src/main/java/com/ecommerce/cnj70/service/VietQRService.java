package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.vietqr.VietQRPaymentResponse;
import com.ecommerce.cnj70.dto.vietqr.VietQRWebhookPayload;

import java.math.BigDecimal;

/**
 * Service interface cho VietQR payment integration.
 */
public interface VietQRService {

    /**
     * Tạo QR code thanh toán cho đơn hàng.
     *
     * @param orderId   Order ID
     * @param amount    Số tiền cần thanh toán (VNĐ)
     * @return VietQRPaymentResponse chứa QR code và thông tin thanh toán
     */
    VietQRPaymentResponse createPaymentQR(String orderId, BigDecimal amount);

    /**
     * Xử lý webhook callback từ VietQR khi có thanh toán.
     *
     * @param payload Dữ liệu webhook từ VietQR
     * @return true nếu xử lý thành công, false nếu có lỗi
     */
    boolean handleWebhook(VietQRWebhookPayload payload);

    /**
     * Query trạng thái thanh toán từ VietQR.
     *
     * @param vietqrTransactionId VietQR transaction ID
     * @return true nếu đã thanh toán, false nếu chưa
     */
    boolean queryPaymentStatus(String vietqrTransactionId);

    /**
     * Kiểm tra xem một order đã được thanh toán qua VietQR chưa.
     *
     * @param orderId Order ID
     * @return true nếu đã thanh toán
     */
    boolean isOrderPaid(String orderId);
}
