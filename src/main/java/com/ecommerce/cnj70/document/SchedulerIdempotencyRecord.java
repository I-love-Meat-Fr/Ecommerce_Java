package com.ecommerce.cnj70.document;

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
 * Phase 3C — Scheduler Idempotency Record.
 *
 * <p>Persistent record to ensure scheduler operations are idempotent
 * across restarts and concurrent executions.</p>
 *
 * <h3>Idempotency Key Strategy</h3>
 * <p>The {@code operationKey} is computed deterministically from:
 * <ul>
 *     <li>Job name (e.g. "complaint-l0-escalation")</li>
 *     <li>Target entity type (e.g. "COMPLAINT")</li>
 *     <li>Target entity ID (e.g. complaint ID)</li>
 * </ul>
 * Format: {@code <jobName>:<entityType>:<entityId>}</p>
 *
 * <h3>Use Cases</h3>
 * <ul>
 *     <li>Prevent duplicate escalation when scheduler re-runs on the same overdue complaint.</li>
 *     <li>Prevent duplicate enforcement when two scheduler instances run concurrently.</li>
 *     <li>Survive application restart — idempotency state persists in MongoDB.</li>
 * </ul>
 *
 * <h3>Index Safety</h3>
 * <p>Unique compound index on {@code operationKey}. If existing data causes
 * a conflict, the index creation will fail loudly — we do NOT delete existing
 * data to resolve conflicts (Phase 3C §10).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scheduler_idempotency")
@CompoundIndexes({
    @CompoundIndex(name = "uniq_operation_key", def = "{'operationKey': 1}", unique = true),
    @CompoundIndex(name = "job_status_idx", def = "{'jobName': 1, 'status': 1}"),
    @CompoundIndex(name = "expires_idx", def = "{'expiresAt': 1}")
})
public class SchedulerIdempotencyRecord {

    @Id
    private String id;

    /** Stable idempotency key — unique per (jobName, entityType, entityId). */
    @Indexed
    private String operationKey;

    /** Job name that produced this record (e.g. "complaint-l0-escalation"). */
    @Indexed
    private String jobName;

    /** Entity type (e.g. "COMPLAINT"). */
    private String entityType;

    /** Entity ID this operation was applied to. */
    @Indexed
    private String entityId;

    /** Operation name (e.g. "ESCALATE_L1", "ESCALATE_L2"). */
    private String operation;

    /** Outcome of the operation: SUCCESS / FAILED / SKIPPED. */
    private String status;

    /** Reason / detail message for SKIPPED / FAILED outcomes. */
    private String detail;

    /** Timestamp when the record was created. */
    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    /**
     * Optional TTL — record can be cleaned up after this time.
     * Used by the scheduler to allow re-processing after the deadline
     * has passed or the state has changed.
     */
    private LocalDateTime expiresAt;
}
