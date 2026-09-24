package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.moderation.ModerationDecision;
import com.ecommerce.cnj70.dto.moderation.ProductModerationContext;
import com.ecommerce.cnj70.dto.moderation.ProductModerationQueueItem;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;

/**
 * Phase 2A — Moderator Product Moderation Service.
 *
 * <p>Boundaries:</p>
 * <ul>
 *     <li>Owner: Moderator side (Phase 2A scope)</li>
 *     <li>Not Admin Product Management (different service)</li>
 *     <li>Not Auto Moderation Engine (integration seam only)</li>
 * </ul>
 *
 * <p>State transition rule (Phase 2A §34):</p>
 * <pre>
 *   PENDING_MANUAL → APPROVED  → ProductStatus = ACTIVE
 *   PENDING_MANUAL → REJECTED  → ProductStatus = HIDDEN
 *   PENDING_MANUAL → ESCALATED → ProductStatus = HIDDEN  (admin will enforce)
 * </pre>
 */
public interface ModeratorProductService {

    /**
     * Queue page — products with {@code moderationStatus IN (PENDING_MANUAL, AUTO_PASSED, AUTO_REJECTED)}.
     *
     * @param q                optional name keyword (case-insensitive)
     * @param moderationStatus optional filter (null = all in-queue states)
     */
    Page<ProductModerationQueueItem> listQueue(String q, com.ecommerce.cnj70.enums.ModerationStatus moderationStatus,
                                               org.springframework.data.domain.Pageable pageable);

    /**
     * Aggregated detail context for the moderator product detail page.
     */
    ProductModerationContext getContext(String productId);

    /**
     * Approve product. Requires {@code moderationStatus == PENDING_MANUAL}.
     */
    ModerationDecision approve(String productId, CustomUserDetails moderator, String clientIp);

    /**
     * Reject product. Requires {@code moderationStatus == PENDING_MANUAL} and a non-blank {@code reason}.
     */
    ModerationDecision reject(String productId, String reason, CustomUserDetails moderator, String clientIp);

    /**
     * Escalate product. Requires {@code moderationStatus == PENDING_MANUAL} and a non-blank {@code reason}.
     * Admin enforcement is OUT OF SCOPE for Phase 2A.
     */
    ModerationDecision escalate(String productId, String reason, CustomUserDetails moderator, String clientIp);
}
