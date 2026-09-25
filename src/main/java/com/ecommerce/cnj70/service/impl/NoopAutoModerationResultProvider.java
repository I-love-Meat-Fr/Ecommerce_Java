package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.dto.moderation.AutoModerationResult;
import com.ecommerce.cnj70.service.AutoModerationResultProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Phase 2A — Default no-op AutoModerationResultProvider.
 *
 * <p>Active when no real Auto Moderation backend is wired in. Always
 * returns {@link Optional#empty()} so that the UI degrades gracefully
 * to "Auto Moderation Result: not available" rather than displaying
 * fake data.</p>
 *
 * <p>Production fake is FORBIDDEN per Phase 2A §11 / §46. This class
 * is NOT a fake — it is a documented no-op that signals "backend not
 * connected". Real Auto Moderation results will appear once the
 * integration team provides a real {@code AutoModerationResultProvider}
 * bean (e.g. {@code AutoModerationClientProvider}).</p>
 */
@Slf4j
@Component
@Profile("!auto-moderation")
public class NoopAutoModerationResultProvider implements AutoModerationResultProvider {

    public NoopAutoModerationResultProvider() {
        log.info("NoopAutoModerationResultProvider active — Auto Moderation backend not wired in. " +
                "Phase 2A UI will display 'Auto Moderation Result: not available'.");
    }

    @Override
    public Optional<AutoModerationResult> getResult(String productId) {
        // Documented no-op: backend unavailable → empty result, NOT a fake.
        return Optional.empty();
    }
}
