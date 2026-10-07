package com.ecommerce.cnj70.service;

import java.util.List;

/**
 * TASK #Tồn kho — Service layer cho việc cập nhật tồn kho an toàn dưới tải đồng thời.
 *
 * <p><b>Vấn đề mà interface này giải quyết:</b></p>
 *
 * <p>Pattern cũ trong {@code OrderServiceImpl.createOrder} là:</p>
 * <pre>
 *   product = findById(id)
 *   product.setStock(product.getStock() - qty)   // (1) read in Java
 *   productRepository.save(product)              // (2) save (overwrite)
 * </pre>
 *
 * <p>→ Hai request A &amp; B cùng đọc {@code stock=5} tại cùng thời điểm, cùng tính {@code 5-3=2},
 * cùng save {@code 2}. Kết quả: bán được 6 sản phẩm nhưng DB vẫn còn {@code 2}. Đây là classic
 * "lost update" race condition.</p>
 *
 * <p>{@code @Transactional} KHÔNG cứu được vì:</p>
 * <ul>
 *   <li>Spring Data MongoDB cần MongoDB replica set mới thực sự rollback được; Atlas của dự án
 *       là replica set nên transaction có hiệu lực, nhưng…</li>
 *   <li>…race condition xảy ra ngay giữa 2 round-trips {@code findById} → {@code save}, không
 *       phải giữa 2 transaction. Isolation {@code SERIALIZABLE} vẫn có thể cho phép 2 transaction
 *       cùng đọc {@code stock=5} và ghi đè lẫn nhau.</li>
 * </ul>
 *
 * <p><b>Giải pháp:</b> thay read-modify-save bằng <b>atomic conditional update</b> của MongoDB.</p>
 *
 * <p>Cú pháp MongoDB tương đương:</p>
 * <pre>
 *   db.products.updateOne(
 *     { _id: productId, stock: { $gte: qty } },   // CAS filter: chỉ update nếu đủ hàng
 *     { $inc: { stock: -qty } }                   // atomic decrement tại DB level
 *   )
 * </pre>
 *
 * <p>Nếu {@code stock} không đủ → filter không khớp → {@code modifiedCount = 0} → service trả
 * {@link DecrementResult#insufficient()} và KHÔNG thay đổi DB.</p>
 *
 * <p><b>Thread-safety guarantee:</b></p>
 * <ul>
 *   <li>Mọi thay đổi stock đều thông qua method trong interface này.</li>
 *   <li>KHÔNG BAO GIỜ đọc stock rồi tính new value ở Java code rồi save (tránh race).</li>
 *   <li>Sử dụng {@code MongoTemplate.findAndModify} để đảm bảo atomic tại database level.</li>
 * </ul>
 *
 * <p><b>Defense-in-depth:</b></p>
 * <ul>
 *   <li>Filter {@code stock: {$gte: qty}} chính là CAS (compare-and-set).</li>
 *   <li>Product có field {@code @Version} (optimistic locking) — Spring Data MongoDB tự check
 *       version, nếu có concurrent update sẽ throw {@code OptimisticLockingFailureException}.</li>
 *   <li>Có thể kết hợp với {@code @Transactional} để rollback các thay đổi khác (order, cart)
 *       khi một item fail.</li>
 * </ul>
 *
 * @see com.ecommerce.cnj70.service.impl.InventoryServiceImpl
 */
public interface InventoryService {

    /**
     * Cố gắng giảm stock của 1 product theo lượng {@code quantity} một cách atomic.
     *
     * <p><b>Quy tắc quan trọng:</b></p>
     * <ul>
     *   <li>Nếu product không tồn tại → trả {@link DecrementResult#notFound()}.</li>
     *   <li>Nếu stock hiện tại &lt; {@code quantity} → trả
     *       {@link DecrementResult#insufficient(currentStock)} (DB không bị thay đổi).</li>
     *   <li>Nếu đủ hàng → giảm stock nguyên tử tại DB level, trả
     *       {@link DecrementResult#success(newStock)}.</li>
     * </ul>
     *
     * <p>Method này KHÔNG throw exception cho trường hợp "không đủ hàng" — caller dựa vào
     * {@link DecrementResult} để xử lý (phù hợp với flow checkout cần hiển thị lỗi cho user).</p>
     *
     * @param productId ID sản phẩm cần giảm stock
     * @param quantity  số lượng cần giảm (phải &gt; 0)
     * @return kết quả thao tác (success / insufficient / notFound)
     */
    DecrementResult tryDecrement(String productId, int quantity);

