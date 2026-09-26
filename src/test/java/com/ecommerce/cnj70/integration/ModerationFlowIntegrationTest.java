package com.ecommerce.cnj70.integration;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.dto.request.KycSubmitRequest;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.*;
import com.ecommerce.cnj70.repository.*;
import com.ecommerce.cnj70.service.AutoModerationService;
import com.ecommerce.cnj70.service.automation.AutoCheckVerdict;
import com.ecommerce.cnj70.service.automation.BlacklistKeywordCheck;
import com.ecommerce.cnj70.service.automation.ForbiddenCategoryCheck;
import com.ecommerce.cnj70.service.automation.ForbiddenCategoryPolicy;
import com.ecommerce.cnj70.service.impl.AutoModerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration Test #26.0 — Moderation Flow end-to-end với mock repositories.
 *
 * <p>Flow theo architecture hiện tại:
 *
 * <pre>
 *   KYC submit              → Shop.kycStatus = PENDING_THIRD_PARTY
 *   KYC callback/approve    → Shop.kycStatus = APPROVED
 *   Admin approve Shop      → Shop.status = ACTIVE / APPROVED
 *   Vendor create Product   → Auto Moderation pipeline
 *   Pipeline (HARD_REJECT)  → Product.status = REJECTED_AUTO
 *   Moderator review        → Product.status = ACTIVE
 *   Admin escalate          → Violation + Shop.status = SUSPENDED
 * </pre>
 *
 * <p>Test này KHÔNG yêu cầu MongoDB chạy — chỉ mock toàn bộ repository layer
 * theo pattern hiện có của project (ProductServiceTest, ...). Đây là
 * "component integration test" tập trung vào state transition + audit
 * emission.
 *
 * <p>Để chạy với MongoDB thật (full {@code @SpringBootTest}), cần embedded
 * MongoDB — <b>chưa có dependency đó trong pom.xml</b>, nên tạm thời theo
 * cách này để không vi phạm rule "không thêm dependency lớn cho feature nhỏ".
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Moderation Flow - integration test (mock repositories)")
class ModerationFlowIntegrationTest {

    @Mock private ShopRepository shopRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ViolationRepository violationRepository;
    @Mock private AuditLogRepository auditLogRepository;

    private AutoModerationService autoModeration;
    private ModerationFlowHarness harness;

