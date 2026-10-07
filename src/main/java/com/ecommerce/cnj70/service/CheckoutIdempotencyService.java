package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.CheckoutIdempotencyRecord;

/**
 * Checkout Idempotency Service — chống tạo Order trùng cho POST /checkout.
 *
 * <p>Service này được {@code OrderServiceImpl.createOrder} wrap bên ngoài để
 * đảm bảo cùng một yêu cầu (cùng {@code userId + idempotencyKey}) chỉ sinh
 * ra tối đa một Order, kể cả khi:</p>
 *
 * <ul>
 *   <li>User double-click "Đặt hàng" (2 POST đến gần như đồng thời)</li>
 *   <li>Browser auto-retry sau network timeout</li>
 *   <li>Mobile mất sóng → gửi lại sau khi có sóng trở lại</li>
 *   <li>Server xử lý xong nhưng response bị rớt → client POST lại với cùng key</li>
 * </ul>
 *
 * <h3>Race-condition guard</h3>
 * <p>Dùng compound unique index {@code (userId, idempotencyKey)} ở MongoDB —
 * INSERT đầu tiên thắng, các INSERT sau nhận
 * {@link com.mongodb.DuplicateKeyException}. Service tra lại record hiện có
 * và trả về:</p>
 *
 * <ul>
 *   <li><b>COMPLETED + cùng requestHash</b> → trả về {@code orderId} cũ (idempotent replay)</li>
 *   <li><b>COMPLETED + requestHash khác</b> → throw Conflict (key reuse với body khác)</li>
 *   <li><b>PENDING</b> → throw Conflict (request trước vẫn đang xử lý)</li>
 *   <li><b>FAILED</b> → xoá record cũ + thử reserve lại (cho phép retry sau lỗi)</li>
 * </ul>
 *
 * <h3>TTL</h3>
 * <p>Record tự động bị Mongo xóa sau 24h (xem {@code expiresAt} + TTL index).
 * Quá đủ để user check lại Order và admin debug khi có tranh chấp thanh toán.</p>
 */
public interface CheckoutIdempotencyService {

    /** Status constants — dùng chung giữa service + impl để tránh typo. */
    String STATUS_PENDING = "PENDING";
    String STATUS_COMPLETED = "COMPLETED";
    String STATUS_FAILED = "FAILED";

    /**
     * Cố gắng reserve idempotency key cho (userId, idempotencyKey).
     *
     * @param userId         user hiện tại
     * @param idempotencyKey key do client cung cấp (UUID) — KHÔNG được rỗng
     * @param requestHash    SHA-256 hex hash của request body đã chuẩn hoá
     * @param endpoint       endpoint generate key (vd: "POST /checkout")
     * @param clientHint     user-agent / IP để audit
     * @return {@link ReservationOutcome} mô tả kết quả:
     *         <ul>
     *           <li>{@code RESERVED} — caller thắng race, có thể tiếp tục tạo Order</li>
     *           <li>{@code REPLAYED} — key đã COMPLETED với cùng requestHash, trả về orderId cũ</li>
     *           <li>{@code CONFLICT} — key đang PENDING hoặc hash mismatch, không thể tiếp tục</li>
     *         </ul>
     */
    ReservationOutcome tryReserve(String userId, String idempotencyKey, String requestHash,
                                 String endpoint, String clientHint);

    /**
     * Đánh dấu reservation hiện tại đã tạo Order thành công.
     * Ghi nhận {@code orderId} để các lần replay sau tra lại được.
     */
    void markCompleted(String userId, String idempotencyKey, String orderId);

    /**
     * Đánh dấu reservation thất bại — lưu errorMessage ngắn gọn để debug.
     * Record vẫn tồn tại tới khi TTL xóa, nhưng status=FAILED sẽ được
     * {@link #tryReserve} coi như "có thể retry" (xoá + insert lại).
     */
    void markFailed(String userId, String idempotencyKey, String errorMessage);

