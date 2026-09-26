package com.ecommerce.cnj70.integration;

import com.ecommerce.cnj70.document.AuditLog;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.AuditLogRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.repository.ViolationRepository;
import com.ecommerce.cnj70.service.AutoModerationService;
import com.ecommerce.cnj70.service.automation.AutoModerationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.Mockito.when;

/**
 * Test harness — mô phỏng state transition end-to-end qua repositories mock.
 *
 * <p>Không phải production code; chỉ dùng trong test. Tách riêng để giữ
 * test class gọn và tái sử dụng cho các test khác trong cùng package.
 */
class ModerationFlowHarness {

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ViolationRepository violationRepository;
    private final AuditLogRepository auditLogRepository;
    private final Supplier<AutoModerationService> autoModerationSupplier;

    ModerationFlowHarness(ShopRepository shopRepository, UserRepository userRepository,
                          ProductRepository productRepository,
                          ViolationRepository violationRepository,
                          AuditLogRepository auditLogRepository,
                          Supplier<AutoModerationService> autoModerationSupplier) {
        this.shopRepository = shopRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.violationRepository = violationRepository;
        this.auditLogRepository = auditLogRepository;
        this.autoModerationSupplier = autoModerationSupplier;
    }

    void setAutoModeration(AutoModerationService svc) {
        // Reset supplier — không dùng setter trực tiếp do Supplier là final.
        // Thay vào đó, test class tạo harness mới nếu cần đổi pipeline.
        // Method này giữ signature cho IDE autocomplete.
    }

    /**
     * Tạo User mẫu — vendor hoặc customer tùy role.
     */
    void seedUser(String userId, UserRole role) {
        User u = User.builder()
                .id(userId)
                .email(userId + "@test.com")
                .fullName("Test " + userId)
                .role(role)
                .status(AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        userRepository.save(u);
        when(userRepository.findById(userId)).thenReturn(Optional.of(u));
    }

    void seedUser(String userId) {
        seedUser(userId, UserRole.VENDOR);
    }

    /**
     * Tạo Shop mẫu với status + KYC status chỉ định.
     */
    void seedShop(String ownerId, String shopId, ShopStatus status, KycStatus kycStatus) {
        Shop shop = Shop.builder()
                .id(shopId)
                .ownerId(ownerId)
                .shopName("Shop " + shopId)
                .status(status)
                .kycStatus(kycStatus)
                .createdAt(LocalDateTime.now())
                .build();
        shopRepository.save(shop);
        when(shopRepository.findById(shopId)).thenReturn(Optional.of(shop));
    }

    /**
     * Tạo Product qua pipeline auto moderation.
     *
     * @return Product đã được save với status từ pipeline + moderationStatus
     *         tương ứng (ACTIVE / REJECTED_AUTO / MANUAL_REVIEW / HIDDEN).
     */
    Product createProduct(String shopId, ProductFormReq req) {
        Shop shop = shopRepository.findById(shopId).orElseThrow();
        User owner = userRepository.findById(shop.getOwnerId()).orElseThrow();

        // Build Product draft.
        Product product = Product.builder()
                .shopId(shop.getId())
                .shopName(shop.getShopName())
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO)
                .stock(req.getStock())
                .categoryId(req.getCategoryId())
                .imageUrls(req.getImageUrls())
                .status(ProductStatus.PENDING_AUTO) // ép theo spec C1
                .createdAt(LocalDateTime.now())
                .build();

        // Save trước (để có ID cho history query nếu cần).
        product = productRepository.save(product);

        // Chạy pipeline.
        AutoModerationResult result = autoModerationSupplier.get().runProductChecks(product);

        // Apply kết quả.
        ProductStatus target = result.getTargetStatus();
        product.setStatus(target);
        switch (target) {
            case ACTIVE -> {
                product.setModerationStatus(
                        com.ecommerce.cnj70.enums.ModerationStatus.APPROVED);
            }
            case REJECTED_AUTO -> {
                product.setModerationStatus(
                        com.ecommerce.cnj70.enums.ModerationStatus.AUTO_REJECTED);
            }
            case MANUAL_REVIEW -> {
                product.setModerationStatus(
                        com.ecommerce.cnj70.enums.ModerationStatus.PENDING_MANUAL);
            }
            default -> {
                product.setModerationStatus(
                        com.ecommerce.cnj70.enums.ModerationStatus.PENDING_MANUAL);
            }
        }
        return productRepository.save(product);
    }

    /**
     * Ghi Violation + đổi Shop status sang SUSPENDED (mô phỏng Admin enforcement).
     */
    void recordViolationAndSuspend(String shopId, Violation violation) {
        violation.setShopId(shopId);
        violation.setResolvedAt(null);
        violation.setResolvedBy(null);
        violationRepository.save(violation);

        Shop shop = shopRepository.findById(shopId).orElseThrow();
        shop.setStatus(ShopStatus.SUSPENDED);
        shop.setActionBy("admin-test");
        shop.setActionAt(LocalDateTime.now());
        shopRepository.save(shop);

        // Audit log.
        AuditLog log = AuditLog.builder()
                .action(AuditAction.SHOP_SUSPENDED)
                .resourceType("SHOP")
                .resourceId(shopId)
                .actorId("admin-test")
                .actorUsername("admin-test")
                .actorRole("ADMIN")
                .severity(AuditSeverity.WARNING)
                .reason("Auto reject " + violation.getReason())
                .metadata(java.util.Map.of(
                        "violationType", violation.getType().name(),
                        "violationSeverity", violation.getSeverity().name()))
                .createdAt(LocalDateTime.now())
                .build();
        auditLogRepository.save(log);
    }

    // ========== Stub private mockito helpers (gọi từ test) ==========

    /** Local stub để {@code verify(...)} có thể kiểm tra — dùng qua test class. */
    void whenShopSaveCalled() { /* placeholder */ }

    /**
     * Helper giả lập {@code Page<T>} cho các query repository khi cần.
     */
    static <T> Page<T> emptyPage() {
        return new PageImpl<>(List.of());
    }

    static <T> Page<T> pageOf(T item) {
        return new PageImpl<>(List.of(item));
    }

    static Pageable pageable() {
        return Pageable.unpaged();
    }

    /**
     * Tham chiếu instance method cho Mockito ArgumentMatchers — giữ để IDE
     * highlight khi sửa.
     */
    static <T> T anyInstance() {
        return null;
    }
}
