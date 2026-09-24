package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 6 — Shop Moderation Queue Item.
 *
 * <p>Single row in the Moderator Shop queue page. Aggregates minimal data
 * needed to decide whether to drill into a Shop detail. Reuses existing
 * {@code Shop} entity fields and adds Moderation-specific view fields.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopModerationQueueItem {
    private String shopId;
    private String shopName;
    private String ownerId;
    private String ownerEmail;
    private String ownerName;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private ShopStatus status;
    private boolean active;
    /** Snapshot of total report cases against this shop (for queue triage). */
    private long totalCases;
    /** Snapshot of total violations against this shop. */
    private long totalViolations;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime enforcementAt;
    private String enforcementReason;
}
