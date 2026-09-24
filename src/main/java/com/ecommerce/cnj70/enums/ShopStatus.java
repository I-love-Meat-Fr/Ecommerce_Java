package com.ecommerce.cnj70.enums;

public enum ShopStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Phase 3A — Admin enforcement via SUSPEND_SHOP. */
    SUSPENDED,
    /** Phase 3A — Admin enforcement via SHOP_RESTRICTED (lighter than SUSPENDED). */
    RESTRICTED;

    public boolean isTerminalEnforcement() {
        return this == SUSPENDED;
    }

    public boolean isSuspendedOrRestricted() {
        return this == SUSPENDED || this == RESTRICTED;
    }
}
