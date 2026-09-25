package com.ecommerce.cnj70.dto.moderation;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phase 2A — Escalate Product Request.
 *
 * <p>{@code reason} is REQUIRED (Phase 2A §32).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EscalateProductReq {

    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;
}
