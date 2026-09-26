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
 * Việc check acceptTerms sẽ được thực hiện trong AuthServiceImpl.register().
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

    // ===== TASK #22: Bắt buộc đồng ý Điều khoản sử dụng + Chính sách bảo mật (1 checkbox duy nhất) =====
    // Check thủ công trong AuthController (xem comment ở đầu class).
    // Khi user tick "Tôi đồng ý...", server vẫn ghi nhận accepted*Version cho cả TERMS và PRIVACY.
    private Boolean acceptTerms;

    /** Optional: opt-in nhận email marketing */
    @Builder.Default
    private Boolean marketingOptIn = false;
}
