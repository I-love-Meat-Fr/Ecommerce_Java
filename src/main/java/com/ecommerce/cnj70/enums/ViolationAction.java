package com.ecommerce.cnj70.enums;

/**
 * Phase 3A — Violation enforcement action, aligned with Marketplace Policy
 * BỘ LUẬT §ViolationAction.
 *
 * <p>Maps to actual enforcement contracts:</p>
 * <ul>
 *     <li>{@link #WARNING} — issued by Admin (note in {@code Violation.reason})</li>
 *     <li>{@link #PRODUCT_HIDDEN} — maps to {@code Product.status = HIDDEN} (AdminProductService)</li>
 *     <li>{@link #PRODUCT_REJECTED} — maps to {@code Product.status = HIDDEN} + reason (escalation path)</li>
 *     <li>{@link #SHOP_RESTRICTED} — maps to {@code ShopStatus.RESTRICTED}</li>
 *     <li>{@link #SHOP_SUSPENDED} — maps to {@code ShopStatus.SUSPENDED}</li>
 *     <li>{@link #VENDOR_BANNED} — maps to {@code User.status = LOCKED} for VENDOR</li>
 *     <li>{@link #FUNDS_FROZEN} — Phase 4C scope (Finance) — not implemented here</li>
 * </ul>
 *
 * <p>Note: Policy maps {@code FUNDS_FROZEN} to Finance. Phase 3A does
 * NOT touch Finance / Settlement / Payout / Refund. The enum value
 * is exposed for forward compatibility, but no enforcement contract
 * is wired in Phase 3A.</p>
 */
public enum ViolationAction {
    WARNING,
    PRODUCT_HIDDEN,
    PRODUCT_REJECTED,
    SHOP_RESTRICTED,
    SHOP_SUSPENDED,
    VENDOR_BANNED,
    FUNDS_FROZEN
}
