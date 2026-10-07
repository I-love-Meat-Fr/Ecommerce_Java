package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TASK #Tồn kho — Implementation của {@link InventoryService} dùng MongoDB atomic operators.
 *
 * <p><b>Cơ chế atomic:</b></p>
 *
 * <p>Mỗi lần giảm stock, ta gọi {@link MongoTemplate#findAndModify(Query, Update, FindAndModifyOptions, Class)}
 * với:</p>
 * <ul>
 *   <li><b>Query (CAS filter):</b> {@code _id = productId AND stock >= quantity}.
 *       Filter này chính là "compare-and-set" — nếu stock không đủ thì query không khớp và
 *       MongoDB KHÔNG thực hiện update.</li>
 *   <li><b>Update:</b> {@code $inc: { stock: -quantity, version: 1 }}. MongoDB xử lý {@code $inc}
 *       atomic tại database level — không có 2 operation nào xen vào giữa lệnh này.</li>
 *   <li><b>ReturnNew:</b> {@code true} để lấy document SAU update → biết được stock mới.</li>
 * </ul>
 *
 * <p><b>Vì sao KHÔNG dùng {@code repository.save()}:</b></p>
 * <pre>
 *   // ❌ Pattern CŨ (race condition):
 *   Product p = repo.findById(id).get();       // T1: read stock=5
 *   p.setStock(p.getStock() - 3);             // Java: 5-3=2
 *   repo.save(p);                              // T2: write stock=2
 *   // → Concurrent buyer B cũng đọc 5, cũng tính 2, cũng ghi 2. Mất update.
 *
 *   // ✅ Pattern MỚI (atomic):
 *   findAndModify(
 *     where("_id").is(id).and("stock").gte(3),  // CAS filter
 *     Update.inc("stock", -3),                  // atomic $inc
 *     Product.class
 *   );
 *   // → MongoDB đảm bảo chỉ 1 trong 2 concurrent request thành công.
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final MongoTemplate mongoTemplate;

    @Override
    public DecrementResult tryDecrement(String productId, int quantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        // CAS filter: chỉ match nếu stock hiện tại >= quantity
        Query query = new Query(
                Criteria.where("_id").is(productId)
                        .and("stock").gte(quantity)
        );

        // Atomic decrement + bump version (để @Version optimistic locking tăng thêm 1 lớp bảo vệ)
        Update update = new Update()
                .inc("stock", -quantity)
                .inc("version", 1);

        // returnNew=true: trả về document SAU khi update → biết được stock mới
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

        Product updated = mongoTemplate.findAndModify(query, update, options, Product.class);

        if (updated != null) {
            // Update thành công — đã trừ stock vào DB
            log.debug("[Inventory] Decrement OK productId={} qty={} newStock={}",
                    productId, quantity, updated.getStock());
            return DecrementResult.success(productId, updated.getStock());
        }

        // Filter không khớp → có thể do: (a) product không tồn tại, hoặc (b) stock không đủ
        // Phân biệt 2 trường hợp bằng cách đọc lại product (best-effort, không dùng để ghi)
        Product current = mongoTemplate.findById(productId, Product.class);
        if (current == null) {
            log.debug("[Inventory] Decrement FAIL productId={} reason=NOT_FOUND", productId);
            return DecrementResult.notFound(productId);
        }
        log.debug("[Inventory] Decrement FAIL productId={} reason=INSUFFICIENT_STOCK current={} requested={}",
                productId, current.getStock(), quantity);
        return DecrementResult.insufficient(productId, current.getStock(), quantity);
    }

    @Override
    public IncrementResult increment(String productId, int quantity) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException("productId must not be blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be > 0");
        }

        // Tăng stock: unconditional (không cần filter) vì tăng stock luôn safe
        // Vẫn dùng findAndModify để biết được stock mới + bump version
        Query query = new Query(Criteria.where("_id").is(productId));
        Update update = new Update()
                .inc("stock", quantity)
                .inc("version", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

        Product updated = mongoTemplate.findAndModify(query, update, options, Product.class);

        if (updated != null) {
            log.debug("[Inventory] Increment OK productId={} qty={} newStock={}",
                    productId, quantity, updated.getStock());
            return IncrementResult.success(productId, updated.getStock());
        }

        log.warn("[Inventory] Increment FAIL productId={} reason=NOT_FOUND", productId);
        return IncrementResult.notFound(productId);
    }

    @Override
    public BatchDecrementResult tryDecrementBatch(List<DecrementRequest> items) {
        if (items == null || items.isEmpty()) {
            return BatchDecrementResult.success(0);
        }

        // Track các item đã giảm thành công để compensation nếu batch fail
        java.util.ArrayList<DecrementRequest> completed = new java.util.ArrayList<>();

        for (DecrementRequest item : items) {
            DecrementResult result = tryDecrement(item.productId(), item.quantity());

            if (result instanceof DecrementResult.Success) {
                completed.add(item);
            } else if (result instanceof DecrementResult.Insufficient insufficient) {
                // Compensation: rollback tất cả items đã giảm trước đó
                compensate(completed, "INSUFFICIENT_STOCK:" + insufficient.productId());
                return BatchDecrementResult.failure(
                        "INSUFFICIENT_STOCK",
                        insufficient.productId());
            } else if (result instanceof DecrementResult.NotFound notFound) {
                compensate(completed, "PRODUCT_NOT_FOUND:" + notFound.productId());
                return BatchDecrementResult.failure(
                        "PRODUCT_NOT_FOUND",
                        notFound.productId());
            }
        }

        log.debug("[Inventory] Batch decrement OK totalItems={}", items.size());
        return BatchDecrementResult.success(items.size());
    }

    /**
     * Rollback các decrement đã thực hiện trước đó khi batch fail.
     *
     * <p>Lưu ý: compensation dùng {@link #increment} (unconditional $inc) nên an toàn với
     * race condition (cộng dồn là idempotent nếu cùng 1 batch). Tuy nhiên, nếu có request
     * khác cùng lúc tăng stock (vd: order khác bị cancel), số lượng cuối cùng vẫn đúng vì
     * cả 2 đều là $inc.</p>
     */
    private void compensate(List<DecrementRequest> completed, String reason) {
        if (completed.isEmpty()) {
            return;
        }
        log.warn("[Inventory] Batch FAIL — compensating {} items. reason={}",
                completed.size(), reason);
        for (DecrementRequest done : completed) {
            try {
                IncrementResult r = increment(done.productId(), done.quantity());
                if (r instanceof IncrementResult.NotFound) {
                    // Edge case: product bị xóa giữa lúc decrement và increment
                    // Log error nhưng vẫn tiếp tục các item khác
                    log.error("[Inventory] Compensation FAIL productId={} reason=NOT_FOUND",
                            done.productId());
                }
            } catch (Exception ex) {
                // KHÔNG throw — best-effort. Log lỗi để job dọn dẹp xử lý sau.
                log.error("[Inventory] Compensation EXCEPTION productId={} qty={} ex={}",
                        done.productId(), done.quantity(), ex.toString(), ex);
            }
        }
    }
}