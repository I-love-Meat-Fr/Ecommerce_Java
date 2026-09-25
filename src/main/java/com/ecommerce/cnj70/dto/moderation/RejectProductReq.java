package com.ecommerce.cnj70.dto.moderation;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phase 2A — Reject Product Request.
 *
 * <p>{@code reason} is REQUIRED (Phase 2A §29 / §30). Validation:
 * not null, not empty, not whitespace-only.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectProductReq {

    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;
}
