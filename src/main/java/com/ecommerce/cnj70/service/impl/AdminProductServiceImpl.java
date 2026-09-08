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
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy sản phẩm với ID: " + id));
    }

    @Override
    @Transactional
    public Product hideProduct(String id) {
        Product product = getProductById(id);

        // Validate theo Contract Phase 14:
        // Hide ≠ Delete: Document vẫn tồn tại, chỉ set status = HIDDEN.
        // Vendor vẫn quản lý được Product (xem theo Vendor ownership).

        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new BadRequestException(
                    "Sản phẩm \"" + product.getName() + "\" đã ở trạng thái Ẩn");
        }

        product.setStatus(ProductStatus.HIDDEN);
        Product saved = productRepository.save(product);
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

        // Sau Unhide -> ACTIVE. KHÔNG đổi các field khác.
        product.setStatus(ProductStatus.ACTIVE);
        Product saved = productRepository.save(product);
        log.info("AdminProductService.unhideProduct: id={} name={} -> ACTIVE",
                saved.getId(), saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        // TASK 14.11 — Delete Violation = hard delete theo Contract Phase 14.
        // KHÔNG cascade Order/Review/Cart (LOCK 7).
        // Order lịch sử giữ nguyên productId reference (chỉ là ID string).
        Product product = getProductById(id);

        productRepository.deleteById(product.getId());
        log.info("AdminProductService.deleteProduct: removed id={} name={}",
                product.getId(), product.getName());
    }
}
