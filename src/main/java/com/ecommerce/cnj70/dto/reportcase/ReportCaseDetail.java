package com.ecommerce.cnj70.dto.reportcase;

import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 2C — ReportCase Detail (full context for the detail page).
 *
 * <p>Composes the {@link ReportCase} entity with resource context
 * (Product / Shop / Vendor / Customer / Reporter) and history entries
 * (previous cases, escalation history).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseDetail {
    private String caseId;
    private ReportCaseResourceType resourceType;
    private String resourceId;
    private String resourceName;
    private String reporterId;
    private String reporterName;
    private String reporterEmail;
    private ReportReason reason;
    private String description;
    private String autoModerationStatus;
    private List<String> autoFlags;
    private List<String> evidence;
    private String vendorId;
    private String vendorName;
    private String shopId;
    private String shopName;
    private ReportCaseStatus status;
    private String assignedModeratorId;
    private String assignedModeratorEmail;
    private String decisionModeratorId;
    private String decisionModeratorEmail;
    private String decisionReason;
    private String decisionNote;
    private LocalDateTime decidedAt;
    private EscalationSeverity escalationSeverity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Phase 2C §18 — previous cases on the same resource. */
    private List<ReportCaseQueueItem> previousCases;

    /** Phase 2C §17 — vendor history snapshot. */
    private long vendorTotalCases;
    private long vendorOpenCases;
    private long vendorEscalatedCases;
}