    /**
     * Tăng stock của 1 product (unconditional) — dùng khi:
     *
     * <ul>
     *   <li>Order bị hủy → restore lại stock đã trừ.</li>
     *   <li>Vendor thêm hàng vào kho.</li>
     *   <li>Compensation khi 1 item trong batch giảm fail (rollback các item đã giảm thành công).</li>
     * </ul>
     *
     * <p>Không cần check điều kiện vì tăng stock là idempotent và luôn safe.</p>
     *
     * @param productId ID sản phẩm
     * @param quantity  số lượng cần tăng (phải &gt; 0)
     * @return {@link IncrementResult#success(int)} nếu thành công, {@link IncrementResult#notFound()}
     *         nếu product không tồn tại
     */
    IncrementResult increment(String productId, int quantity);

    /**
     * Batch decrement an toàn — dùng cho checkout có N sản phẩm.
     *
     * <p><b>Contract:</b></p>
     * <ul>
     *   <li>Nếu TẤT CẢ items giảm thành công → trả {@link BatchDecrementResult#success()} (DB
     *       đã được update cho cả N items).</li>
     *   <li>Nếu BẤT KỲ item nào fail (không đủ hàng / không tồn tại) → <b>rollback tất cả
     *       items đã giảm thành công trước đó</b> (compensation), rồi trả
     *       {@link BatchDecrementResult#failure(reason, failedProductId)}.</li>
     * </ul>
     *
     * <p>Method này đảm bảo: <b>DB luôn ở trạng thái consistent</b> — không có trường hợp "đã
     * trừ stock cho 2 sản phẩm đầu nhưng fail ở sản phẩm thứ 3 và 2 sản phẩm đầu bị mất
     * stock vĩnh viễn".</p>
     *
     * <p>Lưu ý quan trọng: compensation KHÔNG đảm bảo strict atomicity giữa các lệnh (vì nếu
     * MongoDB KHÔNG phải replica set, transaction bị bỏ qua). Tuy nhiên compensation vẫn đảm
     * bảo eventual consistency — nếu process chết giữa compensation, một job dọn dẹp có thể
     * tìm các Order chưa hoàn tất và restore stock (xem {@code abandoned_order_cleanup}).</p>
     *
     * @param items danh sách (productId, quantity) cần giảm stock
     * @return kết quả batch
     */
    BatchDecrementResult tryDecrementBatch(List<DecrementRequest> items);

    // ====================================================================
    // Result types — bất biến, dùng để truyền kết quả từ service lên caller
    // ====================================================================

    /**
     * Input cho {@link InventoryService#tryDecrementBatch(List)}.
     */
    record DecrementRequest(String productId, int quantity) {
        public DecrementRequest {
            if (productId == null || productId.isBlank()) {
                throw new IllegalArgumentException("productId must not be blank");
            }
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be > 0");
            }
        }
    }

    /**
     * Kết quả của {@link InventoryService#tryDecrement(String, int)}.
     */
    sealed interface DecrementResult {
        record Success(String productId, int newStock) implements DecrementResult {}
        record Insufficient(String productId, int currentStock, int requested) implements DecrementResult {}
        record NotFound(String productId) implements DecrementResult {}

        static DecrementResult success(String productId, int newStock) {
            return new Success(productId, newStock);
        }
        static DecrementResult insufficient(String productId, int currentStock, int requested) {
            return new Insufficient(productId, currentStock, requested);
        }
        static DecrementResult notFound(String productId) {
            return new NotFound(productId);
        }
    }

    /**
     * Kết quả của {@link InventoryService#increment(String, int)}.
     */
    sealed interface IncrementResult {
        record Success(String productId, int newStock) implements IncrementResult {}
        record NotFound(String productId) implements IncrementResult {}

        static IncrementResult success(String productId, int newStock) {
            return new Success(productId, newStock);
        }
        static IncrementResult notFound(String productId) {
            return new NotFound(productId);
        }
    }

    /**
     * Kết quả của {@link InventoryService#tryDecrementBatch(List)}.
     */
    sealed interface BatchDecrementResult {
        record Success(int totalDecremented) implements BatchDecrementResult {}
        record Failure(String reason, String failedProductId) implements BatchDecrementResult {}

        static BatchDecrementResult success(int totalDecremented) {
            return new Success(totalDecremented);
        }
        static BatchDecrementResult failure(String reason, String failedProductId) {
            return new Failure(reason, failedProductId);
        }
    }
}