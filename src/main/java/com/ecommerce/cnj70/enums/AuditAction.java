package com.ecommerce.cnj70.enums;

/**
 * TASK #23 — Loại action được audit log.
 *
 * Phân loại để dễ filter và báo cáo.
 */
public enum AuditAction {
    // User management
    USER_CREATED,
    USER_LOCKED,
    USER_UNLOCKED,
    USER_DELETED,

    // Shop management
    SHOP_CREATED,
    SHOP_APPROVED,
    SHOP_REJECTED,
    SHOP_SUSPENDED,

    // KYC
    KYC_SUBMITTED,
    KYC_APPROVED,
    KYC_REJECTED,
    KYC_CALLBACK,

    // Product moderation
    PRODUCT_AUTO_FLAG,
    PRODUCT_AUTO_PASS,
    PRODUCT_AUTO_REJECT,
    PRODUCT_MANUAL_REVIEW,
    PRODUCT_APPROVED,
    PRODUCT_REJECTED,

    // Review moderation
    REVIEW_CREATED,
    REVIEW_REPORTED,
    REVIEW_HIDDEN,
    REVIEW_DELETED,

    // Complaint
    COMPLAINT_CREATED,
    COMPLAINT_ESCALATED,
    COMPLAINT_RESOLVED,
    COMPLAANT_OVERDUE,

    // Violation
    VIOLATION_WARNED,
    VIOLATION_SUSPENDED,
    VIOLATION_BANNED,

    // Admin
    ADMIN_LOGIN,
    ADMIN_ACTION,

    // Legal
    LEGAL_DOCUMENT_UPDATED,
    LEGAL_TERMS_ACCEPTED,
    LEGAL_PRIVACY_ACCEPTED,

    // Other
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    REGISTER_SUCCESS,
    OTHER
}
