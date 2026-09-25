package com.ecommerce.cnj70.dto.review;

import com.ecommerce.cnj70.enums.ModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2B — Review Moderation Decision Response.
 *
 * <p>Returned to the controller after a Moderator action. The UI uses
 * it to render flash messages and redirect.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewModerationDecision {
    private String reviewId;
    private String action;            // APPROVE / REJECT / HIDE / UNHIDE / ESCALATE
    private ModerationStatus newStatus;
    private String reason;
    private String moderatorId;
    private String moderatorEmail;
    private LocalDateTime decidedAt;
    private String message;
}
