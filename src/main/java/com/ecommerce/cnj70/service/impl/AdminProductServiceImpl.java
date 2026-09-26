package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Phase 14 — Admin Product Moderation Service.
 *
 * Phạm vi:
 *   - Admin xem toàn bộ Product (cross-vendor).
 *   - Search theo name + Filter theo status.
 *   - Hide / Unhide = chuyển status ACTIVE ↔ HIDDEN.
 *   - Delete Violation = hard delete.
 *
 * KHÔNG sửa:
 *   - Vendor Product CRUD (ProductServiceImpl giữ nguyên).
 *   - Customer Product workflow.
 *   - Order/Review/Cart reference (LOCK 7).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProductServiceImpl implements AdminProductService {

    private final ProductRepository productRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Product> listProducts(Pageable pageable, String q, ProductStatus statusFilter) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasStatus = (statusFilter != null);

        // CASE 1: không filter gì -> trả tất cả có phân trang
        if (!hasQ && !hasStatus) {
            Query qAll = new Query().with(pageable);
            long total = mongoTemplate.count(new Query(), Product.class);
            List<Product> content = mongoTemplate.find(qAll, Product.class);
            return new PageImpl<>(content, pageable, total);
        }

        // CASE 2: chỉ search theo name (Phase 14 Search)
        if (hasQ && !hasStatus) {
            String trimmed = q.trim();
            Pattern regex = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
            Query q2 = new Query(Criteria.where("name").regex(regex)).with(pageable);
            long total = mongoTemplate.count(
                    Query.of(q2).limit(-1).skip(-1), Product.class);
            List<Product> content = mongoTemplate.find(q2, Product.class);
            return new PageImpl<>(content, pageable, total);
        }

        // CASE 3: chỉ filter theo status
        if (!hasQ && hasStatus) {
            Query q3 = new Query(Criteria.where("status").is(statusFilter)).with(pageable);
            long total = mongoTemplate.count(
                    Query.of(q3).limit(-1).skip(-1), Product.class);
            List<Product> content = mongoTemplate.find(q3, Product.class);
            return new PageImpl<>(content, pageable, total);
        }

        // CASE 4: search + filter
        String trimmed = q.trim();
        Pattern regex = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
        Query q4 = new Query(
                Criteria.where("name").regex(regex)
                        .and("status").is(statusFilter)
        ).with(pageable);
        long total = mongoTemplate.count(
                Query.of(q4).limit(-1).skip(-1), Product.class);
        List<Product> content = mongoTemplate.find(q4, Product.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Product getProductById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BadRequestException("ID sản phẩm không hợp lệ");
        }
        // Phase 4 + Phase 3 fix — Hỗ trợ CẢ 2 kiểu _id (String lẫn ObjectId).
        // KHÔNG ép raw.put("_id", id) vì sẽ làm save() insert document mới
        // khi DB lưu _id là ObjectId → duplicate key error (E11000).
        org.bson.Document raw = mongoTemplate.getCollection("products")
                .find(new org.bson.Document("_id", id))
                .first();
        if (raw == null && id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            org.bson.types.ObjectId oid = new org.bson.types.ObjectId(id);
            raw = mongoTemplate.getCollection("products")
                    .find(new org.bson.Document("_id", oid))
                    .first();
        }
        if (raw == null) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id);
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", Product.class.getName());
        }
        Product product = mongoTemplate.getConverter().read(Product.class, raw);
        if (product.getId() == null) {
            product.setId(id);
        }
        return product;
    }

    @Override
    @Transactional
    public Product hideProduct(String id) {
        Product product = getProductById(id);

        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new BadRequestException(
                    "Sản phẩm \"" + product.getName() + "\" đã ở trạng thái Ẩn");
        }

        // Phase 3 critical fix: atomic update trực tiếp trên collection (bypass
        // entity mapping) để tránh E11000 duplicate key khi DB lưu _id là ObjectId.
        Object nativeId = resolveIdForQuery(id, product);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("status", ProductStatus.HIDDEN.name())
                        .append("updatedAt", java.util.Date.from(java.time.LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("products")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        if (matched == 0) {
            // Fallback to entity save (defensive)
            log.warn("AdminProductService.hideProduct: atomic update not matched, falling back");
            product.setStatus(ProductStatus.HIDDEN);
            Product saved = productRepository.save(product);
            log.info("AdminProductService.hideProduct: id={} name={} -> HIDDEN",
                    saved.getId(), saved.getName());
            return saved;
        }
        // Reload to return latest state
        Product saved = getProductById(id);
        log.info("AdminProductService.hideProduct: id={} name={} -> HIDDEN",
                saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public Product unhideProduct(String id) {
        Product product = getProductById(id);

        if (product.getStatus() != ProductStatus.HIDDEN) {
            throw new BadRequestException(
                    "Sản phẩm \"" + product.getName() + "\" hiện không ở trạng thái Ẩn");
        }

        // Phase 3 critical fix: atomic update (xem hideProduct).
        Object nativeId = resolveIdForQuery(id, product);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("status", ProductStatus.ACTIVE.name())
                        .append("updatedAt", java.util.Date.from(java.time.LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("products")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        if (matched == 0) {
            log.warn("AdminProductService.unhideProduct: atomic update not matched, falling back");
            product.setStatus(ProductStatus.ACTIVE);
            Product saved = productRepository.save(product);
            log.info("AdminProductService.unhideProduct: id={} name={} -> ACTIVE",
                    saved.getId(), saved.getName());
            return saved;
        }
        Product saved = getProductById(id);
        log.info("AdminProductService.unhideProduct: id={} name={} -> ACTIVE",
                saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        Product product = getProductById(id);

        // Phase 3 critical fix: dùng native _id (ObjectId hoặc String) để delete,
        // tránh silent miss khi DB lưu _id là ObjectId.
        Object nativeId = resolveIdForQuery(id, product);
        org.bson.Document query = new org.bson.Document("_id", nativeId);
        long deleted = mongoTemplate.getCollection("products")
                .deleteOne(query).getDeletedCount();
        if (deleted == 0) {
            log.warn("AdminProductService.deleteProduct: atomic delete not matched, falling back to repo");
            productRepository.deleteById(product.getId());
        }
        log.info("AdminProductService.deleteProduct: removed id={} name={}",
                product.getId(), product.getName());
    }

    /**
     * Phase 3 critical fix: trả về _id dạng native (ObjectId hoặc String) để
     * Mongo query match đúng document trong DB, tránh E11000 duplicate key
     * do save() insert document mới khi entity._id là String hex.
     */
    private Object resolveIdForQuery(String id, Product product) {
        if (id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            org.bson.Document raw = mongoTemplate.getCollection("products")
                    .find(new org.bson.Document("_id", new org.bson.types.ObjectId(id)))
                    .first();
            if (raw != null) {
                return new org.bson.types.ObjectId(id);
            }
        }
        return id;
    }
}
