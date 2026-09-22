package com.ecommerce.cnj70.enums;

/**
 * TASK #26 — Trạng thái của một ReportCase (phiên kiểm duyệt).
 *
 * Luồng trạng thái:
 *   PENDING  ─── Moderator xử lý ───▶  RESOLVED (đã duyệt/từ chối)
 *      │
 *      └── Moderator escalate ──▶  ESCALATED (đẩy lên Admin)
 *
 * PENDING:     Vừa được tạo, đang chờ Moderator xử lý
 * RESOLVED:    Moderator đã ra quyết định cuối cùng (Approve hoặc Reject)
 * ESCALATED:   Đã chuyển lên Admin Dashboard do vi phạm nghiêm trọng
 */
public enum ReportCaseStatus {
    PENDING,
    RESOLVED,
    ESCALATED
}
