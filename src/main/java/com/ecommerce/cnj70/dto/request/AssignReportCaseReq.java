package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #26 — Request để Moderator tự assign một case cho mình.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignReportCaseReq {

    @NotBlank(message = "Moderator ID không được để trống")
    private String moderatorId;
}
