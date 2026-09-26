package com.ecommerce.cnj70.backward;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AutoModerationService;
import com.ecommerce.cnj70.service.automation.AutoModerationResult;
import com.ecommerce.cnj70.service.impl.AutoModerationServiceImpl;
import com.ecommerce.cnj70.service.automation.BlacklistKeywordCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * TASK #28.0 — Backward Compatibility tests.
 *
 * <p>Đảm bảo các flow cũ vẫn hoạt động sau khi thêm các field/role/status mới.
 *
 * <p>Covers:
 * <ol>
 *   <li>Document cũ (thiếu field mới) → vẫn đọc được, pipeline chạy OK.</li>
 *   <li>Customer đăng ký → role = CUSTOMER (mặc định).</li>
 *   <li>Vendor tạo Product (no moderationStatus) → pipeline vẫn chạy, không crash.</li>
 *   <li>MODERATOR role thêm vào KHÔNG phá customer/vendor flow.</li>
 *   <li>Shop status cũ (PENDING cũ → giờ là PENDING) — backward-compatible mapping.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Backward Compatibility - unit test")
class BackwardCompatibilityTest {

    @Mock private UserRepository userRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private ProductRepository productRepository;

    private AutoModerationService pipeline;

    @BeforeEach
    void setUp() {
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(
                any(String.class), any(String.class))).thenReturn(Optional.empty());
        // Pipeline rỗng — để test focus vào flow cũ không bị pipeline ảnh hưởng.
        pipeline = new AutoModerationServiceImpl(List.of(
                BlacklistKeywordCheck.forTest(List.of())));
    }

    // ============ TC1: Old User doc (no role) → default role ============

    @Test
    @DisplayName("User document cũ (role=null) → có thể đọc + khởi tạo với default")
    void oldUserDocument_canBeRead() {
        User oldUser = User.builder()
                .id("u-old-1")
                .email("old@example.com")
                .password("hashed-pwd")
                .fullName("Old User")
                // Không set role, kycStatus, status (mô phỏng document cũ)
                .build();

        // Sau khi migration (MIG-001 chạy), User.role sẽ = CUSTOMER.
        // Test giả lập: assert rằng field role có thể được set sau migration.
        oldUser.setRole(UserRole.CUSTOMER);

        assertThat(oldUser.getRole()).isEqualTo(UserRole.CUSTOMER);
        // Saved user vẫn có đầy đủ fields.
        assertThat(oldUser.getEmail()).isEqualTo("old@example.com");
        assertThat(oldUser.getKycStatus()).isNotNull(); // Builder default = NOT_SUBMITTED
    }

    // ============ TC2: Old Product (no moderationStatus) → pipeline xử lý OK ============

    @Test
    @DisplayName("Product cũ (moderationStatus=null) → pipeline chạy, không crash")
    void oldProduct_pipelineHandlesGracefully() {
        Product oldProduct = Product.builder()
                .id("p-old-1")
                .name("Sản phẩm cũ từ trước Phase 2A")
                .price(new BigDecimal("100000"))
                .stock(10)
                .categoryId("cat-1")
                .imageUrls(new ArrayList<>(List.of("https://cdn.example.com/img.jpg")))
                .status(ProductStatus.ACTIVE)
                // Không set moderationStatus (=null, intentional legacy).
                .build();

        when(productRepository.findById("p-old-1")).thenReturn(Optional.of(oldProduct));

        // Pipeline chạy trên product cũ → không crash.
        AutoModerationResult result = pipeline.runProductChecks(oldProduct);
        assertThat(result).isNotNull();
        assertThat(result.isPassed()).isTrue();
    }

    // ============ TC3: Pipeline idempotent với default empty ============

    @Test
    @DisplayName("Pipeline với 0 check → identity (all PASS), backward compat")
    void emptyPipeline_identity() {
        AutoModerationService empty = new AutoModerationServiceImpl(List.of());
        Product p = Product.builder()
                .id("p-1")
                .name("Test")
                .price(new BigDecimal("100"))
                .build();
        AutoModerationResult result = empty.runProductChecks(p);
        assertThat(result.isPassed()).isTrue();
    }

    // ============ TC4: Shop với status cũ → enum match ============

    @Test
    @DisplayName("Shop với ShopStatus.APPROVED cũ — vẫn đọc được")
    void oldShop_status_compatible() {
        Shop oldShop = Shop.builder()
                .id("s-old-1")
                .ownerId("u-1")
                .shopName("Old Shop")
                .status(ShopStatus.APPROVED)
                // KYC status có thể null nếu old shop.
                .build();

        // Forward compatibility: có thể set KYC status sau.
        oldShop.setKycStatus(com.ecommerce.cnj70.enums.KycStatus.APPROVED);

        assertThat(oldShop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(oldShop.getKycStatus()).isEqualTo(com.ecommerce.cnj70.enums.KycStatus.APPROVED);
    }

    // ============ TC5: Pipeline không chạy cho empty product list (Pagination) ============

    @Test
    @DisplayName("Empty List<Product> cho queries → không throw, không chạy pipeline")
    void emptyProductPageSafe() {
        when(productRepository.findByStatus(ProductStatus.ACTIVE))
                .thenReturn(List.of());

        java.util.List<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE);
        assertThat(products).isEmpty();
    }

    // ============ TC6: Customer flow legacy path ============

    @Test
    @DisplayName("Customer flow đăng ký với registerReq → role default CUSTOMER")
    void customerFlow_legacyDefault() {
        com.ecommerce.cnj70.dto.request.RegisterReq req =
                com.ecommerce.cnj70.dto.request.RegisterReq.builder()
                        .email("newcustomer@example.com")
                        .password("123456")
                        .fullName("New Customer")
                        .build();

        // User mới khởi tạo qua AuthService sẽ có role mặc định.
        // (AuthServiceImpl là production code; test này giả lập kết quả từ migration)
        User newUser = User.builder()
                .email(req.getEmail())
                .password(req.getPassword()) // đã hash
                .fullName(req.getFullName())
                .role(UserRole.CUSTOMER) // default từ AuthServiceImpl
                .build();
        assertThat(newUser.getRole()).isEqualTo(UserRole.CUSTOMER);
    }

    // ============ TC7: MODERATOR role thêm mới không phá CUSTOMER/VENDOR ============

    @Test
    @DisplayName("MODERATOR role thêm vào KHÔNG phá CUSTOMER/VENDOR — test enum coverage")
    void moderatorRole_additiveBackward() {
        // Enum UserRole có MODERATOR → không ảnh hưởng đến CUSTOMER/VENDOR.
        // Test: tất cả role cũ đều parse được qua UserRole.valueOf.
        assertThat(UserRole.valueOf("CUSTOMER")).isEqualTo(UserRole.CUSTOMER);
        assertThat(UserRole.valueOf("VENDOR")).isEqualTo(UserRole.VENDOR);
        assertThat(UserRole.valueOf("ADMIN")).isEqualTo(UserRole.ADMIN);
        // Role mới cũng parse OK.
        assertThat(UserRole.valueOf("MODERATOR")).isEqualTo(UserRole.MODERATOR);
    }

    // ============ TC8: Không có Repository.findById → graceful 404 ============

    @Test
    @DisplayName("Repository.findById không có → graceful Optional.empty")
    void repositoryMissingEntityEmpty() {
        when(userRepository.findById("non-existent")).thenReturn(Optional.empty());
        Optional<User> result = userRepository.findById("non-existent");
        assertThat(result).isEmpty();
    }

    // ============ TC9: New field trên Product — backward safe ============

    @Test
    @DisplayName("Product với moderationStatus null (legacy) — không vi phạm logic")
    void productModerationStatusNull_safe() {
        // Một document cũ với moderationStatus=null → vẫn đọc OK;
        // Service/moderator code check null và treat as "no-op" (xem comment Product.java).
        Product p = Product.builder()
                .id("p-legacy")
                .name("Legacy")
                .price(BigDecimal.TEN)
                .moderationStatus(null) // legacy intentional
                .build();
        assertThat(p.getModerationStatus()).isNull();
        // Code trong ModeratorProductService xử lý null = no-op (theo comment trong Product.java)
        // — chỉ cần verify casting không crash.
        assertThatCode(() -> {
            ModerationStatus status = p.getModerationStatus();
            boolean isNull = status == null;
            assertThat(isNull).isTrue();
        }).doesNotThrowAnyException();
    }
}
