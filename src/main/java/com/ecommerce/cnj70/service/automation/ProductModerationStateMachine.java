package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Group 1 — State Machine thực thi transition cho Product moderation.
 *
 * <p>Đảm bảo <b>C1 invariant</b>: Vendor không thể bypass pipeline bằng cách
 * set thẳng Product sang {@code ACTIVE} / {@code APPROVED}. Mọi thay đổi
 * state đều phải đi qua {@link #transition(Product, ProductModerationState, String, String)}.</p>
 *
 * <p>Side effects của transition (cùng atomic transaction):</p>
 * <ol>
 *   <li>Validate transition hợp lệ theo bảng trong
 *       {@link ProductModerationState#canTransitionTo(ProductModerationState)}.</li>
 *   <li>Set {@code Product.status} (qua {@link ProductModerationState#toProductStatus()}).</li>
 *   <li>Set {@code Product.moderationStatus} + {@code moderationActorId} +
 *       {@code moderationReason} + {@code moderationAt}.</li>
 *   <li>Ghi AuditLog với severity phù hợp.</li>
 * </ol>
 *
 * <p>Việc {@code productRepository.save(product)} là trách nhiệm của caller
 * (vd {@code ProductServiceImpl.runAutoModerationPipeline}) — State Machine
 * chỉ mutate field trên instance, KHÔNG gọi repository để giữ separation
 * of concerns.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductModerationStateMachine {

    private static final String SYSTEM_ACTOR_ID = "SYSTEM";
    private static final String SYSTEM_ACTOR_NAME = "SYSTEM";

    private final AuditLogService auditLogService;

    /**
     * Apply 1 transition lên Product.
     *
     * @param product    product instance (chưa save)
     * @param target     state đích
     * @param actorId    id actor thực hiện (SYSTEM, vendorId, moderatorId, adminId)
     * @param reason     lý do (vd "BLACKLIST_KEYWORD hit 'vũ khí'", "Moderator APPROVE")
     * @throws BadRequestException nếu transition không hợp lệ
     */
    public void transition(Product product,
                           ProductModerationState target,
                           String actorId,
                           String reason) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        if (target == null) {
            throw new IllegalArgumentException("target state không được null");
        }

        // Resolve current state từ Product.moderationStatus (nếu có) hoặc fallback ProductStatus.
        ProductModerationState current = resolveCurrentState(product);

        if (!current.canTransitionTo(target)) {
            String msg = String.format(
                    "Không thể chuyển Product (id=%s) từ trạng thái moderation '%s' sang '%s'",
                    product.getId(), current, target);
            log.warn("[StateMachine] Invalid transition productId={} {} -> {} (actor={})",
                    product.getId(), current, target, actorId);

            // Ghi audit CRITICAL để admin điều tra
            try {
                auditLogService.log(
                        AuditAction.PRODUCT_AUTO_REJECT,
                        "PRODUCT",
                        product.getId(),
                        actorId,
                        actorId,
                        actorId,
                        AuditSeverity.CRITICAL,
                        msg,
                        Map.of("from", current.name(), "to", target.name(), "reason", reason)
                );
            } catch (Exception ignored) {
                // Audit fail không block business logic
            }

            throw new BadRequestException(msg);
        }

        // Apply transition lên Product instance (caller save sau)
        product.setStatus(target.toProductStatus());
        product.setModerationStatus(com.ecommerce.cnj70.enums.ModerationStatus
                .valueOf(target.name()));
        product.setModerationActorId(actorId);
        product.setModerationReason(reason);
        product.setModerationAt(LocalDateTime.now());

        // Audit log
        AuditAction action = pickAuditAction(current, target);
        AuditSeverity severity = (target == ProductModerationState.AUTO_REJECTED
                || target == ProductModerationState.REJECTED)
                ? AuditSeverity.WARNING
                : AuditSeverity.INFO;

        try {
            auditLogService.log(
                    action,
                    "PRODUCT",
                    product.getId(),
                    actorId != null ? actorId : SYSTEM_ACTOR_ID,
                    actorId != null ? actorId : SYSTEM_ACTOR_NAME,
                    actorId != null ? actorId : SYSTEM_ACTOR_ID,
                    severity,
                    "Product state transition: " + current + " -> " + target
                            + (reason != null ? " | " + reason : ""),
                    Map.of("from", current.name(), "to", target.name())
            );
        } catch (Exception ex) {
            log.warn("[StateMachine] AuditLog failed (non-fatal): {}", ex.getMessage());
        }

        log.info("[StateMachine] Transition productId={} {} -> {} actor={}",
                product.getId(), current, target, actorId);
    }

    /**
     * Resolve current state từ Product. Ưu tiên {@code moderationStatus}
     * (Phase 2A) — fallback sang ProductStatus nếu chưa có.
     */
    private ProductModerationState resolveCurrentState(Product product) {
        if (product.getModerationStatus() != null) {
            try {
                return ProductModerationState.valueOf(product.getModerationStatus().name());
            } catch (IllegalArgumentException ex) {
                // ModerationStatus enum rộng hơn ProductModerationState; fallback.
            }
        }
        return switch (product.getStatus()) {
            case PENDING_AUTO -> ProductModerationState.PENDING_AUTO;
            case ACTIVE -> ProductModerationState.APPROVED;
            case MANUAL_REVIEW -> ProductModerationState.PENDING_MANUAL;
            case REJECTED_AUTO -> ProductModerationState.AUTO_REJECTED;
            case HIDDEN -> ProductModerationState.REJECTED;
            case OUT_OF_STOCK, DRAFT -> ProductModerationState.PENDING_AUTO;
        };
    }

    private AuditAction pickAuditAction(ProductModerationState from, ProductModerationState to) {
        if (to == ProductModerationState.AUTO_PASSED || to == ProductModerationState.APPROVED) {
            return AuditAction.PRODUCT_AUTO_PASS;
        }
        if (to == ProductModerationState.AUTO_REJECTED || to == ProductModerationState.REJECTED) {
            return AuditAction.PRODUCT_AUTO_REJECT;
        }
        if (to == ProductModerationState.PENDING_MANUAL || to == ProductModerationState.ESCALATED) {
            return AuditAction.PRODUCT_MANUAL_REVIEW;
        }
        return AuditAction.PRODUCT_AUTO_PASS;
    }
}
