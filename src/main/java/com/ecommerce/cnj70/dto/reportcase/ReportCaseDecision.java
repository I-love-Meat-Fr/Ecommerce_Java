package com.ecommerce.cnj70.dto.reportcase;

import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2C — ReportCase Moderator Decision Response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseDecision {
    private String caseId;
    private String action;            // APPROVE / REJECT / ESCALATE
    private ReportCaseStatus newStatus;
    private String reason;
    private EscalationSeverity escalationSeverity;
    private String moderatorId;
    private String moderatorEmail;
    private LocalDateTime decidedAt;
    private String message;
}
