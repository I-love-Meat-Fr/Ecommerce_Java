package com.ecommerce.cnj70.enums;

/**
 * TASK #26 — Quyết định mà Moderator đưa ra đối với một ReportCase.
 *
 * - APPROVE:   Duyệt (target an toàn, hiển thị lại)
 * - REJECT:    Từ chối (target bị ẩn/xóa, kèm lý do)
 * - ESCALATE:  Leo thang (đẩy lên Admin do vi phạm nghiêm trọng)
 *
 * Mapping sang ReportCaseStatus:
 *   APPROVE  ─▶ RESOLVED
 *   REJECT   ─▶ RESOLVED
 *   ESCALATE ─▶ ESCALATED
 */
public enum ReportCaseDecision {
    APPROVE,
    REJECT,
    ESCALATE
}
