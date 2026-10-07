package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.CheckoutIdempotencyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository cho {@link CheckoutIdempotencyRecord}.
 *
 * <p>Compound unique index {@code (userId, idempotencyKey)} ở document level
 * là "gate" chính cho idempotency — race-condition giữa 2 request đồng thời
 * được Mongo xử lý bằng cách reject 1 INSERT với
 * {@link com.mongodb.DuplicateKeyException}. Service layer dùng exception đó
 * để tra lại record hiện có và trả về Order đã tạo (idempotent replay).</p>
 */
@Repository
public interface CheckoutIdempotencyRepository
        extends MongoRepository<CheckoutIdempotencyRecord, String> {

    /**
     * Tra cứu 1 record theo cặp (userId, idempotencyKey).
     * Trả về Optional.empty nếu chưa có (lần đầu với key này) — caller sẽ
     * gọi {@code tryReserve}.
     */
    Optional<CheckoutIdempotencyRecord> findByUserIdAndIdempotencyKey(
            String userId, String idempotencyKey);

    /**
     * Check nhanh để debug / logging — KHÔNG dùng thay cho tryReserve vì
     * check-then-insert không atomic.
     */
    boolean existsByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}