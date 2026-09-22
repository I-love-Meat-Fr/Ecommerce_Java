package com.ecommerce.cnj70.dto.response;

import com.ecommerce.cnj70.enums.ReportCaseDecision;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TASK #26 — Response trả về thông tin một ReportCase.
 *
 * Sử dụng cho cả list (queue) và detail.
 * Khi list: chỉ trả các field cơ bản
 * Khi detail: thêm `evidence`, `autoFlags`, `targetDetail` (Product/Review detail)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseRes {

    private String id;
    private ReportTargetType targetType;
    private String targetId;
    private String targetSnapshot;

    /** Reporter info */
    private String reporterId;
    private String reporterUsername;
    private String source; // USER / AUTO

    /** Reason */
    private String reason;
    private String description;

    /** Status & decision */
    private ReportCaseStatus status;
    private ReportCaseDecision decision;
    private String decisionReason;
    private String note;

    /** Assignment */
    private String assignedModeratorId;
    private String assignedModeratorUsername;

    /** Auto flags (nếu có) */
    private List<String> autoFlags;

    /** Evidence */
    private List<String> evidence;

    /** Snapshot trạng thái trước/sau */
    private String targetStatusBefore;
    private String targetStatusAfter;

    /** Priority */
    private int priority;

    /** Timestamps */
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime updatedAt;
}
