package com.ecommerce.cnj70.enums;

/**
 * Phase 2C — Resource type a ReportCase refers to.
 *
 * <p>Independent enum — Phase 2C needs to distinguish what kind of
 * resource the case targets (product vs review) without coupling to
 * Product/Review entities directly. The resourceId is opaque.</p>
 */
public enum ReportCaseResourceType {
    PRODUCT,
    REVIEW
}
