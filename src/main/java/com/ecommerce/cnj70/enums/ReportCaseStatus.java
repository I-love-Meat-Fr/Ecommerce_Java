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
    OPEN,
    IN_REVIEW,
    RESOLVED,
    APPROVED,
    REJECTED,
    ESCALATED;

    /**
     * Phase 2C — Terminal states are those where the case is closed and no
     * further Moderator action is required.
     */
    public boolean isTerminal() {
        return this == RESOLVED || this == ESCALATED;
    }
}
