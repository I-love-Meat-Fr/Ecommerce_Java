package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.dto.request.ReportCaseCreateReq;
import com.ecommerce.cnj70.dto.request.ReportCaseDecisionReq;
import com.ecommerce.cnj70.dto.response.ReportCaseRes;
import com.ecommerce.cnj70.dto.response.ReportCaseStatsRes;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * TASK #26 — ReportCase Service.
 *
 * Quản lý workflow kiểm duyệt cho Moderator:
 *   1. Tạo case (từ User report hoặc Auto Moderation)
 *   2. Moderator assign/xem case
 *   3. Moderator ra quyết định: APPROVE / REJECT / ESCALATE
 *
 * Đảm bảo:
 *   - Transactional cho mọi action ra quyết định
 *   - AuditLog luôn được ghi kèm
 *   - Không cho Moderator thao tác với case đã RESOLVED/ESCALATED
 */
public interface ReportCaseService {

    // ===== CRUD cơ bản =====

    /**
     * Tạo ReportCase mới (từ User report hoặc Auto Moderation).
     * Nếu đã có case PENDING cho cùng target, trả về case cũ (idempotent).
     */
    ReportCaseRes createCase(ReportCaseCreateReq request, String reporterId, String reporterUsername);

    /**
     * Lấy thông tin case theo ID.
     */
    ReportCaseRes getCaseById(String caseId);

    /**
     * Lấy danh sách case (queue cho Moderator).
     * Mặc định filter theo status = PENDING, sort theo priority DESC, createdAt ASC.
     */
    Page<ReportCaseRes> getQueue(ReportCaseStatus status, ReportTargetType targetType, Pageable pageable);

    /**
     * Lấy lịch sử case của một target.
     */
    Page<ReportCaseRes> getCasesByTarget(ReportTargetType targetType, String targetId, Pageable pageable);

    // ===== Assignment =====

    /**
     * Moderator tự assign case cho mình.
     */
    ReportCaseRes assignCase(String caseId, String moderatorId, String moderatorUsername);

    // ===== Quyết định (3 Actions) =====

    /**
     * Action 1 - APPROVE: target an toàn, hiển thị lại.
     * ReportCase.status = RESOLVED.
     */
    ReportCaseRes approveCase(String caseId, ReportCaseDecisionReq request,
                              String moderatorId, String moderatorUsername);

    /**
     * Action 2 - REJECT: target bị ẩn/xóa, bắt buộc có lý do.
     * ReportCase.status = RESOLVED.
     */
    ReportCaseRes rejectCase(String caseId, ReportCaseDecisionReq request,
                             String moderatorId, String moderatorUsername);

    /**
     * Action 3 - ESCALATE: chuyển lên Admin do vi phạm nghiêm trọng.
     * ReportCase.status = ESCALATED.
     */
    ReportCaseRes escalateCase(String caseId, ReportCaseDecisionReq request,
                               String moderatorId, String moderatorUsername);

    // ===== Stats =====

    /**
     * Thống kê ReportCase cho Dashboard.
     */
    ReportCaseStatsRes getStats();
}