    /**
     * Xoá record PENDING khi caller muốn rollback chủ động
     * (vd: OrderService throw trước khi gọi markCompleted/markFailed).
     * Dùng trong finally block.
     */
    void releaseReservation(String userId, String idempotencyKey);

    /**
     * Tra cứu record theo (userId, idempotencyKey) — dùng cho debug / admin
     * endpoint. KHÔNG dùng trong flow bình thường (đã được tryReserve xử lý).
     */
    java.util.Optional<CheckoutIdempotencyRecord> findByKey(String userId, String idempotencyKey);

    /**
     * Sinh SHA-256 hex hash ổn định từ các trường semantic của {@code CheckoutReq}.
     *
     * <p>Loại bỏ các trường không ảnh hưởng business (vd: {@code discount} — server
     * tự tính lại) để 2 request cùng key nhưng khác discount vẫn được coi là
     * "cùng intent" và idempotent replay.</p>
     */
    String generateRequestHash(com.ecommerce.cnj70.dto.request.CheckoutReq request);

    /**
     * Fallback key khi client không gửi {@code Idempotency-Key} (vd: JS disabled,
     * backend-internal call, curl test). Hash từ (userId, cartSnapshot, phút hiện tại)
     * — đủ để bắt double-submit trong cùng phút mà vẫn không quá "bám" gây phiền.
     *
     * <p><b>Lưu ý</b>: nên ưu tiên client gửi key UUID để có TTL 24h chuẩn. Fallback
     * này chỉ là defense-in-depth.</p>
     */
    String generateFallbackKey(String userId, String cartSnapshotHash);

    /**
     * Kết quả của {@link #tryReserve} — dùng {@link Outcome} enum để caller
     * xử lý rõ ràng, không cần check null.
     */
    enum Outcome { RESERVED, REPLAYED, CONFLICT }

    /**
     * Holder trả về từ {@link #tryReserve}.
     *
     * <ul>
     *   <li>{@code outcome == RESERVED}: tiếp tục tạo Order. Có thể đọc
     *       {@link #getRecord()} để xem metadata.</li>
     *   <li>{@code outcome == REPLAYED}: idempotent replay, đã có Order rồi.
     *       {@link #getExistingOrderId()} chứa orderId cũ.</li>
     *   <li>{@code outcome == CONFLICT}: throw exception với message từ
     *       {@link #getConflictReason()}.</li>
     * </ul>
     */
    final class ReservationOutcome {
        private final Outcome outcome;
        private final CheckoutIdempotencyRecord record;
        private final String existingOrderId;
        private final String conflictReason;

        private ReservationOutcome(Outcome outcome,
                                   CheckoutIdempotencyRecord record,
                                   String existingOrderId,
                                   String conflictReason) {
            this.outcome = outcome;
            this.record = record;
            this.existingOrderId = existingOrderId;
            this.conflictReason = conflictReason;
        }

        public static ReservationOutcome reserved(CheckoutIdempotencyRecord record) {
            return new ReservationOutcome(Outcome.RESERVED, record, null, null);
        }

        public static ReservationOutcome replayed(String existingOrderId) {
            return new ReservationOutcome(Outcome.REPLAYED, null, existingOrderId, null);
        }

        public static ReservationOutcome conflict(String reason) {
            return new ReservationOutcome(Outcome.CONFLICT, null, null, reason);
        }

        public Outcome getOutcome() { return outcome; }
        public CheckoutIdempotencyRecord getRecord() { return record; }
        public String getExistingOrderId() { return existingOrderId; }
        public String getConflictReason() { return conflictReason; }

        public boolean isReserved() { return outcome == Outcome.RESERVED; }
        public boolean isReplayed() { return outcome == Outcome.REPLAYED; }
        public boolean isConflict()  { return outcome == Outcome.CONFLICT; }
    }
}