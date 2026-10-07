package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Idempotency record cho POST /checkout.
 *
 * <p>Mục tiêu: ngăn cùng một yêu cầu Checkout bị xử lý 2 lần, sinh ra 2 Order
 * trùng nhau. Áp dụng cho cả 2 kịch bản:</p>
 *
 * <ul>
 *   <li><b>Client double-click / double-submit</b>: user bấm "Đặt hàng" 2 lần
 *       trong khoảng thời gian ngắn, hoặc browser tự retry form sau khi timeout.</li>
 *   <li><b>Network retry</b>: response bị mất trên đường truyền, client gửi lại
 *       request giống hệt → server không được tạo Order thứ 2.</li>
 * </ul>
 *
 * <h3>Idempotency Key Strategy</h3>
 * <p>Client (JS trong {@code checkout.html}) generate một UUID khi user mở trang
 * Checkout, lưu vào {@code sessionStorage} và submit cùng form. Server dùng
 * cặp {@code (userId, idempotencyKey)} làm khóa duy nhất nhờ compound unique
 * index — đảm bảo race-condition giữa 2 request đồng thời được DB xử lý.</p>
 *
 * <p>Nếu client không gửi key (vd: JS disabled, curl test, backend-internal call),
 * server vẫn fallback sang {@code userId + cartSnapshotHash + minuteBucket} để
 * phát hiện double-submit trong cùng phút — defense-in-depth.</p>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   PENDING  ──(success)──►  COMPLETED (giữ orderId)
 *      │
 *      └──────(error)────►  FAILED  (giữ errorMessage, có thể retry với key mới)
 * </pre>
 *
 * <p>Record tự động bị Mongo xóa sau khi {@code expiresAt} trôi qua (TTL index).
 * Mặc định TTL = 24h đủ để:</p>
 * <ul>
 *   <li>User retry do network trong vài phút đầu</li>
 *   <li>User check lại Order trong ngày</li>
 *   <li>Admin debug các case tranh chấp thanh toán</li>
 * </ul>
 *
 * <h3>Race Safety</h3>
 * <p>Compound unique index trên {@code (userId, idempotencyKey)} đảm bảo chỉ một
 * INSERT thành công khi 2 request đến đồng thời với cùng key. Request thứ 2 nhận
 * {@link com.mongodb.DuplicateKeyException} → service layer tra lại record hiện
 * có và trả về Order đã tạo (idempotent replay) hoặc 409 Conflict.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "checkout_idempotency")
@CompoundIndexes({
    // Race-condition guard: cùng (user, key) chỉ được insert 1 lần.
    @CompoundIndex(
            name = "uniq_user_key",
            def = "{'userId': 1, 'idempotencyKey': 1}",
            unique = true),
    // Tra cứu các record đang PENDING để dọn dẹp / monitor.
    @CompoundIndex(name = "status_created_idx", def = "{'status': 1, 'createdAt': 1}"),
    // TTL — Mongo sẽ xóa doc khi expiresAt < now.
    // Tên index "checkout_expires_idx" trùng với index đã có sẵn trong collection
    // (sinh ra từ version trước) để tránh IndexOptionsConflict (code 85) khi
    // auto-index-creation=true. Nếu môi trường nào chưa có index này, MongoDB sẽ
    // tự tạo mới với cùng tên.
    @CompoundIndex(name = "checkout_expires_idx", def = "{'expiresAt': 1}")
})
public class CheckoutIdempotencyRecord {

    @Id
    private String id;

    /** Idempotency key do client cung cấp (UUID) hoặc do server fallback sinh ra. */
    @Indexed
    private String idempotencyKey;

    /** Chủ sở hữu — để scope key theo user, tránh leak giữa các user. */
    @Indexed
    private String userId;

    /** Trạng thái: PENDING / COMPLETED / FAILED. */
    @Indexed
    private String status;

    /**
     * Order ID đã tạo thành công kèm theo key này. Null khi status != COMPLETED.
     * Dùng để trả về cho client trong lần replay (cùng key, lần 2).
     */
    private String orderId;

    /**
     * SHA-256 hex hash của request body đã chuẩn hoá (sau khi loại bỏ field
     * không ảnh hưởng semantics như discount).
     *
     * <p>Nếu client gửi cùng key nhưng body khác → coi là lỗi nghiêm trọng,
     * trả về 422 (key reuse with different body).</p>
     */
    private String requestHash;

    /** Thông báo lỗi rút gọn khi status = FAILED — phục vụ debug + hiển thị. */
    private String errorMessage;

    /** Endpoint generate record (mặc định: "POST /checkout"). */
    private String endpoint;

    /** User-Agent / IP — phục vụ audit khi có tranh chấp. */
    private String clientHint;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Thời điểm hoàn tất (status = COMPLETED). Null nếu chưa xong. */
    private LocalDateTime completedAt;

    /**
     * Thời điểm hết hạn — TTL index (compound name {@code checkout_expires_idx}) sẽ
     * tự động xóa document sau thời điểm này. Mặc định = createdAt + 24h (xem
     * {@code CheckoutIdempotencyServiceImpl}).
     *
     * <p>KHÔNG gắn {@code @Indexed} ở đây — index TTL đã được khai báo qua
     * {@code @CompoundIndex} trên class, tránh MongoDB tạo 2 index trùng key
     * {@code expiresAt} với tên khác nhau (gây IndexOptionsConflict code 85).</p>
     */
    private LocalDateTime expiresAt;
}