package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseDecision;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportReason;
import com.ecommerce.cnj70.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TASK #26 — ReportCase (Entity chính của luồng Moderator).
 *
 * Lưu trữ thông tin một phiên kiểm duyệt do:
 *   1. Auto Moderation phát hiện vi phạm (reporterId = null, source = AUTO)
 *   2. Customer/Vendor report (reporterId != null, source = USER)
 *
 * Luồng:
 *   PENDING  ─── APPROVE/REJECT ──▶  RESOLVED
 *      │
 *      └── ESCALATE ─▶  ESCALATED (chuyển cho Admin xử lý)
 *
 * Đảm bảo tính toàn vẹn:
 *   - Transactional update khi moderator ra quyết định
 *   - AuditLog luôn được ghi kèm
 *   - targetStatusBefore được lưu để rollback nếu cần
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "report_cases")
public class ReportCase {

    @Id
    private String id;

    // ===== Phase 2C — Resource targeting (aligned with ModeratorReportCaseService) =====
    /** Loại resource bị report (PRODUCT/REVIEW) - Phase 2C */
    @Indexed
    private ReportCaseResourceType resourceType;

    /** ID của resource bị report - Phase 2C */
    @Indexed
    private String resourceId;

    /** Tên resource (snapshot) - Phase 2C */
    private String resourceName;

    // ===== Legacy fields (kept from TASK #26) =====
    /** Loại đối tượng bị report */
    @Indexed
    private ReportTargetType targetType;

    /** ID của đối tượng bị report (productId, reviewId...) */
    @Indexed
    private String targetId;

    /** Tên/snapshot của target (để hiển thị khi target đã bị xóa) */
    private String targetSnapshot;

    /** User ID người report (null nếu do Auto Moderation) */
    @Indexed
    private String reporterId;

    /** Tên người report - Phase 2C */
    private String reporterName;

    /** Auto moderation status (string) - Phase 2C */
    private String autoModerationStatus;

    /** Email moderator được assigned - Phase 2C */
    private String assignedModeratorEmail;

    /** Email người report - Phase 2C */
    private String reporterEmail;

    /** Vendor ID sở hữu shop chứa resource - Phase 2C */
    private String vendorId;

    /** Vendor name - Phase 2C */
    private String vendorName;

    /** Shop ID - Phase 2C */
    private String shopId;

    /** Shop name - Phase 2C */
    private String shopName;

    /** Loại nguồn: USER (do customer report) hoặc AUTO (do hệ thống) */
    @Builder.Default
    private String source = "USER";

    /** Lý do report ngắn gọn (do user nhập hoặc do auto flag) - String (legacy TASK #26) */
    private String reason;

    /** Lý do report enum (Phase 2C) - dùng cho ReportCaseService mới */
    private ReportReason reportReason;

    /** Mô tả chi tiết thêm */
    private String description;

    /** Bằng chứng đính kèm (URL hình ảnh, link text...) */
    @Builder.Default
    private List<String> evidence = new ArrayList<>();

    /** Các cờ cảnh báo từ Auto Moderation (matched keywords, blacklisted words...) */
    @Builder.Default
    private List<String> autoFlags = new ArrayList<>();

    /** Trạng thái case */
    @Indexed
    @Builder.Default
    private ReportCaseStatus status = ReportCaseStatus.PENDING;

    /** ID Moderator được giao xử lý (null = chưa assign, ai cũng có thể nhận) */
    @Indexed
    private String assignedModeratorId;

    /** Quyết định cuối cùng của Moderator */
    private ReportCaseDecision decision;

    /** Lý do quyết định (bắt buộc khi REJECT) */
    private String decisionReason;

    /** Ghi chú thêm của Moderator */
    private String note;

    /** Trạng thái target TRƯỚC khi Moderator xử lý (để rollback nếu cần) */
    private String targetStatusBefore;

    /** Trạng thái target SAU khi Moderator xử lý */
    private String targetStatusAfter;

    /** Priority (cao hơn = xử lý trước). Mặc định: 0 */
    @Builder.Default
    private int priority = 0;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    /** Thời điểm Moderator assign */
    private LocalDateTime assignedAt;

    /** Thời điểm Moderator ra quyết định */
    private LocalDateTime resolvedAt;

    /** Phase 2C: Escalation details */
    private EscalationSeverity escalationSeverity;

    /** Phase 2C: Lý do escalate */
    private String escalationReason;

    /** Phase 2C: Admin đã review escalation chưa */
    private boolean escalationReviewed;

    /** Phase 2C: Email moderator ra quyết định */
    private String decisionModeratorEmail;

    /** Phase 2C: ID moderator ra quyết định */
    private String decisionModeratorId;

    /** Phase 2C: Ghi chú quyết định */
    private String decisionNote;

    /** Phase 2C: Thời điểm ra quyết định */
    private LocalDateTime decidedAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
