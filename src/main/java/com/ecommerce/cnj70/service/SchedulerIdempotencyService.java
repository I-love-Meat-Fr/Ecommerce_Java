package com.ecommerce.cnj70.service;

/**
 * Phase 3C — Scheduler Idempotency Service.
 *
 * <p>Centralizes idempotency logic for scheduler jobs. Ensures:</p>
 * <ul>
 *     <li>Each (jobName, entityType, entityId) operation runs at most once.</li>
 *     <li>State persists across restarts via MongoDB.</li>
 *     <li>Concurrent scheduler invocations don't duplicate operations.</li>
 * </ul>
 *
 * <h3>Idempotency Key Format</h3>
 * <pre>{@code <jobName>:<entityType>:<entityId>}</pre>
 *
 * <h3>Strategy</h3>
 * <ol>
 *     <li>Compute the stable operationKey from inputs.</li>
 *     <li>Attempt to reserve (INSERT) — relies on unique index for race protection.</li>
 *     <li>If reserve fails (duplicate key), skip — operation already in progress or completed.</li>
 *     <li>Execute the business operation.</li>
 *     <li>Mark the record as SUCCESS / FAILED / SKIPPED.</li>
 * </ol>
 */
public interface SchedulerIdempotencyService {

    /**
     * Atomically reserve an operation. Returns true if this caller won the
     * race and should proceed; false if another caller already reserved.
     */
    boolean tryReserve(String jobName, String entityType, String entityId,
                       String operation);

    /**
     * Mark the reserved operation as successfully completed.
     */
    void markSuccess(String jobName, String entityType, String entityId);

    /**
     * Mark the reserved operation as failed — does NOT release the reservation
     * to prevent retry storms.
     */
    void markFailed(String jobName, String entityType, String entityId, String detail);

    /**
     * Mark the reserved operation as skipped (e.g. precondition no longer met).
     */
    void markSkipped(String jobName, String entityType, String entityId, String detail);

    /**
     * Check whether an operation has already been attempted for this key.
     */
    boolean hasAttempted(String jobName, String entityType, String entityId);

    /**
     * Compute the stable operation key.
     */
    String computeKey(String jobName, String entityType, String entityId);
}
