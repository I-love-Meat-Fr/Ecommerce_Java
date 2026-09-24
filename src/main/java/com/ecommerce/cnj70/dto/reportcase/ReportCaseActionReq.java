package com.ecommerce.cnj70.dto.reportcase;

import com.ecommerce.cnj70.enums.EscalationSeverity;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phase 2C — Request DTO for Reject and Escalate actions.
 *
 * <p>Both require non-blank reason (Phase 2C §27). {@code severity}
 * is required only for Escalate.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseActionReq {

    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;

    /** Required only for Escalate. */
    private EscalationSeverity severity;
}
