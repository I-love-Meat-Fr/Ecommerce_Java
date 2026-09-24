package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.service.AutoModerationService;
import com.ecommerce.cnj70.service.automation.AutoCheckStrategy;
import com.ecommerce.cnj70.service.automation.AutoCheckStrategy.AutoCheckContext;
import com.ecommerce.cnj70.service.automation.AutoCheckVerdict;
import com.ecommerce.cnj70.service.automation.AutoModerationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * C1 — Pipeline orchestrator (default implementation).
 *
 * <p>Tự động inject mọi bean {@link AutoCheckStrategy} qua Spring
 * ({@code List<AutoCheckStrategy>}). Khi task C2-C6 implement từng check,
 * chúng sẽ tự động xuất hiện trong pipeline mà không cần sửa class này.
 *
 * <p>Hành vi aggregate:
 * <ul>
 *   <li>HARD_REJECT xuất hiện → short-circuit, trả REJECTED_AUTO ngay.</li>
 *   <li>SOFT_FLAG xuất hiện → tiếp tục chạy các check còn lại để thu thập
 *       đủ flags/reasons; cuối cùng trả MANUAL_REVIEW (nếu không có HARD).</li>
 *   <li>Mọi check PASS → ACTIVE.</li>
 * </ul>
 *
 * <p>Fail-safe: nếu một check throw exception, KHÔNG chặn cả pipeline.
 * Check lỗi được tính như {@code SOFT_FLAG} (priority thấp) để Moderator
 * review; pipeline vẫn hoàn tất với verdicts còn lại.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoModerationServiceImpl implements AutoModerationService {

    /** Tất cả AutoCheckStrategy beans, Spring tự inject. */
    private final List<AutoCheckStrategy> checks;

    @Override
    @Transactional(readOnly = true)
    public AutoModerationResult runProductChecks(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        Map<String, Object> sharedState = new HashMap<>();
        AutoCheckContext ctx = AutoCheckContext.builder()
                .product(product)
                .sharedState(sharedState)
                .build();

        log.info("[AutoMod] Running pipeline for product id={} name='{}' shopId={} (checks={})",
                product.getId(), product.getName(), product.getShopId(), checks.size());

        List<String> flags = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        if (checks.isEmpty()) {
            log.warn("[AutoMod] No AutoCheckStrategy beans registered. Pipeline runs as identity (all PASS). "
                    + "This is expected at C1 startup — C2-C6 will register their checks as they are implemented.");
        }

        for (AutoCheckStrategy check : checks) {
            String checkId = check.id();
            try {
                AutoCheckVerdict verdict = check.check(ctx);
                if (verdict == null) {
                    log.warn("[AutoMod] Check {} returned null verdict — counted as ERROR", checkId);
                    flagSoftAsError(checkId, flags, reasons, "verdict=null");
                    continue;
                }

                if (verdict.isHardReject()) {
                    flags.add(checkId + ":" + verdict.getFlagCode());
                    reasons.add(verdict.getMessage());
                    log.warn("[AutoMod] HARD_REJECT from {} for product id={}: {}",
                            checkId, product.getId(), verdict.getMessage());
                    // Short-circuit: bỏ qua các check còn lại, đánh dấu reject luôn.
                    return AutoModerationResult.autoReject(product, flags, reasons);
                }

                if (verdict.isSoftFlag()) {
                    flags.add(checkId + ":" + verdict.getFlagCode());
                    reasons.add(verdict.getMessage());
                    log.info("[AutoMod] SOFT_FLAG from {} for product id={}: {}",
                            checkId, product.getId(), verdict.getMessage());
                    // Tiếp tục chạy các check sau để thu thập đủ flags.
                }

                // PASS → nothing.

            } catch (Exception ex) {
                // Fail-safe: một check bị lỗi không được chặn cả pipeline.
                // Tính như SOFT_FLAG để Moderator review.
                log.error("[AutoMod] Check {} threw exception for product id={}: {}",
                        checkId, product.getId(), ex.getMessage(), ex);
                flagSoftAsError(checkId, flags, reasons, ex.getMessage());
            }
        }

        if (!flags.isEmpty()) {
            return AutoModerationResult.manualReview(product, flags, reasons);
        }
        log.info("[AutoMod] All checks passed for product id={} → ACTIVE", product.getId());
        return AutoModerationResult.pass(product);
    }

    private void flagSoftAsError(String checkId, List<String> flags, List<String> reasons, String errorDetail) {
        flags.add(checkId + ":ERROR");
        String safeDetail = errorDetail != null ? errorDetail : "unknown";
        reasons.add("Check " + checkId + " gặp lỗi runtime: " + safeDetail);
    }
}
