package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.LoginReq;
import com.ecommerce.cnj70.dto.request.RegisterReq;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User register(RegisterReq request) {
        if (existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // TASK 1.5 — Public registration MUST NOT create ADMIN.
        // Client-supplied role is rejected when it equals ADMIN.
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

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .role(role)
                .status(AccountStatus.ACTIVE)
                .build();

        return userRepository.save(user);
    }

    @Override
    public CustomUserDetails login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        // TASK 1.6 — Account state login guard.
        // Only ACTIVE accounts may authenticate. LOCKED/UNVERIFIED are rejected.
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active. Current status: " + user.getStatus());
        }

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
}
