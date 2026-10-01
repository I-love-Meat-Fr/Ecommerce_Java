package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.VendorSubscription;
import com.ecommerce.cnj70.service.PremiumPackageService;
import com.ecommerce.cnj70.service.SchedulerIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Premium Subscription Expiration Scheduler.
 *
 * <p>Quét hàng ngày các VendorSubscription ACTIVE đã quá {@code endDate}
 * và đánh dấu EXPIRED. Đồng thời clear {@code shop.premiumActive}
 * nếu shop đang reference đúng sub này.</p>
 *
 * <p>Production-safe pattern (giống {@link ComplaintEscalationScheduler}):</p>
 * <ul>
 *   <li>Idempotent: dùng {@link SchedulerIdempotencyService} với key stable
 *       để không expire 1 sub 2 lần</li>
 *   <li>Restart-safe: state lưu MongoDB</li>
 *   <li>Per-record failure isolation: 1 record fail không kill cả batch</li>
 *   <li>System actor: chạy không cần authenticated principal</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PremiumSubscriptionScheduler {

    /** Job name cho idempotency record. */
    public static final String JOB_PREMIUM_EXPIRE = "premium-subscription-expire";

    /** Operation name cho idempotency. */
    public static final String OP_EXPIRE = "EXPIRE";

    /** Entity type cho idempotency. */
    public static final String ENTITY_SUBSCRIPTION = "VENDOR_SUBSCRIPTION";

    private final PremiumPackageService premiumPackageService;
    private final SchedulerIdempotencyService idempotencyService;

    /**
     * Cron mặc định: 1h sáng mỗi ngày. Override qua {@code premium.scheduler.cron} trong application.yml.
     */
    @Scheduled(cron = "${premium.scheduler.cron:0 0 1 * * *}")
    public void expirePremiumSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Scheduler [PremiumExpire] starting at {}", now);

        List<VendorSubscription> expired;
        try {
            expired = premiumPackageService.findExpiredSubscriptions(now);
        } catch (RuntimeException ex) {
            log.error("Scheduler [PremiumExpire] lookup failed: {}", ex.getMessage(), ex);
            return;
        }

        if (expired.isEmpty()) {
            log.debug("Scheduler [PremiumExpire] no expired subscriptions at {}", now);
            return;
        }

        log.info("Scheduler [PremiumExpire] found {} expired subscriptions at {}", expired.size(), now);

        int success = 0, skipped = 0, failed = 0;
        for (VendorSubscription sub : expired) {
            try {
                processOne(sub);
                success++;
            } catch (RuntimeException ex) {
                failed++;
                log.error("Scheduler [PremiumExpire] failure for subscriptionId={}: {}",
                        sub.getId(), ex.getMessage(), ex);
            }
        }
        log.info("Scheduler [PremiumExpire] complete: success={} skipped={} failed={}",
                success, skipped, failed);
    }

    private void processOne(VendorSubscription sub) {
        if (!idempotencyService.tryReserve(JOB_PREMIUM_EXPIRE,
                ENTITY_SUBSCRIPTION, sub.getId(), OP_EXPIRE)) {
            log.info("Scheduler [PremiumExpire] skip subscription {} — already attempted",
                    sub.getId());
            return;
        }

        try {
            // Defensive re-check trước khi mutate: nếu vendor vừa renew thì
            // endDate đã được extend, markExpired sẽ tự skip (check trong service).
            premiumPackageService.markExpired(sub.getId());
            idempotencyService.markSuccess(JOB_PREMIUM_EXPIRE, ENTITY_SUBSCRIPTION, sub.getId());
            log.info("Scheduler [PremiumExpire] expired subscription {} (shopId={})",
                    sub.getId(), sub.getShopId());
        } catch (RuntimeException ex) {
            idempotencyService.markFailed(JOB_PREMIUM_EXPIRE, ENTITY_SUBSCRIPTION, sub.getId(),
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }
}
