package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 6 — Shop Moderation Detail Context.
 *
 * <p>Aggregated DTO passed to the Moderator Shop detail view. Composed
 * from existing {@code Shop} entity fields plus Moderation-specific
 * snapshots (vendor info, product count, violation count, report
 * snapshot).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopModerationContext {
    private String shopId;
    private String shopName;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private ShopStatus status;
    private boolean active;

    private String ownerId;
    private String ownerEmail;
    private String ownerName;

    /** Count of products linked to this shop. */
    private long productCount;
    /** Count of report cases linked to this shop. */
    private long totalCases;
    /** Count of violations linked to this shop. */
    private long totalViolations;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Latest enforcement metadata (Phase 3A — Admin enforcement). */
    private String enforcementActorId;
    private String enforcementReason;
    private LocalDateTime enforcementAt;
}
