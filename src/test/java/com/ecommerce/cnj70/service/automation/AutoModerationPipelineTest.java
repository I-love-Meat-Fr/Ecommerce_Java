package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ProductPriceHistory;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.repository.ProductPriceHistoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.ImageHashService;
import com.ecommerce.cnj70.service.impl.AutoModerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link AutoModerationServiceImpl} với tất cả AutoCheckStrategy
 * implementations đã đăng ký.
 *
 * <p>Test aggregate behavior:
 * <ol>
 *   <li>Clean product → ACTIVE (PASS).</li>
 *   <li>Blacklist keyword → REJECTED_AUTO (HARD_REJECT short-circuits).</li>
 *   <li>Forbidden category → REJECTED_AUTO.</li>
 *   <li>Duplicate image → MANUAL_REVIEW (SOFT_FLAG).</li>
 *   <li>Price anomaly → MANUAL_REVIEW (SOFT_FLAG).</li>
 *   <li>Multiple soft flags → MANUAL_REVIEW.</li>
 *   <li>Stock placeholder image + blacklist → REJECTED_AUTO (HARD_REJECT ưu tiên).</li>
 *   <li>Pipeline rỗng → ACTIVE (identity).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AutoModeration Pipeline - integration unit test")
class AutoModerationPipelineTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductPriceHistoryRepository priceHistoryRepository;

    private ImageHashService imageHashService;
    private BlacklistKeywordCheck blacklistCheck;
    private DuplicateProductCheck duplicateCheck;
    private ImageHashCheck imageHashCheck;
    private PriceAnomalyCheck priceCheck;
    private ForbiddenCategoryCheck forbiddenCategoryCheck;
    private ForbiddenCategoryPolicy policy;

    private AutoModerationServiceImpl pipeline;

    @BeforeEach
    void setUp() {
        imageHashService = new ImageHashService();
        blacklistCheck = BlacklistKeywordCheck.forTest(List.of("vũ khí", "fake"));
        policy = ForbiddenCategoryPolicy.forTest(List.of("cat-forbidden"));
        forbiddenCategoryCheck = new ForbiddenCategoryCheck(policy);
        duplicateCheck = new DuplicateProductCheck(productRepository, imageHashService);
        imageHashCheck = ImageHashCheck.forTest(imageHashService, List.of("jpg", "png", "webp"));
        priceCheck = PriceAnomalyCheck.forTest(priceHistoryRepository, 30, 3.0, 5, 1.0);

        pipeline = new AutoModerationServiceImpl(List.of(
                blacklistCheck,
                forbiddenCategoryCheck,
                duplicateCheck,
                imageHashCheck,
                priceCheck
        ));
    }

    private Product cleanProduct() {
        return Product.builder()
                .id("p-clean")
                .name("Ốp lưng iPhone chính hãng")
                .description("Sản phẩm chất lượng cao")
                .price(new BigDecimal("150000"))
                .imageUrls(new ArrayList<>(List.of("https://cdn.shop.com/img/abc.jpg")))
                .categoryId("cat-phone")
                .shopId("s-1")
                .build();
    }

    // ============== 1. Clean product → PASS → ACTIVE ==============

    @Test
    @DisplayName("Clean product → tất cả PASS → ACTIVE")
    void cleanProduct_pass_allActive() {
        // Duplicate: no other product matches.
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(any(), any()))
                .thenReturn(Optional.empty());
        // Price: empty history.
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                any(), any(), any())).thenReturn(List.of());

        Product p = cleanProduct();
        AutoModerationResult result = pipeline.runProductChecks(p);

        assertThat(result.isPassed()).isTrue();
        assertThat(result.getTargetStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result.getAutoFlags()).isEmpty();
    }

    // ============== 2. Blacklist keyword → HARD_REJECT short-circuit ==============

    @Test
    @DisplayName("Blacklist keyword → HARD_REJECT → REJECTED_AUTO (short-circuit)")
    void blacklist_hardReject() {
        Product p = cleanProduct();
        p.setName("Súng vũ khí hàng nặng");

        AutoModerationResult result = pipeline.runProductChecks(p);

        assertThat(result.isAutoRejected()).isTrue();
        assertThat(result.getTargetStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);
        assertThat(result.getAutoFlags()).anyMatch(f -> f.contains("BLACKLIST_KEYWORD"));
    }

    // ============== 3. Forbidden category → HARD_REJECT ==============

    @Test
    @DisplayName("Forbidden category → HARD_REJECT → REJECTED_AUTO")
    void forbiddenCategory_hardReject() {
        Product p = cleanProduct();
        p.setCategoryId("cat-forbidden");

        AutoModerationResult result = pipeline.runProductChecks(p);

        assertThat(result.isAutoRejected()).isTrue();
        assertThat(result.getTargetStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);
        assertThat(result.getAutoFlags()).anyMatch(f -> f.contains("FORBIDDEN_CATEGORY"));
    }

    // ============== 4. Duplicate image → SOFT_FLAG → MANUAL_REVIEW ==============

    @Test
    @DisplayName("Duplicate image → SOFT_FLAG → MANUAL_REVIEW")
    void duplicate_softFlag() {
        Product other = Product.builder()
                .id("p-other").name("Another phone case").build();
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(
                eq("https://cdn.shop.com/img/abc.jpg"), eq("p-clean")))
                .thenReturn(Optional.of(other));

        AutoModerationResult result = pipeline.runProductChecks(cleanProduct());

        assertThat(result.isManualReview()).isTrue();
        assertThat(result.getTargetStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
        assertThat(result.getAutoFlags()).anyMatch(f -> f.contains("DUPLICATE_PRODUCT"));
    }

    // ============== 5. Price anomaly → SOFT_FLAG → MANUAL_REVIEW ==============

    @Test
    @DisplayName("Price anomaly → SOFT_FLAG → MANUAL_REVIEW")
    void priceAnomaly_softFlag() {
        // History quanh 100k-110k. Current price = 10tr → anomaly.
        List<ProductPriceHistory> hist = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 6; i++) {
            hist.add(ProductPriceHistory.builder()
                    .price(new BigDecimal(100000 + i * 2000))
                    .productId("p-clean")
                    .recordedAt(now.minusDays(i))
                    .build());
        }
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                eq("p-clean"), any(LocalDateTime.class), any()))
                .thenReturn(hist);

        Product p = cleanProduct();
        p.setPrice(new BigDecimal("10000000")); // 10M → anomaly

        AutoModerationResult result = pipeline.runProductChecks(p);

        assertThat(result.isManualReview()).isTrue();
        assertThat(result.getAutoFlags()).anyMatch(f -> f.contains("ABNORMAL_PRICE"));
    }

    // ============== 6. Multiple soft flags → MANUAL_REVIEW (aggregate) ==============

    @Test
    @DisplayName("Multiple SOFT_FLAGs → MANUAL_REVIEW với danh sách flag đầy đủ")
    void multipleSoftFlags_manualReview() {
        // Duplicate match
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(any(), any()))
                .thenReturn(Optional.of(Product.builder().id("p-other").build()));
        // Price anomaly
        List<ProductPriceHistory> hist = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < 6; i++) {
            hist.add(ProductPriceHistory.builder()
                    .price(new BigDecimal(100000 + i * 1000))
                    .productId("p-clean")
                    .recordedAt(now.minusDays(i))
                    .build());
        }
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                any(), any(), any())).thenReturn(hist);

        Product p = cleanProduct();
        // Stock placeholder + price anomaly.
        p.setImageUrls(new ArrayList<>(List.of(
                "https://cdn.shop.com/img/abc.jpg",  // duplicate
                "https://placehold.co/600x400")));   // placeholder
        p.setPrice(new BigDecimal("99999999"));

        AutoModerationResult result = pipeline.runProductChecks(p);

        assertThat(result.isManualReview()).isTrue();
        assertThat(result.getAutoFlags().size()).isGreaterThanOrEqualTo(2);
        // Có ít nhất 2 flag từ các check khác nhau.
        long distinctCheckIds = result.getAutoFlags().stream()
                .map(f -> f.substring(0, f.indexOf(':')))
                .distinct()
                .count();
        assertThat(distinctCheckIds).isGreaterThanOrEqualTo(2);
    }

    // ============== 7. HARD_REJECT ưu tiên SOFT_FLAG ==============

    @Test
    @DisplayName("Vừa HARD_REJECT vừa SOFT_FLAG → ưu tiên REJECTED_AUTO")
    void hardRejectPlusSoftFlag_priorityToHard() {
        // Blacklist keyword (HARD_REJECT) + duplicate image (SOFT_FLAG).
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(any(), any()))
                .thenReturn(Optional.of(Product.builder().id("p-other").build()));

        Product p = cleanProduct();
        p.setName("Súng FAKE vũ khí");

        AutoModerationResult result = pipeline.runProductChecks(p);

        assertThat(result.isAutoRejected()).isTrue();
        // Chỉ HARD_REJECT flag được capture (short-circuit).
        assertThat(result.getAutoFlags()).anyMatch(f -> f.contains("BLACKLIST_KEYWORD"));
    }

    // ============== 8. Pipeline rỗng → identity (tất cả PASS) ==============

    @Test
    @DisplayName("Pipeline rỗng (không có check nào) → identity → ACTIVE")
    void emptyPipeline_identity() {
        AutoModerationServiceImpl empty = new AutoModerationServiceImpl(List.of());
        Product p = cleanProduct();
        p.setName("Anything goes");  // sẽ là blacklist nếu check tồn tại
        AutoModerationResult result = empty.runProductChecks(p);
        assertThat(result.isPassed()).isTrue();
    }

    // ============== 9. Null product → throws ==============

    @Test
    @DisplayName("Null product → IllegalArgumentException")
    void nullProduct_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> pipeline.runProductChecks(null));
    }

    // ============== 10. Một check throw → vẫn aggregate ==============

    @Test
    @DisplayName("Một check throw exception → pipeline tiếp tục (fail-safe)")
    void checkThrows_pipelineContinues() {
        // Tạo check custom luôn throw exception.
        AutoCheckStrategy badCheck = new AutoCheckStrategy() {
            @Override
            public String id() { return "BAD_CHECK"; }
            @Override
            public AutoCheckVerdict check(AutoCheckContext ctx) {
                throw new RuntimeException("simulated failure");
            }
        };

        // Mock productRepository để duplicate check không match.
        when(productRepository.findFirstByImageUrlsContainingAndIdNot(any(), any()))
                .thenReturn(Optional.empty());
        when(priceHistoryRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                any(), any(), any())).thenReturn(List.of());

        AutoModerationServiceImpl p = new AutoModerationServiceImpl(List.of(
                badCheck,
                blacklistCheck,
                forbiddenCategoryCheck,
                duplicateCheck,
                imageHashCheck,
                priceCheck
        ));

        AutoModerationResult result = p.runProductChecks(cleanProduct());

        // Bad check counted as SOFT_FLAG → MANUAL_REVIEW.
        assertThat(result.isManualReview()).isTrue();
        assertThat(result.getAutoFlags()).anyMatch(f -> f.contains("BAD_CHECK"));
    }
}
