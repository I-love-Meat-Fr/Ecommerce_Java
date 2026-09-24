package com.ecommerce.cnj70.enums;

/**
 * Phase 3 — Complaint escalation level.
 *
 * <p>Per Marketplace Policy §7:</p>
 * <ul>
 *     <li>LEVEL_0 — Customer ↔ Vendor</li>
 *     <li>LEVEL_1 — Moderator</li>
 *     <li>LEVEL_2 — Admin</li>
 * </ul>
 */
public enum ComplaintLevel {
    LEVEL_0,
    LEVEL_1,
    LEVEL_2;

    public boolean isAtLeast(ComplaintLevel other) {
        return this.ordinal() >= other.ordinal();
    }
}
