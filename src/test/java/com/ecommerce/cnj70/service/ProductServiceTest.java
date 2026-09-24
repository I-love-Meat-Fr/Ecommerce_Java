package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.dto.request.ProductFormReq;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.automation.AutoModerationResult;
import com.ecommerce.cnj70.service.impl.ProductServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho ProductServiceImpl.
 *
 * <p>C1 — Product Moderation Pipeline:
 * <ul>
 *   <li>createProduct: ép PENDING_AUTO, gọi pipeline, áp kết quả.</li>
 *   <li>updateProduct: bỏ qua {@code request.getStatus()} (vendor bypass chặn),
 *       re-run pipeline khi thay đổi significant.</li>
 * </ul>
 *
 * <p>Behavior cũ vẫn giữ cho các case không liên quan C1 (validate stock,
 * category, price...). Pipeline được mock trả PASS mặc định (giả lập hiện
 * chưa có AutoCheckStrategy nào).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProductService - unit test")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;
    // C1 — pipeline + side effects
    @Mock private AutoModerationService autoModerationService;
    @Mock private ReportCaseService reportCaseService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) p.setId("p-new");
            return p;
        });
        // C1 — default pipeline result = PASS (mô phỏng hiện chưa có AutoCheckStrategy nào).
        when(autoModerationService.runProductChecks(any(Product.class)))
                .thenAnswer(inv -> AutoModerationResult.pass(inv.getArgument(0)));
    }

    // ============ createProduct ============

    @Test
    @DisplayName("createProduct: stock âm → throw BadRequestException")
    void createProduct_negativeStock_throws() {
        ProductFormReq req = ProductFormReq.builder()
                .name("SP Test")
                .price(new BigDecimal("100000"))
                .stock(-1)
                .build();

        assertThatThrownBy(() -> productService.createProduct(req, "shop-1", "Shop 1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tồn kho");
    }

    @Test
    @DisplayName("createProduct: categoryId không tồn tại → throw BadRequestException")
    void createProduct_invalidCategory_throws() {
        when(categoryRepository.findById("cat-x")).thenReturn(Optional.empty());

        ProductFormReq req = ProductFormReq.builder()
                .name("SP Test")
                .price(new BigDecimal("100000"))
                .stock(10)
                .categoryId("cat-x")
                .build();

        assertThatThrownBy(() -> productService.createProduct(req, "shop-1", "Shop 1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Danh mục");
    }

    @Test
    @DisplayName("createProduct: happy path → pipeline PASS → ACTIVE + thumbnail + shopId")
    void createProduct_happyPath_pipelinePassedBecomesActive() {
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(
                TestFixtures.category("cat-1", "Điện tử")));

        ProductFormReq req = ProductFormReq.builder()
                .name("iPhone Test")
                .price(new BigDecimal("20000000"))
                .stock(5)
                .categoryId("cat-1")
                .imageUrls(List.of("/uploads/a.jpg", "/uploads/b.jpg"))
                .build();

        Product saved = productService.createProduct(req, "shop-1", "Shop ABC");

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(saved.getCategoryName()).isEqualTo("Điện tử");
        assertThat(saved.getThumbnailUrl()).isEqualTo("/uploads/a.jpg");
        assertThat(saved.getShopId()).isEqualTo("shop-1");
        verify(autoModerationService, times(1)).runProductChecks(any(Product.class));
    }

    @Test
    @DisplayName("createProduct: pipeline MANUAL_REVIEW → product MANUAL_REVIEW + ReportCase created")
    void createProduct_pipelineManualReview_createsReportCase() {
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(
                TestFixtures.category("cat-1", "Điện tử")));
        when(autoModerationService.runProductChecks(any(Product.class)))
                .thenAnswer(inv -> AutoModerationResult.manualReview(
                        inv.getArgument(0),
                        new ArrayList<>(List.of("BLACKLIST_KEYWORD:HIT")),
                        new ArrayList<>(List.of("phát hiện từ cấm"))
                ));

        ProductFormReq req = ProductFormReq.builder()
                .name("Sp có từ khóa bị cấm")
                .price(new BigDecimal("100000"))
                .stock(5)
                .categoryId("cat-1")
                .build();

        Product saved = productService.createProduct(req, "shop-1", "Shop ABC");

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
        verify(reportCaseService, times(1))
                .createCase(any(), any(), org.mockito.ArgumentMatchers.isNull());
    }

    @Test
    @DisplayName("createProduct: pipeline REJECTED_AUTO → product REJECTED_AUTO + KHÔNG tạo ReportCase")
    void createProduct_pipelineAutoRejected_noReportCase() {
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(
                TestFixtures.category("cat-1", "Điện tử")));
        when(autoModerationService.runProductChecks(any(Product.class)))
                .thenAnswer(inv -> AutoModerationResult.autoReject(
                        inv.getArgument(0),
                        new ArrayList<>(List.of("FORBIDDEN_CATEGORY:WEAPON")),
                        new ArrayList<>(List.of("danh mục bị cấm"))
                ));

        ProductFormReq req = ProductFormReq.builder()
                .name("Vũ khí")
                .price(new BigDecimal("100000"))
                .stock(5)
                .categoryId("cat-1")
                .build();

        Product saved = productService.createProduct(req, "shop-1", "Shop ABC");

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);
        // REJECTED_AUTO không cần ReportCase — đã là verdict cuối
        verify(reportCaseService, never()).createCase(any(), any(), any());
    }

    @Test
    @DisplayName("createProduct: pipeline throw exception → fallback MANUAL_REVIEW + ReportCase")
    void createProduct_pipelineThrows_fallbackToManualReview() {
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(
                TestFixtures.category("cat-1", "Điện tử")));
        when(autoModerationService.runProductChecks(any(Product.class)))
                .thenThrow(new RuntimeException("Pipeline down"));

        ProductFormReq req = ProductFormReq.builder()
                .name("Test")
                .price(new BigDecimal("100000"))
                .stock(5)
                .categoryId("cat-1")
                .build();

        Product saved = productService.createProduct(req, "shop-1", "Shop ABC");

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
        verify(reportCaseService, times(1))
                .createCase(any(), any(), org.mockito.ArgumentMatchers.isNull());
    }

    // ============ updateProduct ============

    @Test
    @DisplayName("updateProduct: price <= 0 → throw BadRequestException")
    void updateProduct_invalidPrice_throws() {
        Product existing = TestFixtures.activeProduct("p-1", "shop-1", 5);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));

        ProductFormReq req = ProductFormReq.builder()
                .price(BigDecimal.ZERO)
                .build();

        assertThatThrownBy(() -> productService.updateProduct("p-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Giá");
    }

    @Test
    @DisplayName("updateProduct: stock < 0 → throw BadRequestException")
    void updateProduct_negativeStock_throws() {
        Product existing = TestFixtures.activeProduct("p-1", "shop-1", 5);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));

        ProductFormReq req = ProductFormReq.builder().stock(-3).build();

        assertThatThrownBy(() -> productService.updateProduct("p-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("tồn kho");
    }

    @Test
    @DisplayName("updateProduct: chỉ thay đổi stock (không significant) → KHÔNG re-run pipeline")
    void updateProduct_partialUpdate_keepsUntouched() {
        Product existing = TestFixtures.activeProduct("p-1", "shop-1", 10);
        existing.setName("Tên cũ");
        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));

        ProductFormReq req = ProductFormReq.builder().stock(7).build();

        Product updated = productService.updateProduct("p-1", req);

        assertThat(updated.getStock()).isEqualTo(7);
        assertThat(updated.getName()).isEqualTo("Tên cũ");
        // Pipeline KHÔNG chạy vì stock-only update không phải significant change
        verify(autoModerationService, never()).runProductChecks(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct: C1 — request.setStatus(ACTIVE) bị BỎ QUA, vendor không bypass")
    void updateProduct_vendorStatusChange_ignored() {
        Product existing = TestFixtures.hiddenProduct("p-1", "shop-1", 10); // HIDDEN
        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));

        ProductFormReq req = ProductFormReq.builder()
                .stock(20)
                .status(ProductStatus.ACTIVE) // vendor cố bypass
                .build();

        Product updated = productService.updateProduct("p-1", req);

        assertThat(updated.getStatus()).isEqualTo(ProductStatus.HIDDEN); // KHÔNG đổi
        verify(autoModerationService, never()).runProductChecks(any(Product.class));
    }

    @Test
    @DisplayName("updateProduct: thay đổi significant (name) trên ACTIVE → re-run pipeline")
    void updateProduct_significantChangeName_triggersReModeration() {
        Product existing = TestFixtures.activeProduct("p-1", "shop-1", 10);
        existing.setName("Tên cũ");
        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));

        ProductFormReq req = ProductFormReq.builder()
                .name("Tên mới đáng ngờ")
                .build();

        Product updated = productService.updateProduct("p-1", req);

        assertThat(updated.getName()).isEqualTo("Tên mới đáng ngờ");
        // Pipeline được gọi vì name là significant change trên ACTIVE product
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(autoModerationService, times(1)).runProductChecks(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProductStatus.PENDING_AUTO);
    }

    @Test
    @DisplayName("updateProduct: thay đổi significant trên HIDDEN → KHÔNG re-run (admin-controlled)")
    void updateProduct_significantChangeOnHidden_noReModeration() {
        Product existing = TestFixtures.hiddenProduct("p-1", "shop-1", 10);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));

        ProductFormReq req = ProductFormReq.builder()
                .name("Tên mới")
                .build();

        productService.updateProduct("p-1", req);

        // HIDDEN products are admin-controlled — vendor update must not auto-restore
        verify(autoModerationService, never()).runProductChecks(any(Product.class));
    }

    // ============ deleteProduct ============

    @Test
    @DisplayName("deleteProduct: status HIDDEN → throw BadRequestException (đã xóa trước đó)")
    void deleteProduct_alreadyHidden_throws() {
        Product hidden = TestFixtures.hiddenProduct("p-1", "shop-1", 0);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(hidden));

        assertThatThrownBy(() -> productService.deleteProduct("p-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("xóa trước đó");
    }

    // ============ getProductById ============

    @Test
    @DisplayName("getProductById: id không tồn tại → ResourceNotFoundException")
    void getProductById_notFound_throws() {
        when(productRepository.findById("p-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById("p-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ updateProductStatus ============

    @Test
    @DisplayName("updateProductStatus: round-trip lưu status mới")
    void updateProductStatus_changesStatus() {
        Product existing = TestFixtures.activeProduct("p-1", "shop-1", 10);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(existing));

        productService.updateProductStatus("p-1", ProductStatus.HIDDEN);

        assertThat(existing.getStatus()).isEqualTo(ProductStatus.HIDDEN);
    }
}