    @BeforeEach
    void setUp() {
        buildCleanPipeline();

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) p.setId("p-new");
            return p;
        });
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(violationRepository.save(any(Violation.class))).thenAnswer(inv -> inv.getArgument(0));

        harness = new ModerationFlowHarness(
                shopRepository, userRepository, productRepository,
                violationRepository, auditLogRepository,
                () -> autoModeration);
    }

    private void buildCleanPipeline() {
        var blacklistCheck = BlacklistKeywordCheck.forTest(List.of("vũ khí", "cấm"));
        var policy = ForbiddenCategoryPolicy.forTest(List.of("cat-forbidden"));
        var forbiddenCheck = new ForbiddenCategoryCheck(policy);
        autoModeration = new AutoModerationServiceImpl(List.of(blacklistCheck, forbiddenCheck));
    }

    // ======================== TC1 — Happy path ========================

    @Test
    @DisplayName("Happy path: Shop APPROVED + Product clean → ACTIVE")
    void happyPath_productActive() {
        harness.seedShop("u-1", "s-1", ShopStatus.APPROVED, KycStatus.APPROVED);

        ProductFormReq req = ProductFormReq.builder()
                .name("Ốp lưng iPhone chính hãng")
                .description("Sản phẩm chất lượng")
                .price(new BigDecimal("150000"))
                .stock(10)
                .categoryId("cat-phone")
                .build();

        Product created = harness.createProduct("s-1", req);

        assertThat(created.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        ArgumentCaptor<Shop> shopCaptor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository, atLeast(1)).save(shopCaptor.capture());
        assertThat(shopCaptor.getAllValues())
                .extracting(Shop::getStatus)
                .contains(ShopStatus.APPROVED);
    }

    // ======================== TC2 — HARD_REJECT path ========================

    @Test
    @DisplayName("HARD_REJECT path: Product chứa blacklist → REJECTED_AUTO")
    void hardRejectPath_productRejectedAuto() {
        harness.seedShop("u-1", "s-1", ShopStatus.APPROVED, KycStatus.APPROVED);

        ProductFormReq req = ProductFormReq.builder()
                .name("Súng vũ khí hàng nặng")
                .description("Sản phẩm thuộc danh mục cấm")
                .price(new BigDecimal("500"))
                .stock(10)
                .categoryId("cat-1")
                .build();

        Product created = harness.createProduct("s-1", req);

        assertThat(created.getStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);
    }

    // ======================== TC3 — SOFT_FLAG + Duplicate ========================

    @Test
    @DisplayName("SOFT_FLAG path: duplicate image → MANUAL_REVIEW")
    void softFlagPath_productManualReview() {
        harness.seedShop("u-1", "s-1", ShopStatus.APPROVED, KycStatus.APPROVED);

        // Re-build pipeline with duplicate check.
        var blacklistCheck = BlacklistKeywordCheck.forTest(List.of());
        var policy = ForbiddenCategoryPolicy.forTest(List.of());
        var forbiddenCheck = new ForbiddenCategoryCheck(policy);
        var imageHashService = new com.ecommerce.cnj70.service.ImageHashService();
        var duplicateCheck = new com.ecommerce.cnj70.service.automation.DuplicateProductCheck(
                productRepository, imageHashService);
        autoModeration = new AutoModerationServiceImpl(
                List.of(blacklistCheck, forbiddenCheck, duplicateCheck));

        // Pre-existing approved product with same image URL.
        Product existing = Product.builder()
                .id("p-existing")
                .name("Existing product")
                .imageUrls(List.of("https://cdn.example.com/img/shared.jpg"))
                .shopId("s-1")
                .status(ProductStatus.ACTIVE)
                .moderationStatus(ModerationStatus.APPROVED)
                .build();
        when(productRepository.findById("p-existing")).thenReturn(Optional.of(existing));
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(
                any(String.class), any(String.class))).thenReturn(Optional.of(existing));

        ProductFormReq req = ProductFormReq.builder()
                .name("Sản phẩm mới")
                .description("Mô tả sản phẩm")
                .price(new BigDecimal("150000"))
                .stock(10)
                .categoryId("cat-phone")
                .imageUrls(List.of("https://cdn.example.com/img/shared.jpg"))
                .build();

        Product created = harness.createProduct("s-1", req);

        assertThat(created.getStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
    }

    // ======================== TC4 — Full escalation chain ========================

    @Test
    @DisplayName("Full escalation: Product HARD_REJECT → Violation → Shop SUSPENDED")
    void fullEscalation_chain() {
        harness.seedShop("u-1", "s-1", ShopStatus.APPROVED, KycStatus.APPROVED);

        ProductFormReq req = ProductFormReq.builder()
                .name("Sản phẩm cấm")
                .price(new BigDecimal("100"))
                .stock(5)
                .categoryId("cat-1")
                .build();
        Product rejected = harness.createProduct("s-1", req);
        assertThat(rejected.getStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);

        // Sau HARD_REJECT, Admin tạo violation và suspend shop.
        Violation violation = Violation.builder()
                .shopId("s-1")
                .type(ViolationType.WARNING)
                .severity(ViolationSeverity.HIGH)
                .reason("Auto moderation đã reject 3 sản phẩm cấm liên tiếp")
                .productId(rejected.getId())
                .build();
        harness.recordViolationAndSuspend("s-1", violation);

        ArgumentCaptor<Violation> violationCaptor = ArgumentCaptor.forClass(Violation.class);
        verify(violationRepository, times(1)).save(violationCaptor.capture());
        assertThat(violationCaptor.getValue().getShopId()).isEqualTo("s-1");

        ArgumentCaptor<Shop> shopCaptor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository, atLeast(1)).save(shopCaptor.capture());
        assertThat(shopCaptor.getAllValues())
                .extracting(Shop::getStatus)
                .contains(ShopStatus.SUSPENDED);

        verify(auditLogRepository, atLeast(1)).save(any(com.ecommerce.cnj70.document.AuditLog.class));
    }

    // ======================== TC5 — Backward compat ========================

    @Test
    @DisplayName("Backward compat: Shop không có product → Shop giữ APPROVED")
    void backwardCompat_noProducts() {
        harness.seedShop("u-1", "s-1", ShopStatus.APPROVED, KycStatus.APPROVED);

        Shop shop = shopRepository.findById("s-1").orElseThrow();
        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);

        verify(violationRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }
}
