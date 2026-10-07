package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Document lưu trữ thông tin thanh toán của đơn hàng.
 * 
 * <p>Hỗ trợ nhiều phương thức thanh toán:
 * <ul>
 *   <li>COD - Thanh toán khi nhận hàng</li>
 *   <li>VNPAY - Thanh toán qua cổng VNPAY</li>
 *   <li>BANK_QR - Quét mã QR qua VietQR</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "payments")
@CompoundIndex(name = "order_payment_method_idx", def = "{'orderId': 1, 'paymentMethod': 1}", unique = true)
public class Payment {

    @Id
    private String id;

    /**
     * Order ID liên kết với thanh toán.
     */
    @Indexed
    private String orderId;

    /**
     * Số tiền thanh toán (VNĐ).
     */
    private BigDecimal amount;

    /**
     * Phương thức thanh toán.
     */
    private PaymentMethod paymentMethod;

    /**
     * Trạng thái thanh toán.
     */
    private PaymentStatus status;

    /**
     * VietQR transaction ID (nếu dùng BANK_QR).
     * Dùng để query trạng thái và verify thanh toán.
     */
    @Indexed
    private String vietqrTransactionId;

    /**
     * VNPAY transaction ID (nếu dùng VNPAY).
     */
    private String vnpTransactionId;

    /**
     * Số tài khoản người thanh toán (VietQR webhook).
     */
    private String paidBy;

    /**
     * Tên người thanh toán (VietQR webhook).
     */
    private String paidByName;

    /**
     * Mã ngân hàng người thanh toán (VietQR webhook).
     */
    private String bankCode;

    /**
     * Tên ngân hàng người thanh toán (VietQR webhook).
     */
    private String bankName;

    /**
     * VNPAY response code.
     */
    private String vnpResponseCode;

    /**
     * Thời gian tạo thanh toán.
     */
    private LocalDateTime createdAt;

    /**
     * Thời gian thanh toán thành công.
     */
    private LocalDateTime paidAt;

    /**
     * Thời gian hoàn tiền (nếu có).
     */
    private LocalDateTime refundedAt;

    /**
     * Ghi chú/message lỗi.
     */
    private String errorMessage;

    /**
     * Dữ liệu raw từ webhook/provider (JSON string).
     * Lưu để debug và audit.
     */
    private String rawWebhookData;

    /**
     * Kiểm tra đã thanh toán chưa.
     */
    public boolean isPaid() {
        return status == PaymentStatus.PAID;
    }

    /**
     * Kiểm tra có thể hoàn tiền không.
     */
    public boolean canRefund() {
        return status == PaymentStatus.PAID;
    }
}
