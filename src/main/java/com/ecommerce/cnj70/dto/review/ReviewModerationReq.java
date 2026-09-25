package com.ecommerce.cnj70.dto.review;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Phase 2B — Generic Review Moderation Request (reason required for
 * Reject / Hide / Escalate).
 *
 * <p>{@code reason} is REQUIRED per Phase 2B §31 for Reject / Hide /
 * Escalate. Backend validates; front-end validation is UX only.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationReq {

    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;
}
