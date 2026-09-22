package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.enums.ReportCaseDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #26 — Request khi Moderator ra quyết định xử lý một ReportCase.
 *
 * Bắt buộc với REJECT và ESCALATE: reason phải có ít nhất 10 ký tự.
 * Với APPROVE: reason là optional (nhưng vẫn nên có để audit).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCaseDecisionReq {

    @NotNull(message = "Quyết định không được để trống")
    private ReportCaseDecision decision;

    /**
     * Lý do quyết định.
     * - Bắt buộc với REJECT (ít nhất 10 ký tự)
     * - Bắt buộc với ESCALATE (ít nhất 10 ký tự)
     * - Optional với APPROVE
     */
    @Size(min = 0, max = 1000)
    private String reason;

    /** Ghi chú thêm của Moderator (optional) */
    @Size(max = 2000)
    private String note;
}
