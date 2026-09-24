package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Phase 2A — Moderation History Document.
 *
 * <p>Persisted on every Moderator action (Approve / Reject / Escalate).
 * Read-only history of decisions taken against Products by Moderators.</p>
 *
 * <p>This is the Phase 2A implementation of "Moderation History". The
 * AuditLog backend (Phase 3A / 3C) will be a separate, broader log
 * capturing all administrative events; this document is specifically
 * the per-product Moderation history required by Phase 2A §41.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "moderation_history")
public class ModerationHistory {

    @Id
    private String id;

    /** Resource type for future extensibility (always "PRODUCT" in Phase 2A). */
    @Builder.Default
    private String resourceType = "PRODUCT";

    @Indexed
    private String resourceId;

    private String resourceName;

    private ModerationAction action;

    private ModerationStatus beforeStatus;

    private ModerationStatus afterStatus;

    private String reason;

    private String moderatorId;

    private String moderatorEmail;

    private UserRole moderatorRole;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
}
