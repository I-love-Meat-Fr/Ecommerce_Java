package com.ecommerce.cnj70.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận webhook callback từ VietQR khi có thanh toán.
 * 
 * <p>VietQR sẽ POST đến callback URL đã đăng ký khi:
 * <ul>
 *   <li>Khách hàng quét QR và thanh toán thành công</li>
 *   <li>Có sự thay đổi trạng thái giao dịch</li>
 * </ul>
 * 
 * <p><b>Lưu ý:</b> Cấu trúc webhook có thể thay đổi theo VietQR API version.
 * Tham khảo: https://vietqr.net/developer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietQRWebhookPayload {

    /**
     * Mã phản hồi từ webhook.
     * - "00": Thành công
     */
    @JsonProperty("code")
    private String code;

    /**
     * Thông điệp phản hồi.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Dữ liệu giao dịch.
     */
    @JsonProperty("data")
    private TransactionData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionData {
        
        /**
         * Mã giao dịch (UUID) từ VietQR.
         * Dùng để verify và query.
         */
        @JsonProperty("id")
        private String id;

        /**
         * Số tài khoản người gửi (khách hàng).
         */
        @JsonProperty("accountNumber")
        private String accountNumber;

        /**
         * Tên người gửi.
         */
        @JsonProperty("accountName")
        private String accountName;

        /**
         * Số tài khoản merchant (người nhận).
         */
        @JsonProperty("toAccount")
        private String toAccount;

        /**
         * Số tiền giao dịch (VNĐ).
         */
        @JsonProperty("amount")
        private Long amount;

        /**
         * Nội dung chuyển khoản.
         * Format: "THANH TOAN DH [orderId]"
         */
        @JsonProperty("addInfo")
        private String addInfo;

        /**
         * Mã ngân hàng của người gửi.
         */
        @JsonProperty("bankCode")
        private String bankCode;

        /**
         * Tên ngân hàng của người gửi.
         */
        @JsonProperty("bankName")
        private String bankName;

        /**
         * Ngày giờ giao dịch (ISO 8601 format).
         */
        @JsonProperty("transactionDateTime")
        private String transactionDateTime;

        /**
         * Credit/Debit indicator.
         * - "C": Credit (tiền vào tài khoản)
         * - "D": Debit (tiền ra khỏi tài khoản)
         */
        @JsonProperty("crDr")
        private String crDr;

        /**
         * Mã trace để đối soát.
         */
        @JsonProperty("traceId")
        private String traceId;

        /**
         * Tổng số tiền còn lại trong tài khoản (nếu có).
         */
        @JsonProperty("remainAmount")
        private Long remainAmount;
    }

    /**
     * Kiểm tra webhook có thành công không.
     */
    public boolean isSuccess() {
        return "00".equals(code);
    }

    /**
     * Extract order ID từ nội dung chuyển khoản.
     * Format: "THANH TOAN DH [orderId]"
     */
    public String extractOrderId() {
        if (data != null && data.getAddInfo() != null) {
            String info = data.getAddInfo();
            // Parse "THANH TOAN DH [orderId]" -> extract orderId
            if (info.startsWith("THANH TOAN DH ")) {
                return info.substring("THANH TOAN DH ".length()).trim();
            }
        }
        return null;
    }
}
