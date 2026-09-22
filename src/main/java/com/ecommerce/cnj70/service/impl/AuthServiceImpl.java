package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.LoginReq;
import com.ecommerce.cnj70.dto.request.RegisterReq;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.LegalDocumentType;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.AuthService;
import com.ecommerce.cnj70.service.LegalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LegalDocumentService legalDocumentService;
    private final AuditLogService auditLogService;

    @Override
    public User register(RegisterReq request) {
        // ===== TASK #22: Bắt buộc accept Terms + Privacy =====
        if (request.getAcceptTerms() == null || !request.getAcceptTerms()) {
            throw new BadRequestException("Bạn phải đồng ý với Điều khoản sử dụng để đăng ký");
        }
        if (request.getAcceptPrivacy() == null || !request.getAcceptPrivacy()) {
            throw new BadRequestException("Bạn phải đồng ý với Chính sách bảo mật để đăng ký");
        }

        if (existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã tồn tại");
        }

        // TASK 1.5 — Public registration MUST NOT create ADMIN.
        UserRole role = UserRole.CUSTOMER;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            try {
                UserRole requested = UserRole.valueOf(request.getRole().toUpperCase());
                if (requested == UserRole.ADMIN) {
                    throw new BadRequestException("ADMIN role cannot be created via public registration");
                }
                role = requested;
            } catch (BadRequestException e) {
                throw e;
            } catch (IllegalArgumentException e) {
                role = UserRole.CUSTOMER;
            }
        }

        // ===== TASK #22: Lấy version Terms/Privacy hiện tại để lưu =====
        // Nếu LegalDocument chưa có (test/dev), mặc định version = 1
        int termsVersion = getCurrentVersion(LegalDocumentType.TERMS);
        int privacyVersion = getCurrentVersion(LegalDocumentType.PRIVACY);

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(role)
                .status(AccountStatus.ACTIVE)
                // ===== TASK #22: Lưu acceptedAt + version =====
                .acceptedTermsAt(LocalDateTime.now())
                .acceptedTermsVersion(termsVersion)
                .acceptedPrivacyAt(LocalDateTime.now())
                .acceptedPrivacyVersion(privacyVersion)
                .marketingOptIn(Boolean.TRUE.equals(request.getMarketingOptIn()))
                .build();

        User saved = userRepository.save(user);

        // ===== TASK #24: AuditLog cho register =====
        auditLogService.logInfo(
                AuditAction.REGISTER_SUCCESS,
                "USER",
                saved.getId(),
                saved.getId(),
                saved.getEmail(),
                role.name(),
                "User đăng ký thành công, role=" + role +
                        ", termsVersion=" + termsVersion + ", privacyVersion=" + privacyVersion
        );

        return saved;
    }

    @Override
    public CustomUserDetails login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            // ===== TASK #24: AuditLog cho login failed =====
            auditLogService.logWarning(
                    AuditAction.LOGIN_FAILED,
                    "USER",
                    null,
                    null,
                    email,
                    "UNKNOWN",
                    "Login failed: sai mật khẩu cho email=" + email
            );
            throw new BadRequestException("Invalid email or password");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            auditLogService.logWarning(
                    AuditAction.LOGIN_FAILED,
                    "USER",
                    user.getId(),
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name(),
                    "Login failed: tài khoản không active, status=" + user.getStatus()
            );
            throw new BadRequestException("Tài khoản không hoạt động. Trạng thái: " + user.getStatus());
        }

        // ===== TASK #24: AuditLog cho login success =====
        auditLogService.logInfo(
                AuditAction.LOGIN_SUCCESS,
                "USER",
                user.getId(),
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                "User đăng nhập thành công"
        );

        return CustomUserDetails.fromUser(user);
    }

    @Override
    public CustomUserDetails login(LoginReq request) {
        return login(request.getEmail(), request.getPassword());
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * TASK #22: Lấy version hiện tại của LegalDocument.
     * Nếu không tìm thấy (test environment chưa seed), fallback về 1.
     */
    private int getCurrentVersion(LegalDocumentType type) {
        try {
            return legalDocumentService.getByType(type).getVersion();
        } catch (Exception e) {
            // LegalDocument chưa được seed → dùng version 1 (backward-compatible)
            return 1;
        }
    }
}
