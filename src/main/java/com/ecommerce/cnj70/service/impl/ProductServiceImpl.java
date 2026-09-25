package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ProductSpecification;
import com.ecommerce.cnj70.document.ProductVariant;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.dto.moderation.AutoModerationResult;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
<<<<<<< HEAD
import com.ecommerce.cnj70.dto.request.ReportCaseCreateReq;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ReportTargetType;
=======
import com.ecommerce.cnj70.enums.AutoModerationStatus;
import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.UserRole;
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
<<<<<<< HEAD
import com.ecommerce.cnj70.service.AutoModerationService;
import com.ecommerce.cnj70.service.AuditLogService;
=======
import com.ecommerce.cnj70.service.AutoModerationResultProvider;
import com.ecommerce.cnj70.service.AuditEventWriter;
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.ReportCaseService;
import com.ecommerce.cnj70.service.automation.AutoModerationResult;
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
import java.util.HashMap;
import java.util.List;
<<<<<<< HEAD
import java.util.Map;
import java.util.Objects;
=======
import java.util.Optional;
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    /** C1 — field mà thay đổi sẽ trigger re-run Auto Moderation. */
    private static final String AUDIT_ACTOR_SYSTEM = "SYSTEM";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
<<<<<<< HEAD
    private final AutoModerationService autoModerationService;
    private final ReportCaseService reportCaseService;
    private final AuditLogService auditLogService;
