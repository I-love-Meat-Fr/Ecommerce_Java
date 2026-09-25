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
 * Phase 2C — ReportCase Queue Item (read-only DTO for the queue page).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseQueueItem {
    private String caseId;
    private ReportCaseResourceType resourceType;
    private String resourceId;
    private String resourceName;
    private String reporterId;
    private String reporterName;
    private ReportReason reason;
    private String descriptionPreview;
    private EscalationSeverity severity;
    private ReportCaseStatus status;
    private String autoModerationStatus;
    private List<String> autoFlags;
    private String vendorName;
    private String shopName;
    private String assignedModeratorEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private long evidenceCount;

    public static ReportCaseQueueItem fromEntity(ReportCase c) {
        return ReportCaseQueueItem.builder()
                .caseId(c.getId())
                .resourceType(c.getResourceType())
                .resourceId(c.getResourceId())
                .resourceName(c.getResourceName())
                .reporterId(c.getReporterId())
                .reporterName(c.getReporterName())
                .reason(c.getReportReason())
                .descriptionPreview(c.getDescription() != null && c.getDescription().length() > 100
                        ? c.getDescription().substring(0, 100) + "…"
                        : c.getDescription())
                .severity(c.getEscalationSeverity())
                .status(c.getStatus())
                .autoModerationStatus(c.getAutoModerationStatus())
                .autoFlags(c.getAutoFlags())
                .vendorName(c.getVendorName())
                .shopName(c.getShopName())
                .assignedModeratorEmail(c.getAssignedModeratorEmail())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .evidenceCount(c.getEvidence() != null ? c.getEvidence().size() : 0L)
                .build();
    }
}
