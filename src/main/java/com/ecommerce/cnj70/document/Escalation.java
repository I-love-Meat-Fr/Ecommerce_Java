package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Phase 2C — Escalation document. Extended in Phase 3A to track
 * Admin lifecycle.
 *
 * <p>Persists the act of escalating a ReportCase to Admin. Per Phase
 * 2C §22, Moderator does not perform Admin enforcement — Moderator
 * only records the escalation; Admin backend consumes this document
 * (Phase 3A scope).</p>
 *
 * <h3>Phase 3A additions</h3>
 * <ul>
 *     <li>{@link #status} — Admin lifecycle: PENDING / IN_REVIEW / RESOLVED</li>
 *     <li>{@link #assignedAdminId} / {@link #assignedAdminEmail} — Admin ownership</li>
 *     <li>{@link #decisionAdminId} / {@link #decisionAdminEmail} — final decision actor</li>
 *     <li>{@link #decisionAction} — ViolationAction chosen by Admin</li>
 *     <li>{@link #decisionNote} — internal note from Admin</li>
 *     <li>{@link #resolvedAt} — terminal timestamp</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "escalations")
public class Escalation {

    @Id
    private String id;

    /** ID of the ReportCase being escalated. */
    @Indexed
    private String reportCaseId;

    /** Resource type at the time of escalation. */
    private ReportCaseResourceType resourceType;

    /** Opaque resource ID. */
    private String resourceId;

    /** Vendor / shop for Admin routing. */
    private String vendorId;
    private String shopId;

    /** Reason provided by the Moderator (mandatory). */
    private String reason;

    /** Severity selected by Moderator (Phase 2C §22 — not auto-derived). */
    private EscalationSeverity severity;

    /** Moderator who performed the escalation. */
    private String moderatorId;
    private String moderatorEmail;
    private String moderatorRole;

    /** Status mirrored from the source ReportCase at the time of escalation. */
    private ReportCaseStatus sourceStatus;

    /**
     * Phase 3A — Admin lifecycle. {@code PENDING} when first persisted
     * by Moderator; {@code IN_REVIEW} when an Admin opens it;
     * {@code RESOLVED} (terminal) after Admin takes an enforcement
     * action or dismisses.
     */
    @Indexed
    private Status status;

    /** Admin who opened / is reviewing this escalation. */
    private String assignedAdminId;
    private String assignedAdminEmail;

    /** Admin who finalized the decision. */
    private String decisionAdminId;
    private String decisionAdminEmail;

    /** Violation action chosen by Admin (Phase 3A §21 contract). */
    private com.ecommerce.cnj70.enums.ViolationAction decisionAction;

    /** Internal note from Admin. */
    private String decisionNote;

    /** Timestamp when the escalation moved to RESOLVED. */
    private LocalDateTime resolvedAt;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Phase 3A — Admin lifecycle states. Internal to Escalation
     * (separate from {@link ReportCaseStatus} which lives on
     * the source ReportCase).
     */
    public enum Status {
        /** Newly created by Moderator, no Admin has opened it yet. */
        PENDING,
        /** An Admin has claimed / is reviewing this escalation. */
        IN_REVIEW,
        /** Admin resolved (terminal). */
        RESOLVED;

        public boolean isTerminal() {
            return this == RESOLVED;
        }
    }
}
