package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ReportCaseDecision;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
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

    /** Loại nguồn: USER (do customer report) hoặc AUTO (do hệ thống) */
    @Builder.Default
    private String source = "USER";

    /** Lý do report ngắn gọn (do user nhập hoặc do auto flag) */
    private String reason;

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

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
