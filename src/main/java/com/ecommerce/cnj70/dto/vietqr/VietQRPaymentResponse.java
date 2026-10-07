package com.ecommerce.cnj70.dto.vietqr;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Response DTO trả về cho checkout page khi chọn BANK_QR.
 * 
 * <p>Chứa thông tin QR code và link thanh toán để hiển thị cho khách hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietQRPaymentResponse {

    /**
     * Order ID liên kết với QR code.
     */
    private String orderId;

    /**
     * Số tiền cần thanh toán (VNĐ).
     */
    private Long amount;

    /**
     * Số tiền dạng BigDecimal để hiển thị.
     */
    private BigDecimal amountDisplay;

    /**
     * QR code dạng Base64 (data:image/png;base64,...).
     * Dùng trực tiếp trong thẻ <img>.
     */
    private String qrDataURL;

    /**
     * Raw QR code string (nếu cần generate phía client).
     */
    private String qrCode;

    /**
     * Deep link để mở app ngân hàng.
     */
    private String deepLink;

    /**
     * VietQR transaction ID (để query trạng thái).
     */
    private String vietqrTransactionId;

    /**
     * Nội dung chuyển khoản.
     * Format: "THANH TOAN DH [orderId]"
     */
    private String transferDescription;

    /**
     * Thông tin tài khoản merchant để hiển thị.
     */
    private String merchantAccount;

    /**
     * Tên ngân hàng merchant.
     */
    private String merchantBank;

    /**
     * Tên chủ tài khoản merchant.
     */
    private String merchantAccountName;

    /**
     * Link ảnh QR để tải về/in.
     */
    private String qrDownloadUrl;
}
