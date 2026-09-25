package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.SchedulerIdempotencyRecord;
import com.ecommerce.cnj70.repository.SchedulerIdempotencyRepository;
import com.ecommerce.cnj70.service.SchedulerIdempotencyService;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Phase 3C — MongoDB-backed Scheduler Idempotency Service.
 *
 * <p>Uses the unique index on {@code operationKey} as the race-condition
 * guard. Concurrent inserts will result in exactly one success; all other
 * callers will see a {@link DuplicateKeyException} and skip.</p>
 *
 * <h3>Restart Safety</h3>
 * <p>State persists in MongoDB. After a restart, the scheduler will see
 * the previous records and skip operations that have already completed.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerIdempotencyServiceImpl implements SchedulerIdempotencyService {

    /** Status enum values — kept as constants for consistency. */
    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private final SchedulerIdempotencyRepository repository;

    @Override
    public String computeKey(String jobName, String entityType, String entityId) {
        return jobName + ":" + entityType + ":" + entityId;
    }

    @Override
    public boolean tryReserve(String jobName, String entityType, String entityId, String operation) {
        String key = computeKey(jobName, entityType, entityId);

        // Fast-path: check if record already exists.
        Optional<SchedulerIdempotencyRecord> existing = repository.findByOperationKey(key);
        if (existing.isPresent()) {
            log.debug("Idempotency hit (already exists): key={} status={}", key, existing.get().getStatus());
            return false;
        }

        // Attempt to reserve via INSERT — unique index guards against concurrent inserts.
        SchedulerIdempotencyRecord record = SchedulerIdempotencyRecord.builder()
                .operationKey(key)
                .jobName(jobName)
                .entityType(entityType)
                .entityId(entityId)
                .operation(operation)
                .status(STATUS_RESERVED)
                .build();

        try {
            repository.save(record);
            log.debug("Idempotency reserved: key={}", key);
            return true;
        } catch (DataIntegrityViolationException | DuplicateKeyException ex) {
            // Lost the race — another caller reserved first.
            log.debug("Idempotency race lost: key={}", key);
            return false;
        }
    }

    @Override
    public void markSuccess(String jobName, String entityType, String entityId) {
        updateStatus(jobName, entityType, entityId, STATUS_SUCCESS, null);
    }

    @Override
    public void markFailed(String jobName, String entityType, String entityId, String detail) {
        updateStatus(jobName, entityType, entityId, STATUS_FAILED, detail);
    }

    @Override
    public void markSkipped(String jobName, String entityType, String entityId, String detail) {
        updateStatus(jobName, entityType, entityId, STATUS_SKIPPED, detail);
    }

    @Override
    public boolean hasAttempted(String jobName, String entityType, String entityId) {
        String key = computeKey(jobName, entityType, entityId);
        return repository.existsByOperationKey(key);
    }

    private void updateStatus(String jobName, String entityType, String entityId,
                              String newStatus, String detail) {
        String key = computeKey(jobName, entityType, entityId);
        try {
            repository.findByOperationKey(key).ifPresent(record -> {
                record.setStatus(newStatus);
                record.setDetail(detail);
                record.setCreatedAt(LocalDateTime.now());
                repository.save(record);
                log.debug("Idempotency status updated: key={} status={}", key, newStatus);
            });
        } catch (RuntimeException ex) {
            log.warn("Failed to update idempotency status: key={} status={} error={}",
                    key, newStatus, ex.getMessage());
        }
    }
}
