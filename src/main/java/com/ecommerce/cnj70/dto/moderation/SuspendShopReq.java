package com.ecommerce.cnj70.dto.moderation;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phase 6 — Suspend Shop Request.
 *
 * <p>{@code reason} is REQUIRED. Validation: not null, not empty, not
 * whitespace-only.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspendShopReq {

    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;

    @Size(max = 2000, message = "Note must not exceed 2000 characters")
    private String note;
}
