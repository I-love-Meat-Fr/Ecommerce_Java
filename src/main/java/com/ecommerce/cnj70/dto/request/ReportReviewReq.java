package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #15 — Request khi Customer report một Review.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportReviewReq {

    @NotBlank(message = "Lý do report không được để trống")
    private String reason;

    /** Mô tả chi tiết thêm (optional) */
    private String description;
}
