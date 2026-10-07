package com.ecommerce.cnj70.dto.vietqr;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO để tạo QR Code thanh toán VietQR.
 * 
 * <p>API Endpoint: POST /v2/{accountId}/{bankCode}
 * 
 * @see <a href="https://vietqr.net/developer">VietQR Developer Documentation</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VietQRCreateRequest {

    /**
     * Số tài khoản ngân hàng của merchant.
     * Format: string, max 20 characters.
     */
    @JsonProperty("accountNumber")
    private String accountNumber;

    /**
     * Tên chủ tài khoản (không dấu, viết hoa).
     * Format: string, max 25 characters (theo quy tắc VietQR).
     */
    @JsonProperty("accountName")
    private String accountName;

    /**
     * Số tiền thanh toán (VNĐ).
     * Format: integer, min 1000, max 999999999999.
     */
    @JsonProperty("amount")
    private Long amount;

    /**
     * Nội dung chuyển khoản.
     * Format: string, max 50 characters.
     * Nên format: "THANH TOAN DH [orderId]"
     */
    @JsonProperty("addInfo")
    private String addInfo;

    /**
     * Tên ngân hàng của merchant (để hiển thị trong app ngân hàng).
     * Format: string.
     */
    @JsonProperty("accountNameBank")
    private String accountNameBank;
}
