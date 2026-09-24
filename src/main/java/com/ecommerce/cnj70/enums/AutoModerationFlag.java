package com.ecommerce.cnj70.enums;

/**
 * Phase 2A — Auto Moderation flag categories (integration seam).
 *
 * <p>Display-only enum. The actual flag detection is performed by the
 * Auto Moderation Engine (external). Phase 2A only consumes these values
 * to render UI badges.</p>
 *
 * <p>Phase 2A never recomputes flags from Product content — that would
 * be frontend recomputation which is forbidden per Phase 2A §24.</p>
 */
public enum AutoModerationFlag {
    BLACKLIST,
    DUPLICATE,
    IMAGE,
    PRICE,
    CATEGORY,
    CONTENT,
    BRAND,
    WARRANTY,
    MANUFACTURER,
    OTHER
}
