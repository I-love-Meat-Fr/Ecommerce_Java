package com.ecommerce.cnj70.enums;

/**
 * Phase 2C — Report reason / category for a ReportCase.
 *
 * <p>Aligned with marketplace policy categories. Used for filtering
 * and for Moderator queue priority display. Severity hints come from
 * the integration seam (auto-moderation) — Phase 2C never derives
 * severity on its own.</p>
 */
public enum ReportReason {
    SPAM,
    FAKE_PRODUCT,
    COUNTERFEIT_IP,
    PROHIBITED_CONTENT,
    OFFENSIVE_LANGUAGE,
    PII_LEAK,
    OFF_PLATFORM_CONTACT,
    MISLEADING_DESCRIPTION,
    DUPLICATE_LISTING,
    PRICE_ANOMALY,
    WRONG_CATEGORY,
    REVIEW_MANIPULATION,
    OTHER
}
