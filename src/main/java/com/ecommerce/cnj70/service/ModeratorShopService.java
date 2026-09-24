package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.moderation.ShopModerationContext;
import com.ecommerce.cnj70.dto.moderation.ShopModerationDecision;
import com.ecommerce.cnj70.dto.moderation.ShopModerationQueueItem;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 6 — Moderator Shop Moderation Service (Contract).
 *
 * <p>Boundaries:</p>
 * <ul>
 *     <li>Owner: Moderator side (this phase)</li>
 *     <li>Not Admin Shop Management (different service)</li>
 *     <li>Not Shop creation/approval flow (Vendor-side)</li>
 * </ul>
 *
 * <h3>State transition contract</h3>
 * <pre>
 *   PENDING      → APPROVED      (Moderator approves a pending registration)
 *   APPROVED     → RESTRICTED    (Moderator restricts an active shop)
 *   APPROVED     → SUSPENDED     (Moderator suspends an active shop)
 *   RESTRICTED   → APPROVED      (Moderator restores after a restriction)
 *   SUSPENDED    → APPROVED      (Moderator restores after a suspension)
 * </pre>
 *
 * <p>This contract is intentionally conservative: Moderator Shop
 * moderation never overwrites a terminal SUSPENDED status from an
 * Admin enforcement — it can only restore it. Admin enforcement is
 * still authoritative.</p>
 */
public interface ModeratorShopService {

    /**
     * Queue page — all shops in {@code PENDING}, {@code APPROVED},
     * {@code RESTRICTED}, {@code SUSPENDED} states. Suspended shops from
     * Admin enforcement are still visible but Moderator cannot act on them.
     *
     * @param q                optional shop name keyword (case-insensitive)
     * @param status           optional ShopStatus filter (null = all in-queue states)
     */
    Page<ShopModerationQueueItem> listQueue(String q, ShopStatus status, Pageable pageable);

    /**
     * Aggregated detail context for the moderator shop detail page.
     */
    ShopModerationContext getContext(String shopId);

    /**
     * Approve a pending shop registration. Requires {@code status == PENDING}.
     */
    ShopModerationDecision approve(String shopId, CustomUserDetails moderator, String clientIp);

    /**
     * Suspend a shop. Requires a non-blank reason.
     *
     * <p>Suspended is treated as a Moderator-issued suspension
     * (recordable in {@code ModerationHistory}). If the shop was already
     * suspended by Admin, this returns 409 (idempotent reject).</p>
     */
    ShopModerationDecision suspend(String shopId, String reason, String note,
                                   CustomUserDetails moderator, String clientIp);

    /**
     * Restore a previously suspended/restricted shop. Requires a
     * non-blank reason. Only restores Moderator-issued states — Admin
     * enforcement is preserved.
     */
    ShopModerationDecision restore(String shopId, String reason, String note,
                                   CustomUserDetails moderator, String clientIp);

    /**
     * Apply a lighter restriction to an approved shop. Requires a
     * non-blank reason.
     */
    ShopModerationDecision restrict(String shopId, String reason, String note,
                                    CustomUserDetails moderator, String clientIp);
}
