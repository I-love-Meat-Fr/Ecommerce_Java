package com.ecommerce.cnj70.enums;

/**
 * Phase 3C — Resource type that a Violation is linked to.
 *
 * <p>Per Phase 3C §21, the Violation backend tracks which domain entity
 * triggered the violation. This enum mirrors the backend contract values
 * verbatim (no renaming). A Vendor or Shop violation may have no specific
 * resource — use {@code OTHER}.</p>
 */
public enum ViolationResourceType {
    PRODUCT,
    REVIEW,
    SHOP,
    VENDOR,
    ORDER,
    REPORT_CASE,
    OTHER
}