=======
    private final ModerationHistoryRepository historyRepository;
    private final AutoModerationResultProvider autoModerationResultProvider;
    private final AuditEventWriter auditEventWriter;
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace

    @Override
    @Transactional
    public Product createProduct(ProductFormReq request, String shopId, String shopName) {
        if (request.getStock() != null && request.getStock() < 0) {
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
<<<<<<< HEAD
                // C1 — FORCE PENDING_AUTO. Vendor KHÔNG được bypass pipeline
                // bằng cách set thẳng ACTIVE qua request.getStatus().
                // Status được quyết định bởi AutoModerationService sau khi pipeline chạy.
                .status(ProductStatus.PENDING_AUTO)
=======
                .status(request.getStatus() != null ? request.getStatus() : ProductStatus.ACTIVE)
                // Phase 2 §13 — mọi Product mới phải vào Moderation queue.
                // Auto Moderation contract sẽ (khi wire vào real backend) nâng cấp
                // thành AUTO_PASSED / AUTO_REJECTED; nếu không có backend thật
                // thì giữ PENDING_MANUAL và Moderator xử lý thủ công.
                .moderationStatus(ModerationStatus.PENDING_MANUAL)
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
                .build();

        if (product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            product.setThumbnailUrl(product.getImageUrls().get(0));
        }

        Product saved = productRepository.save(product);

<<<<<<< HEAD
        log.info("[Product] Created product id={} name='{}' shopId={} — entering Auto Moderation Pipeline",
                saved.getId(), saved.getName(), saved.getShopId());

        // C1 — chạy pipeline + apply kết quả
        return runAutoModerationPipeline(saved);
=======
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
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
    }

    @Override
    @Transactional
    public Product updateProduct(String id, ProductFormReq request) {
        Product product = getProductById(id);

<<<<<<< HEAD
        // Capture trạng thái + các field "significant" TRƯỚC khi apply để
        // quyết định re-run pipeline.
        ProductStatus oldStatus = product.getStatus();
        String oldName = product.getName();
        String oldDescription = product.getDescription();
        BigDecimal oldPrice = product.getPrice();
        List<String> oldImageUrls = product.getImageUrls() == null
                ? null : new ArrayList<>(product.getImageUrls());
        String oldCategoryId = product.getCategoryId();

        // ===== Apply field updates (KHÔNG apply status — vendor bypass bị chặn) =====
=======
        // Phase 2 §47 — Backend ownership guard (defense-in-depth).
        // VendorService.validateProductOwnership() được controller gọi trước,
        // nhưng service cũng enforce để phòng khi controller bypass (API trực tiếp,
        // internal call, batch job).
        enforceOwnership(product);

>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
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
        // C1 — bỏ qua request.getStatus() hoàn toàn. Vendor không đổi status
        // qua edit form. Status chỉ thay đổi qua pipeline (re-run) hoặc qua admin/
        // moderator action. HIDDEN status vẫn giữ nguyên.

        // ===== Detect significant change =====
        boolean significantChange = !Objects.equals(oldName, product.getName())
                || !Objects.equals(oldDescription, product.getDescription())
                || bigDecimalChanged(oldPrice, product.getPrice())
                || imageUrlsChanged(oldImageUrls, product.getImageUrls())
                || !Objects.equals(oldCategoryId, product.getCategoryId());

        // Re-moderate nếu: content đổi + status hiện KHÔNG phải HIDDEN
        // (HIDDEN là admin-controlled, vendor update không tự động recover).
        boolean canReModerate = oldStatus != ProductStatus.HIDDEN;

        if (significantChange && canReModerate) {
            log.info("[Product] Update product id={} triggered by significant content change — re-running Auto Moderation Pipeline (was={})",
                    product.getId(), oldStatus);

            // Reset về PENDING_AUTO trước khi chạy pipeline để khớp vòng đời
            // C1 (mọi entry point phải vào pipeline qua PENDING_AUTO).
            product.setStatus(ProductStatus.PENDING_AUTO);
            product = productRepository.save(product);

            return runAutoModerationPipeline(product);
        }

<<<<<<< HEAD
        // Không thay đổi significant → chỉ save thông thường.
        return productRepository.save(product);
    }

    // ===== C1 helper: chạy pipeline + apply + audit + ReportCase (nếu cần) =====

    /**
     * Chạy Auto Moderation Pipeline trên Product hiện tại.
     * - Pipeline throw → ép về MANUAL_REVIEW với flag SYSTEM:ERROR (fail-safe).
     * - Pipeline return ACTIVE / REJECTED_AUTO → setStatus + save.
     * - Pipeline return MANUAL_REVIEW → setStatus + save + tạo ReportCase + audit log.
     *
     * KHÔNG tạo 2 trạng thái cuối khác nhau cho cùng Product (idempotent về
     * ReportCase nhờ ReportCaseService.createCase kiểm tra case PENDING tồn tại).
     */
    private Product runAutoModerationPipeline(Product product) {
        AutoModerationResult result;
        try {
            result = autoModerationService.runProductChecks(product);
        } catch (Exception ex) {
            log.error("[Product] AutoModeration pipeline threw for product id={}: {} — falling back to MANUAL_REVIEW",
                    product.getId(), ex.getMessage(), ex);
            result = AutoModerationResult.manualReview(
                    product,
                    new ArrayList<>(List.of("SYSTEM:ERROR")),
                    new ArrayList<>(List.of("Auto Moderation pipeline gặp lỗi hệ thống: " + ex.getMessage()))
            );
        }

        product.setStatus(result.getTargetStatus());
        Product finalized = productRepository.save(product);

        log.info("[Product] Auto Moderation pipeline result for id={} name='{}': {} (flags={})",
                finalized.getId(), finalized.getName(),
                result.getTargetStatus(),
                result.getAutoFlags());

        // MANUAL_REVIEW → tạo ReportCase cho Moderator queue.
        // Idempotent: ReportCaseService tự skip nếu đã có case PENDING cho target.
        if (result.isManualReview()) {
            try {
                createAutoReportCase(result);
            } catch (Exception ex) {
                log.error("[Product] createAutoReportCase failed for product id={}: {}",
                        finalized.getId(), ex.getMessage(), ex);
                // Không fail — product đã ở MANUAL_REVIEW. Moderator có thể pick up qua các kênh khác.
            }
        }

        // Audit log — dùng AuditAction đã có sẵn trong enum để tránh tạo mới.
        try {
            logAutoModerationOutcome(finalized, result);
        } catch (Exception ex) {
            log.warn("[Product] AuditLog failed for product id={} (non-fatal): {}",
                    finalized.getId(), ex.getMessage());
        }

        return finalized;
    }

    private void createAutoReportCase(AutoModerationResult result) {
        Product product = result.getProduct();
        String description = "Auto Moderation phát hiện dấu hiệu đáng ngờ:\n"
                + String.join("\n", result.getReasons());
        ReportCaseCreateReq req = ReportCaseCreateReq.builder()
                .targetType(ReportTargetType.PRODUCT)
                .targetId(product.getId())
                .reason("Auto Moderation flag")
                .description(description)
                .autoFlags(result.getAutoFlags())
                .source("AUTO")
                .priority(5)
                .build();
        // Note: ReportCaseServiceImpl.validateTargetExists() tự snapshot
        // targetName/productName vào ReportCase.targetSnapshot.
        reportCaseService.createCase(req, null, AUDIT_ACTOR_SYSTEM);
    }

    private void logAutoModerationOutcome(Product product, AutoModerationResult result) {
        AuditAction action = AuditAction.PRODUCT_AUTO_PASS;
        AuditSeverity severity = AuditSeverity.INFO;

        if (result.isAutoRejected()) {
            action = AuditAction.PRODUCT_AUTO_REJECT;
            severity = AuditSeverity.WARNING;
        } else if (result.isManualReview()) {
            action = AuditAction.PRODUCT_MANUAL_REVIEW;
            severity = AuditSeverity.WARNING;
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("targetStatus", result.getTargetStatus().name());
        metadata.put("autoFlags", result.getAutoFlags());
        metadata.put("reasons", result.getReasons());

        auditLogService.log(
                action,
                "PRODUCT",
                product.getId(),
                AUDIT_ACTOR_SYSTEM,
                AUDIT_ACTOR_SYSTEM,
                AUDIT_ACTOR_SYSTEM,
                severity,
                "Auto Moderation result for product '" + product.getName() + "': "
                        + result.getTargetStatus() + " - " + String.join("; ", result.getReasons()),
                metadata
        );
    }

    // ===== Significant-change detection helpers =====
    private boolean bigDecimalChanged(BigDecimal old, BigDecimal now) {
        if (old == null && now == null) return false;
        if (old == null || now == null) return true;
        return old.compareTo(now) != 0;
    }

    private boolean imageUrlsChanged(List<String> oldList, List<String> newList) {
        if (oldList == null && newList == null) return false;
        if (oldList == null || newList == null) return true;
        if (oldList.size() != newList.size()) return true;
        for (int i = 0; i < oldList.size(); i++) {
            if (!Objects.equals(oldList.get(i), newList.get(i))) return true;
        }
        return false;
=======
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
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
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
