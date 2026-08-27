package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Phase 1 bootstrap helper — only active when the Spring profile
 * {@code admin-bootstrap} is enabled.
 *
 * <p>It resets the password hash of the admin account (admin2@gmail.com)
 * to the well-known BCrypt hash of "admin123", and ensures
 * role = ADMIN and status = ACTIVE. This is a Phase 1 test fixture only
 * and must NOT be enabled in production.</p>
 *
 * <p>When the system property {@code admin-bootstrap.lock-email} is set,
 * the user with that email will be transitioned to status = LOCKED,
 * which is used for Phase 1 LOCKED-user scenarios.</p>
 */
@Slf4j
@Component
@Profile("admin-bootstrap")
@RequiredArgsConstructor
public class AdminBootstrap implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin2@gmail.com";
    private static final String ADMIN_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = userRepository.findByEmail(ADMIN_EMAIL).orElse(null);
        if (admin == null) {
            log.warn("AdminBootstrap: no user found with email {}. Nothing to do.", ADMIN_EMAIL);
            return;
        }

        admin.setRole(UserRole.ADMIN);
        admin.setStatus(AccountStatus.ACTIVE);
        admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        userRepository.save(admin);

        log.info("AdminBootstrap: password for {} has been reset to '{}'", ADMIN_EMAIL, ADMIN_PASSWORD);

        String lockEmail = System.getProperty("admin-bootstrap.lock-email");
        if (lockEmail == null || lockEmail.isBlank()) {
            lockEmail = System.getenv("ADMIN_BOOTSTRAP_LOCK_EMAIL");
        }
        if (lockEmail != null && !lockEmail.isBlank()) {
            User toLock = userRepository.findByEmail(lockEmail).orElse(null);
            if (toLock != null) {
                toLock.setStatus(AccountStatus.LOCKED);
                userRepository.save(toLock);
                log.info("AdminBootstrap: user {} has been transitioned to LOCKED", lockEmail);
            } else {
                log.warn("AdminBootstrap: user {} not found, cannot lock", lockEmail);
            }
        }
    }
}
