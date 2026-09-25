package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.dto.review.VerifiedPurchaseResult;
import com.ecommerce.cnj70.service.VerifiedPurchaseGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Phase 2B — No-op Verified Purchase Provider.
 *
 * <p>Used when no real verified-purchase backend is wired in. Always
 * returns {@code verified=false}. This is NOT a fake — it is a
 * documented fallback so the Moderator UI degrades gracefully.</p>
 *
 * <p>Production fake is FORBIDDEN per Phase 2B §19.</p>
 */
@Slf4j
@Component
@Profile("!verified-purchase")
public class NoopVerifiedPurchaseGateway implements VerifiedPurchaseGateway {

    public NoopVerifiedPurchaseGateway() {
        log.info("NoopVerifiedPurchaseGateway active — Verified Purchase integration not wired in. " +
                "Phase 2B UI will display 'Verified Purchase: not available'.");
    }

    @Override
    public VerifiedPurchaseResult check(String userId, String productId) {
        return VerifiedPurchaseResult.notVerified();
    }
}
