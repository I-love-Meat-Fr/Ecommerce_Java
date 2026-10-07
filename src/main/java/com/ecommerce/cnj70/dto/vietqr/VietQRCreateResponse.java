package com.ecommerce.cnj70.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO từ VietQR API khi tạo QR Code.
 * 
 * <p>API Response: POST /v2/{accountId}/{bankCode}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietQRCreateResponse {

    /**
     * Mã phản hồi từ API.
     * - "00": Thành công
     * - Other: Lỗi
     */
    @JsonProperty("code")
    private String code;

    /**
     * Thông điệp phản hồi.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Dữ liệu QR code trả về (chỉ có khi code = "00").
     */
    @JsonProperty("data")
    private QRData data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QRData {
        
        /**
         * UUID của giao dịch (dùng để query trạng thái).
         */
        @JsonProperty("id")
        private String id;

        /**
         * Base64 encoded QR code image.
         * Có thể dùng trực tiếp trong thẻ <img src="data:image/png;base64,...">
         */
        @JsonProperty("qrDataURL")
        private String qrDataURL;

        /**
         * Raw string để generate QR code phía client (nếu cần).
         */
        @JsonProperty("qrCode")
        private String qrCode;

        /**
         * Link deep payment (dùng cho app banking).
         */
        @JsonProperty("deepLink")
        private String deepLink;

        /**
         * Original account number đã truyền lên.
         */
        @JsonProperty("accountNumber")
        private String accountNumber;

        /**
         * Original amount đã truyền lên.
         */
        @JsonProperty("amount")
        private Long amount;

        /**
         * Original addInfo đã truyền lên.
         */
        @JsonProperty("addInfo")
        private String addInfo;
    }

    /**
     * Kiểm tra xem response có thành công không.
     */
    public boolean isSuccess() {
        return "00".equals(code);
    }
}
