package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 6 — Shop Moderation Decision Response.
 *
 * <p>Returned to the controller after a Moderator Shop action
 * (Suspend / Restore / Restrict) is processed. UI uses it to render a
 * flash message and redirect.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopModerationDecision {
    private String shopId;
    private String shopName;
    private ModerationAction action;
    private ShopStatus beforeStatus;
    private ShopStatus afterStatus;
    private String reason;
    private String moderatorId;
    private String moderatorEmail;
    private LocalDateTime decidedAt;
    private String message;
}
