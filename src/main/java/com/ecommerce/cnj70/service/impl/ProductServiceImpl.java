package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ProductSpecification;
import com.ecommerce.cnj70.document.ProductVariant;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.dto.moderation.AutoModerationResult;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.AutoModerationStatus;
import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.AutoModerationResultProvider;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModerationHistoryRepository historyRepository;
    private final AutoModerationResultProvider autoModerationResultProvider;
    private final AuditEventWriter auditEventWriter;

    @Override
    @Transactional
    public Product createProduct(ProductFormReq request, String shopId, String shopName) {
        if (request.getStock() < 0) {
            throw new BadRequestException("Số lượng tồn kho không được âm");
        }

        String categoryName = null;
        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại"));
            categoryName = category.getName();
        }

        List<ProductSpecification> specs = sanitizeSpecifications(request.getSpecifications());
        List<ProductVariant> variants = sanitizeVariants(request.getVariants());

        Product product = Product.builder()
                .name(request.getName())
                .brand(request.getBrand())
                .warrantyMonths(request.getWarrantyMonths())
                .manufacturer(request.getManufacturer())
                .manufacturerAddress(request.getManufacturerAddress())
                .description(request.getDescription())
                .richDescription(request.getRichDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .categoryId(request.getCategoryId())
                .categoryName(categoryName)
                .imageUrls(request.getImageUrls())
                .specifications(specs)
                .variants(variants)
                .shopId(shopId)
                .shopName(shopName)
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                // Phase 4 — khi vendor tạo Product mới, Moderation status khởi tạo
                // là PENDING_AUTO (đang chờ Auto Moderation engine trả kết quả).
                // Khi engine có kết quả thì chuyển sang AUTO_PASSED / AUTO_REJECTED /
                // PENDING_MANUAL (nếu engine không phản hồi). Phase 2A cũ dùng
                // PENDING_MANUAL làm default; giờ phân biệt rõ hai trạng thái này
                // để moderator biết sản phẩm đã qua auto check hay chưa.
                .moderationStatus(ModerationStatus.PENDING_AUTO)
                .build();

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            product.setThumbnailUrl(product.getImageUrls().get(0));
        }

        Product saved = productRepository.save(product);

        // Phase 2 §13 + §57 — thử lấy Auto Moderation result (best-effort).
        // Nếu không có real backend (NoopAutoModerationResultProvider) → Optional.empty()
        // và Product giữ PENDING_MANUAL. KHÔNG fake production data.
        try {
            Optional<AutoModerationResult> auto = autoModerationResultProvider.getResult(saved.getId());
            if (auto.isPresent()) {
                AutoModerationResult result = auto.get();
                ModerationStatus next = mapAutoStatus(result);
                saved.setModerationStatus(next);
                saved.setModerationReason(result.getReason());
                saved.setModerationAt(result.getCheckedAt() != null ? result.getCheckedAt() : LocalDateTime.now());
                saved = productRepository.save(saved);
            }
        } catch (RuntimeException ex) {
            log.warn("AutoModeration lookup failed for productId={}: {}", saved.getId(), ex.getMessage());
        }

        // Phase 2 §13 — emit History + Audit khi Product submit vào queue
        recordSubmissionHistory(saved);
        emitSubmissionAudit(saved);

        return saved;
    }

    /**
     * Map AutoModerationResult.status sang Product.moderationStatus.
     * AutoModerationStatus hiện có: AUTO_PASSED / AUTO_REJECTED / PENDING_MANUAL.
     * Các giá trị APPROVED/REJECTED của ModerationStatus không suy ra từ AutoModerationStatus
     * (Auto engine chỉ quyết định pass/reject/manual-review).
     */
    private static ModerationStatus mapAutoStatus(AutoModerationResult result) {
        if (result == null || result.getStatus() == null) {
            return ModerationStatus.PENDING_MANUAL;
        }
        AutoModerationStatus s = result.getStatus();
        switch (s) {
            case AUTO_PASSED:
                return ModerationStatus.AUTO_PASSED;
            case AUTO_REJECTED:
                return ModerationStatus.AUTO_REJECTED;
            case PENDING_MANUAL:
            default:
                return ModerationStatus.PENDING_MANUAL;
        }
    }

    @Override
    @Transactional
    public Product updateProduct(String id, ProductFormReq request) {
        Product product = getProductById(id);

        // Phase 2 §47 — Backend ownership guard (defense-in-depth).
        // VendorService.validateProductOwnership() được controller gọi trước,
        // nhưng service cũng enforce để phòng khi controller bypass (API trực tiếp,
        // internal call, batch job).
        enforceOwnership(product);

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName());
        }
        if (request.getBrand() != null) {
            product.setBrand(request.getBrand());
        }
        if (request.getWarrantyMonths() != null) {
            product.setWarrantyMonths(request.getWarrantyMonths());
        }
        if (request.getManufacturer() != null) {
            product.setManufacturer(request.getManufacturer());
        }
        if (request.getManufacturerAddress() != null) {
            product.setManufacturerAddress(request.getManufacturerAddress());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getRichDescription() != null) {
            product.setRichDescription(request.getRichDescription());
        }
        if (request.getPrice() != null) {
            if (request.getPrice().doubleValue() <= 0) {
                throw new BadRequestException("Giá sản phẩm phải lớn hơn 0");
            }
            product.setPrice(request.getPrice());
        }
        if (request.getStock() != null) {
            if (request.getStock() < 0) {
                throw new BadRequestException("Số lượng tồn kho không được âm");
            }
            product.setStock(request.getStock());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại"));
            product.setCategoryId(request.getCategoryId());
            product.setCategoryName(category.getName());
        }
        if (request.getImageUrls() != null) {
            product.setImageUrls(request.getImageUrls());
            if (!request.getImageUrls().isEmpty()) {
                product.setThumbnailUrl(request.getImageUrls().get(0));
            }
        }
        if (request.getSpecifications() != null) {
            product.setSpecifications(sanitizeSpecifications(request.getSpecifications()));
        }
        if (request.getVariants() != null) {
            product.setVariants(sanitizeVariants(request.getVariants()));
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }

        // Phase 2 §13 (Resubmit flow) — Khi vendor sửa Product đang REJECTED,
        // đưa vào lại Moderation queue. APPROVED/ESCALATED giữ nguyên (không tự ý chuyển).
        boolean shouldResubmit = product.getModerationStatus() == ModerationStatus.REJECTED;
        if (shouldResubmit) {
            product.setModerationStatus(ModerationStatus.PENDING_MANUAL);
            product.setModerationReason(null);
            product.setModerationAt(null);
            product.setModerationActorId(null);
            log.info("Product resubmitted to moderator queue: productId={}", product.getId());
        }

        return productRepository.save(product);
    }

    /**
     * Phase 2 §13 — Ghi ModerationHistory khi Product submit vào queue lần đầu.
     */
    private void recordSubmissionHistory(Product product) {
        try {
            ModerationHistory history = ModerationHistory.builder()
                    .resourceType("PRODUCT")
                    .resourceId(product.getId())
                    .resourceName(product.getName())
                    .action(ModerationAction.APPROVE) // marker = "submission" trước khi có action enum riêng
                    .beforeStatus(null)
                    .afterStatus(ModerationStatus.PENDING_MANUAL)
                    .reason("Vendor tạo Product mới")
                    .moderatorRole(UserRole.VENDOR)
                    .moderatorEmail(product.getShopId())
                    .build();
            historyRepository.save(history);
        } catch (RuntimeException ex) {
            log.warn("Failed to record submission history for productId={}: {}",
                    product.getId(), ex.getMessage());
        }
    }

    /**
     * Phase 2 §13 — Emit AuditEvent khi Product được submit vào Moderation queue.
     */
    private void emitSubmissionAudit(Product product) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorId(product.getShopId())
                    .actorEmail(product.getShopId())
                    .role(UserRole.VENDOR)
                    .action("PRODUCT_SUBMIT")
                    .resourceType("PRODUCT")
                    .resourceId(product.getId())
                    .before("DRAFT")
                    .after(ModerationStatus.PENDING_MANUAL.name())
                    .reason("Vendor submit Product vào Moderation queue")
                    .createdAt(LocalDateTime.now())
                    .build();
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.warn("Failed to emit submission audit for productId={}: {}",
                    product.getId(), ex.getMessage());
        }
    }

    private List<ProductSpecification> sanitizeSpecifications(List<ProductSpecification> specs) {
        if (specs == null) {
            return new ArrayList<>();
        }
        List<ProductSpecification> cleaned = new ArrayList<>();
        for (ProductSpecification spec : specs) {
            if (spec == null) {
                continue;
            }
            String name = spec.getName() == null ? "" : spec.getName().trim();
            String value = spec.getValue() == null ? "" : spec.getValue().trim();
            String unit = spec.getUnit() == null ? "" : spec.getUnit().trim();
            if (name.isEmpty() && value.isEmpty() && unit.isEmpty()) {
                continue;
            }
            cleaned.add(ProductSpecification.builder()
                    .name(name)
                    .value(value)
                    .unit(unit)
                    .build());
        }
        return cleaned;
    }

    private List<ProductVariant> sanitizeVariants(List<ProductVariant> variants) {
        if (variants == null) {
            return new ArrayList<>();
        }
        List<ProductVariant> cleaned = new ArrayList<>();
        for (ProductVariant variant : variants) {
            if (variant == null) {
                continue;
            }
            BigDecimal price = variant.getPrice();
            if (price == null || price.doubleValue() <= 0) {
                continue;
            }
            List<ProductSpecification> specs = sanitizeSpecifications(variant.getSpecifications());
            cleaned.add(ProductVariant.builder()
                    .specifications(specs)
                    .price(price)
                    .stock(Math.max(0, variant.getStock()))
                    .sku(variant.getSku())
                    .build());
        }
        return cleaned;
    }

    @Override
    @Transactional
    public Product resubmitProduct(String id) {
        Product product = getProductById(id);
        enforceOwnership(product);

        if (product.getModerationStatus() != ModerationStatus.REJECTED) {
            throw new BadRequestException(
                    "Chỉ sản phẩm đang ở trạng thái BỊ TỪ CHỐI mới có thể gửi lại. "
                            + "Hiện tại: " + product.getModerationStatus());
        }

        // Reset về PENDING_AUTO để Auto Moderation engine có thể chạy lại.
        // Khi không có real backend thì PENDING_AUTO vẫn được liệt kê trong
        // Moderator queue (xem QUEUE_STATUSES), nên Moderator vẫn xử lý được.
        product.setModerationStatus(ModerationStatus.PENDING_AUTO);
        product.setModerationReason(null);
        product.setModerationAt(null);
        product.setModerationActorId(null);

        Product saved = productRepository.save(product);

        // Best-effort: ghi audit + history để truy vết lịch sử resubmit
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorId(product.getShopId())
                    .actorEmail(product.getShopId())
                    .role(UserRole.VENDOR)
                    .action("PRODUCT_RESUBMIT")
                    .resourceType("PRODUCT")
                    .resourceId(saved.getId())
                    .before(ModerationStatus.REJECTED.name())
                    .after(ModerationStatus.PENDING_AUTO.name())
                    .reason("Vendor gửi lại sản phẩm đã bị từ chối")
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.warn("Failed to emit resubmit audit for productId={}: {}",
                    saved.getId(), ex.getMessage());
        }

        log.info("Product resubmitted by vendor: productId={} shopId={}",
                saved.getId(), saved.getShopId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteProduct(String id) {
        Product product = getProductById(id);

        // Phase 2 §47 — Backend ownership guard (defense-in-depth)
        enforceOwnership(product);

        if (product.getStatus() == ProductStatus.HIDDEN) {
            throw new BadRequestException("Sản phẩm đã bị xóa trước đó");
        }

        productRepository.deleteById(id);
    }

    /**
     * Phase 2 §47 — Backend ownership guard. Kiểm tra Product.shopId khớp với
     * Shop của user hiện tại. Nếu ADMIN/MODERATOR đăng nhập → bypass (theo
     * permission matrix Phase 2 §49). Nếu customer/anonymous → luôn DENY.
     *
     * <p>Không throw exception khi authentication không phải VENDOR — chỉ khi
     * VENDOR đang cố truy cập Product của shop khác (IDOR).</p>
     */
    private void enforceOwnership(Product product) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return;
        boolean isPrivileged = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                            || "ROLE_MODERATOR".equals(a.getAuthority()));
        if (isPrivileged) return;

        // Vendor path: chỉ sửa Product của chính shop mình
        String currentShopId = resolveCurrentShopId(auth);
        if (currentShopId == null) {
            throw new UnauthorizedException(
                    "Vui lòng đăng nhập với tài khoản Vendor để thao tác Product");
        }
        if (!currentShopId.equals(product.getShopId())) {
            throw new UnauthorizedException(
                    "Bạn không có quyền thao tác sản phẩm của Shop khác");
        }
    }

    private String resolveCurrentShopId(org.springframework.security.core.Authentication auth) {
        Object principal = auth.getPrincipal();
        if (principal instanceof com.ecommerce.cnj70.security.CustomUserDetails cud) {
            return cud.getShopId();
        }
        return null;
    }

    @Override
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
    }

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Override
    public List<Product> getProductsByShop(String shopId) {
        return productRepository.findByShopId(shopId);
    }

    @Override
    public List<Product> getProductsByCategory(String categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Override
    public Page<Product> getProductsByCategory(String categoryId, Pageable pageable) {
        return productRepository.findByCategoryId(categoryId, pageable);
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseAndStatus(keyword, ProductStatus.ACTIVE);
    }

    @Override
    public Page<Product> searchProducts(String keyword, Pageable pageable) {
        return productRepository.findByNameContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public List<Product> getActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE);
    }

    @Override
    public List<Product> getNewArrivals(int limit) {
        return productRepository.findTop10ByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE)
                .stream().limit(limit).toList();
    }

    @Override
    public List<Product> getFeaturedProducts(int limit) {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .sorted((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()))
                .limit(limit)
                .toList();
    }

    @Override
    public void updateProductStatus(String id, ProductStatus status) {
        Product product = getProductById(id);
        product.setStatus(status);
        productRepository.save(product);
    }
}
