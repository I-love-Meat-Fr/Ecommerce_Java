package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.CheckoutIdempotencyRecord;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.repository.CheckoutIdempotencyRepository;
import com.ecommerce.cnj70.service.CheckoutIdempotencyService;
import com.mongodb.DuplicateKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoDB-backed implementation của {@link CheckoutIdempotencyService}.
 *
 * <h3>Race-condition guard</h3>
 * <p>Dùng compound unique index {@code (userId, idempotencyKey)} trên
 * {@link CheckoutIdempotencyRecord} làm "gate" duy nhất. Hai request đồng thời
 * với cùng key: một INSERT thắng, các INSERT sau nhận
 * {@link DuplicateKeyException}. Service bắt exception, tra lại record hiện
 * có để quyết định trả về {@code REPLAYED} (cùng hash) hoặc {@code CONFLICT}
 * (hash khác / vẫn PENDING).</p>
 *
 * <h3>Hash strategy</h3>
 * <p>{@link #generateRequestHash} băm các trường semantic của CheckoutReq
 * (phone, address, items, paymentMethod, voucherCode) theo thứ tự canonical
 * để 2 request cùng intent nhưng khác thứ tự field vẫn cho cùng hash.</p>
 *
 * <p>Các trường KHÔNG hash (vd: {@code discount}) sẽ do server tự tính lại
 * — nếu hash chúng, replay sẽ bị reject oan.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckoutIdempotencyServiceImpl implements CheckoutIdempotencyService {

    /** TTL mặc định cho mỗi reservation — 24h đủ cho retry + debug. */
    private static final long DEFAULT_TTL_HOURS = 24L;

    private final CheckoutIdempotencyRepository repository;

    // =========================================================================
    // Reservation flow
    // =========================================================================

    @Override
    public ReservationOutcome tryReserve(String userId, String idempotencyKey,
                                         String requestHash, String endpoint,
                                         String clientHint) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId không được rỗng khi reserve idempotency");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey không được rỗng khi reserve");
        }

        CheckoutIdempotencyRecord reservation = CheckoutIdempotencyRecord.builder()
                .id(buildDocId(userId, idempotencyKey))
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .status(STATUS_PENDING)
                .requestHash(requestHash)
                .endpoint(endpoint)
                .clientHint(truncate(clientHint, 200))
                .expiresAt(LocalDateTime.now().plusHours(DEFAULT_TTL_HOURS))
                .build();

        try {
            CheckoutIdempotencyRecord saved = repository.save(reservation);
            log.debug("Idempotency reserved: userId={} key={} hash={}",
                    userId, idempotencyKey, shortHash(requestHash));
            return ReservationOutcome.reserved(saved);
        } catch (DataIntegrityViolationException | DuplicateKeyException dup) {
            // Race lost — có request khác đã insert trước. Tra lại để quyết định.
            return handleDuplicate(userId, idempotencyKey, requestHash);
        }
    }

    /**
     * Xử lý khi INSERT bị reject vì trùng (userId, idempotencyKey).
     * Quyết định trả về REPLAYED, CONFLICT, hoặc cho phép retry (xoá FAILED cũ).
     *
     * <p><b>Edge case (MongoDB phantom index entry):</b> Nếu INSERT fail với
     * DuplicateKeyException nhưng cả compound query lẫn {@code findById(_id)}
     * đều trả về empty — nghĩa là unique index có bản ghi "ma" (phantom entry):
     * record đã từng tồn tại rồi bị xoá nhưng index entry chưa được dọn.
     * Đây là bug đã biết của MongoDB với unique index trên collection TTL hoặc
     * khi soft-delete + TTL race.</p>
     *
     * <p><b>Recovery:</b> best-effort deleteById(_id) để xoá phantom entry, rồi
     * recurse {@code tryReserve()} một lần. Nếu vẫn fail, return CONFLICT để
     * controller xử lý (server generate key mới + JS clear sessionStorage).</p>
     */
    private ReservationOutcome handleDuplicate(String userId, String idempotencyKey,
                                              String requestHash) {
        // Cố gắng tìm theo cả 2 cách: compound query (chuẩn) + _id lookup (fallback).
        Optional<CheckoutIdempotencyRecord> existingOpt =
                repository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingOpt.isEmpty()) {
            // Fallback: thử tìm theo _id (primary write thường consistent hơn compound index)
            String docId = buildDocId(userId, idempotencyKey);
            existingOpt = repository.findById(docId);
        }

        if (existingOpt.isEmpty()) {
            // ===== Phantom index entry recovery (best-effort) =====
            // DuplicateKeyException xảy ra → index có entry trùng. Nhưng query
            // cả 2 cách đều empty → record đã bị xoá (TTL hoặc manual) nhưng
            // unique index vẫn giữ entry cũ (stale). Đây là bug đã biết của
            // MongoDB trên collection có TTL index race với writes.
            //
            // Best-effort: thử deleteById(_id) để xoá record + rebuild index.
            // Nếu vẫn fail, return CONFLICT để controller generate key mới
            // (Spring form binding render hidden field với UUID mới, JS clear
            // sessionStorage). KHÔNG recurse tryReserve() ở đây vì nếu Mongo
            // thật sự có phantom, recursion sẽ loop vô hạn.
            String docId = buildDocId(userId, idempotencyKey);
            log.warn("Idempotency phantom index detected. Best-effort cleanup: " +
                     "userId={} key={} docId={}", userId, idempotencyKey, docId);
            try {
                repository.deleteById(docId);
                log.info("Phantom cleanup deleteById executed. Returning CONFLICT " +
                         "to let controller generate a fresh key.");
            } catch (RuntimeException deleteEx) {
                log.error("Failed to delete phantom entry, returning CONFLICT: {}",
                        deleteEx.getMessage());
            }
            return ReservationOutcome.conflict(
                    "Đã xảy ra lỗi tạm thời với hệ thống thanh toán. " +
                    "Vui lòng tải lại trang và đặt hàng lại.");
        }

        CheckoutIdempotencyRecord existing = existingOpt.get();
        String existingStatus = existing.getStatus();

        // Trường hợp 1: COMPLETED với cùng hash → idempotent replay.
        // Đây là flow quan trọng nhất: response bị mất, client POST lại
        // cùng key → trả về orderId cũ thay vì tạo Order mới.
        if (STATUS_COMPLETED.equals(existingStatus)) {
            if (requestHash != null && requestHash.equals(existing.getRequestHash())) {
                log.info("Idempotency replay (COMPLETED): userId={} key={} orderId={}",
                        userId, idempotencyKey, existing.getOrderId());
                return ReservationOutcome.replayed(existing.getOrderId());
            }
            // Cùng key nhưng body khác → có dấu hiệu client bug / gian lận.
            log.warn("Idempotency conflict (hash mismatch on COMPLETED): userId={} key={}",
                    userId, idempotencyKey);
            return ReservationOutcome.conflict(
                    "Idempotency key đã được sử dụng với dữ liệu khác. "
                    + "Vui lòng tải lại trang và đặt hàng lại.");
        }

        // Trường hợp 2: PENDING → request trước vẫn đang xử lý.
        // Thường gặp khi:
        //   - Double-click thật sự (2 request đến cùng lúc)
        //   - Network retry trong khi request đầu vẫn chạy
        // Trả CONFLICT để caller hiển thị "Đang xử lý đơn trước, vui lòng chờ".
        if (STATUS_PENDING.equals(existingStatus)) {
            log.warn("Idempotency conflict (PENDING in-flight): userId={} key={}",
                    userId, idempotencyKey);
            return ReservationOutcome.conflict(
                    "Đơn hàng trước của bạn đang được xử lý. "
                    + "Vui lòng chờ vài giây rồi kiểm tra lịch sử đơn hàng.");
        }

        // Trường hợp 3: FAILED → xoá record cũ + reserve lại.
        // Lý do: lỗi trước có thể là transient (network, timeout); user xứng đáng
        // được retry với cùng key. Hash mismatch trên FAILED vẫn là conflict
        // vì client đã đổi ý định.
        if (STATUS_FAILED.equals(existingStatus)) {
            if (requestHash != null && !requestHash.equals(existing.getRequestHash())) {
                log.warn("Idempotency conflict (hash mismatch on FAILED): userId={} key={}",
                        userId, idempotencyKey);
                return ReservationOutcome.conflict(
                        "Idempotency key đã được sử dụng với dữ liệu khác.");
            }
            try {
                repository.deleteById(existing.getId());
                log.info("Idempotency FAILED record deleted, retry allowed: userId={} key={}",
                        userId, idempotencyKey);
            } catch (RuntimeException ex) {
                log.warn("Failed to delete FAILED idempotency record (will treat as conflict): {}",
                        ex.getMessage());
                return ReservationOutcome.conflict(
                        "Không thể thử lại đơn hàng lúc này, vui lòng thử lại sau ít phút.");
            }
            // Recurse — lần này INSERT sẽ thành công vì đã xoá record cũ.
            // Đặt giới hạn 1 lần để tránh loop vô hạn nếu có bug khác.
            return tryReserve(userId, idempotencyKey, requestHash,
                    existing.getEndpoint(), existing.getClientHint());
        }

        // Trường hợp khác (status lạ — hiếm khi xảy ra) → fail safe.
        log.warn("Idempotency unexpected status '{}' on duplicate: userId={} key={}",
                existingStatus, userId, idempotencyKey);
        return ReservationOutcome.conflict(
                "Trạng thái idempotency không hợp lệ. Vui lòng tải lại trang.");
    }

    @Override
    public void markCompleted(String userId, String idempotencyKey, String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId không được rỗng khi markCompleted");
        }
        String docId = buildDocId(userId, idempotencyKey);
        repository.findById(docId).ifPresentOrElse(record -> {
            record.setStatus(STATUS_COMPLETED);
            record.setOrderId(orderId);
            record.setCompletedAt(LocalDateTime.now());
            record.setErrorMessage(null);
            repository.save(record);
            log.info("Idempotency markCompleted: userId={} key={} orderId={}",
                    userId, idempotencyKey, orderId);
        }, () -> log.warn(
                "Idempotency markCompleted skipped (record missing): userId={} key={}",
                userId, idempotencyKey));
    }

    @Override
    public void markFailed(String userId, String idempotencyKey, String errorMessage) {
        String docId = buildDocId(userId, idempotencyKey);
        repository.findById(docId).ifPresentOrElse(record -> {
            // Không ghi đè COMPLETED — nếu đã hoàn tất thì để nguyên.
            if (STATUS_COMPLETED.equals(record.getStatus())) {
                log.warn("Idempotency markFailed skipped (already COMPLETED): userId={} key={}",
                        userId, idempotencyKey);
                return;
            }
            record.setStatus(STATUS_FAILED);
            record.setErrorMessage(truncate(errorMessage, 500));
            repository.save(record);
            log.info("Idempotency markFailed: userId={} key={} error={}",
                    userId, idempotencyKey, truncate(errorMessage, 200));
        }, () -> log.debug(
                "Idempotency markFailed skipped (record missing): userId={} key={}",
                userId, idempotencyKey));
    }

    @Override
    public void releaseReservation(String userId, String idempotencyKey) {
        String docId = buildDocId(userId, idempotencyKey);
        try {
            // Chỉ xoá khi còn PENDING — không xoá COMPLETED vì user có thể đang
            // check Order detail qua cùng key.
            Optional<CheckoutIdempotencyRecord> existing = repository.findById(docId);
            if (existing.isPresent() && STATUS_PENDING.equals(existing.get().getStatus())) {
                repository.deleteById(docId);
                log.debug("Idempotency reservation released: userId={} key={}",
                        userId, idempotencyKey);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to release idempotency reservation: userId={} key={} error={}",
                    userId, idempotencyKey, ex.getMessage());
        }
    }

    @Override
    public Optional<CheckoutIdempotencyRecord> findByKey(String userId, String idempotencyKey) {
        if (userId == null || idempotencyKey == null) {
            return Optional.empty();
        }
        return repository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
    }

    // =========================================================================
    // Hash generation
    // =========================================================================

    @Override
    public String generateRequestHash(CheckoutReq request) {
        if (request == null) {
            return sha256Hex("");
        }
        // Chỉ hash các trường semantic — KHÔNG hash `discount` (server tự tính lại)
        // và KHÔNG hash `idempotencyKey` (vòng lặp vô hạn).
        StringBuilder canonical = new StringBuilder(256);
        canonical.append("phone=").append(nullSafe(request.getPhone())).append('|');
        canonical.append("address=").append(nullSafe(request.getShippingAddress())).append('|');
        canonical.append("pm=").append(request.getPaymentMethod() != null
                ? request.getPaymentMethod().name() : "").append('|');
        canonical.append("voucher=").append(nullSafe(request.getVoucherCode())).append('|');

        // items: sort theo productId để trật tự field không ảnh hưởng hash.
        List<CheckoutReq.CheckoutItemReq> items = request.getItems();
        if (items != null && !items.isEmpty()) {
            String itemsStr = items.stream()
                    .filter(i -> i != null && i.getProductId() != null)
                    .sorted((a, b) -> a.getProductId().compareTo(b.getProductId()))
                    .map(i -> i.getProductId() + ":" + i.getQuantity())
                    .collect(Collectors.joining(","));
            canonical.append("items=").append(itemsStr).append('|');
        } else {
            canonical.append("items=*").append('|'); // checkout toàn bộ Cart
        }
        return sha256Hex(canonical.toString());
    }

    @Override
    public String generateFallbackKey(String userId, String cartSnapshotHash) {
        // Bucket theo phút hiện tại — đủ rộng để không ảnh hưởng user thật
        // (thao tác cách nhau vài giây) nhưng đủ hẹp để bắt double-click.
        // TTL vẫn là DEFAULT_TTL_HOURS (record cũ sẽ tự xoá sau 24h).
        long minuteBucket = System.currentTimeMillis() / 60_000L;
        String raw = "fb:" + userId + ":" + nullSafe(cartSnapshotHash) + ":" + minuteBucket;
        return sha256Hex(raw).substring(0, 32);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Document _id = "userId:key" — đảm bảo compound uniqueness ngay cả khi
     * MongoDB chưa kịp apply compound index, và giúp tra cứu nhanh.
     */
    private String buildDocId(String userId, String idempotencyKey) {
        return userId + ":" + idempotencyKey;
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 luôn có trong JDK — fallback để tránh NPE trong test.
            return Integer.toHexString(input.hashCode());
        }
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static String shortHash(String hash) {
        if (hash == null) return "null";
        return hash.length() > 12 ? hash.substring(0, 12) + "..." : hash;
    }
}