package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #16 — Request khi Vendor nộp KYC.
 *
 * Lưu ý:
 * - citizenId: bắt buộc, 9 hoặc 12 số (CCCD 9 số / CCCD 12 số)
 * - taxCode: optional, 10 hoặc 13 số
 * - bankAccount: optional, 6-20 số
 *
 * Data được mã hóa AES-256-GCM bằng CryptoUtil TRƯỚC KHI gửi sang provider.
 * Backend KHÔNG lưu plaintext PII vào DB.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycSubmitRequest {

    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(regexp = "^[0-9]{9}$|^[0-9]{12}$",
             message = "CCCD phải có 9 hoặc 12 chữ số")
    private String citizenId;

    @Size(max = 20, message = "Mã số thuế tối đa 20 ký tự")
    @Pattern(regexp = "^[0-9]{0,13}$",
             message = "Mã số thuế chỉ chứa chữ số, tối đa 13 số")
    private String taxCode;

    @Size(max = 20, message = "Số tài khoản tối đa 20 ký tự")
    @Pattern(regexp = "^[0-9]{0,20}$",
             message = "Số tài khoản chỉ chứa chữ số, tối đa 20 số")
    private String bankAccount;
}
