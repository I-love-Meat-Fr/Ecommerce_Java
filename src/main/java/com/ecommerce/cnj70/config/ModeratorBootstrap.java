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
 * Phase 1 — Moderator test account bootstrap.
 *
 * <p>Active only when Spring profile {@code moderator-bootstrap} is enabled.
 * Ensures a MODERATOR test account exists with a well-known password so
 * Phase 1 Security / Login / Route Matrix tests can run without manual
 * DB seeding.</p>
 *
 * <p>The account is upserted: if a user with the configured email already
 * exists, its role is forced to MODERATOR, status to ACTIVE, and the
 * password is reset. If the user does not exist, it is created.</p>
 *
 * <p>This is a Phase 1 test fixture only. MUST NOT be enabled in
 * production. Default Spring profile does not include
 * {@code moderator-bootstrap}, so this bean is inactive by default.</p>
 *
 * <p>To enable for local testing:
 * <pre>
 *   mvn spring-boot:run -Dspring-boot.run.profiles=moderator-bootstrap
 * </pre>
 * </p>
 */
@Slf4j
@Component
@Profile("moderator-bootstrap")
@RequiredArgsConstructor
public class ModeratorBootstrap implements CommandLineRunner {

    private static final String MODERATOR_EMAIL = "moderator2@gmail.com";
    private static final String MODERATOR_PASSWORD = "moderator123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User moderator = userRepository.findByEmail(MODERATOR_EMAIL).orElse(null);

        if (moderator == null) {
            moderator = User.builder()
                    .email(MODERATOR_EMAIL)
                    .password(passwordEncoder.encode(MODERATOR_PASSWORD))
                    .fullName("Moderator Test")
                    .role(UserRole.MODERATOR)
                    .status(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(moderator);
            log.info("ModeratorBootstrap: created MODERATOR test account {} (password='{}')",
                    MODERATOR_EMAIL, MODERATOR_PASSWORD);
            return;
        }

        // Ensure existing account has the right role/status/password for testing.
        moderator.setRole(UserRole.MODERATOR);
        moderator.setStatus(AccountStatus.ACTIVE);
        moderator.setPassword(passwordEncoder.encode(MODERATOR_PASSWORD));
        userRepository.save(moderator);

        log.info("ModeratorBootstrap: reset MODERATOR test account {} (password='{}')",
                MODERATOR_EMAIL, MODERATOR_PASSWORD);
    }
}
