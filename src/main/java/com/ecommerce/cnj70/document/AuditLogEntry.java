package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Phase 3C — AuditLog entry (Admin read-only query document).
 *
 * <p><b>Contract-first.</b> This document is the query/display layer
 * for audit events. The AuditEventWriter (Phase 2A / 3A) is the
 * write seam — when the real audit-log backend is connected, it
 * persists AuditLogEntry documents. The current
 * {@code LoggingAuditEventWriter} writes to the log (no DB persistence).</p>
 *
 * <h3>Immutable</h3>
 * This collection is append-only. Records MUST NOT be updated or deleted
 * by the Admin query layer. Any mutation attempt must be rejected at the
 * service level (Phase 3C §40 — READ ONLY).</p>
 *
 * <h3>PII handling</h3>
 * {@code ip} is stored verbatim (server IP address is not personal data
 * under GDPR/VN law). No plaintext PII (citizen ID, bank account, etc.)
 * is stored in this document.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_log")
@CompoundIndexes({
    @CompoundIndex(name = "actor_created_idx", def = "{'actorId': 1, 'createdAt': -1}"),
    @CompoundIndex(name = "resource_type_id_idx", def = "{'resourceType': 1, 'resourceId': 1}"),
    @CompoundIndex(name = "action_created_idx", def = "{'action': 1, 'createdAt': -1}")
})
public class AuditLogEntry {

    @Id
    private String id;

    /** Who performed the action. */
    @Indexed
    private String actorId;

    private String actorEmail;

    @Indexed
    private UserRole role;

    /** Action code, e.g. PRODUCT_SUSPENDED, SHOP_SUSPENDED, VENDOR_BANNED. */
    @Indexed
    private String action;

    @Indexed
    private String resourceType;

    private String resourceId;

    /** Human-readable reason for the action (may be null for INFO events). */
    private String reason;

    /** Severity level (optional, for violation-related actions). */
    private String severity;

    /** State before the action (optional). */
    private String before;

    /** State after the action (optional). */
    private String after;

    /**
     * Client IP address. Server-side IP address, not personal data.
     * Phase 3C §41: stored for security audit, not PII.
     */
    private String ip;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
}
