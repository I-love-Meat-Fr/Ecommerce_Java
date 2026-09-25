package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.SchedulerIdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchedulerIdempotencyRepository extends MongoRepository<SchedulerIdempotencyRecord, String> {

    /**
     * Find an idempotency record by its stable operation key.
     * If present, the operation has already been applied (or attempted).
     */
    Optional<SchedulerIdempotencyRecord> findByOperationKey(String operationKey);

    /**
     * Find all records for a given job name — useful for cleanup/diagnostics.
     */
    List<SchedulerIdempotencyRecord> findByJobName(String jobName);

    /**
     * Check whether a record exists for the given operation key.
     */
    boolean existsByOperationKey(String operationKey);
}
