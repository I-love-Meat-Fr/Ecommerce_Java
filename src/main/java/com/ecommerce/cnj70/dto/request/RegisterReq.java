package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #22 — Register request với bắt buộc accept Terms + Privacy.
 *
 * Lưu ý: KHÔNG dùng @AssertTrue trên manual getter (vì sẽ xung đột với
 * Lombok-generated `getAcceptTerms()` cho Boolean field — Hibernate Validator
 * gặp lỗi khi có 2 getter cho cùng 1 property với 2 kiểu trả về khác nhau).
 * Việc check acceptTerms/acceptPrivacy sẽ được thực hiện trong AuthController.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterReq {

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;

    private String address;

    private String role;

    // ===== TASK #22: Bắt buộc accept Terms + Privacy =====
    // Check thủ công trong AuthController (xem comment ở đầu class).
    private Boolean acceptTerms;
    private Boolean acceptPrivacy;

    /** Optional: opt-in nhận email marketing */
    @Builder.Default
    private Boolean marketingOptIn = false;
}
