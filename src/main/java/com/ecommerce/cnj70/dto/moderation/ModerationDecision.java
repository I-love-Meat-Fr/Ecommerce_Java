package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2A — Moderation Decision Response.
 *
 * <p>Returned to the controller after a Moderator action
 * (Approve / Reject / Escalate) is processed. UI uses it to render a
 * flash message and redirect.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationDecision {
    private String productId;
    private ModerationAction action;
    private ModerationStatus newStatus;
    private String reason;
    private String moderatorId;
    private String moderatorEmail;
    private LocalDateTime decidedAt;
    private String message;
}
