package com.ecommerce.cnj70.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
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
 * Quy tắc:
 * - acceptTerms: PHẢI true để đăng ký thành công
 * - acceptPrivacy: PHẢI true để đăng ký thành công
 * - marketingOptIn: optional, mặc định false
 *
 * User cũ (chưa có acceptedTermsAt): xử lý backward-compatible theo Phase.
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

    @AssertTrue(message = "Bạn phải đồng ý với Điều khoản sử dụng để đăng ký")
    @JsonProperty("acceptTerms")
    public boolean isAcceptTerms() {
        return acceptTerms != null && acceptTerms;
    }

    @AssertTrue(message = "Bạn phải đồng ý với Chính sách bảo mật để đăng ký")
    @JsonProperty("acceptPrivacy")
    public boolean isAcceptPrivacy() {
        return acceptPrivacy != null && acceptPrivacy;
    }

    private Boolean acceptTerms;
    private Boolean acceptPrivacy;

    /** Optional: opt-in nhận email marketing */
    @Builder.Default
    private Boolean marketingOptIn = false;
}
