package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ProductPriceHistory;
import com.ecommerce.cnj70.repository.ProductPriceHistoryRepository;
import com.ecommerce.cnj70.service.automation.AutoCheckStrategy.AutoCheckContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link PriceAnomalyCheck} (task #5.0).
 *
 * <p>Covers:
 * <ol>
 *   <li>Giá nằm trong dải bình thường → PASS.</li>
 *   <li>Giá tăng bất thường (outlier > median + z*MAD) → SOFT_FLAG.</li>
 *   <li>Giá giảm bất thường → SOFT_FLAG.</li>
 *   <li>Không đủ history (&lt; minSamples) → PASS.</li>
 *   <li>History rỗng → PASS.</li>
 *   <li>Null price → PASS.</li>
 *   <li>Null product / null ctx → PASS.</li>
 *   <li>Math: computeMedian, computeMad (deterministic).</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PriceAnomalyCheck - unit test")
class PriceAnomalyCheckTest {

    @Mock private ProductPriceHistoryRepository historyRepository;
    private PriceAnomalyCheck check;

    @BeforeEach
    void setUp() {
        // minSamples = 5, zThreshold = 3.0
        check = PriceAnomalyCheck.forTest(historyRepository, 30, 3.0, 5, 1.0);
    }

    private AutoCheckContext ctxFor(Product product) {
        return AutoCheckContext.builder()
                .product(product)
                .sharedState(new HashMap<>())
                .build();
    }

    private Product productWithPrice(String id, BigDecimal price) {
        return Product.builder()
                .id(id)
                .name("Test " + id)
                .price(price)
                .shopId("s-1")
                .build();
    }

    private List<ProductPriceHistory> historyPrices(BigDecimal... values) {
        List<ProductPriceHistory> list = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < values.length; i++) {
            list.add(ProductPriceHistory.builder()
                    .price(values[i])
                    .productId("p-1")
                    .recordedAt(now.minusDays(i))
                    .build());
        }
        return list;
    }

    @Test
    @DisplayName("Giá bình thường → PASS")
    void normalPrice_pass() {
        // History prices: 100, 110, 95, 105, 100. Median ≈ 100, MAD ≈ 5-10.
        // Current price = 102 → z ≈ 0.4 → PASS.
        when(historyRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                eq("p-1"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(historyPrices(
                        new BigDecimal("100"), new BigDecimal("110"),
                        new BigDecimal("95"), new BigDecimal("105"),
                        new BigDecimal("100")));

        Product p = productWithPrice("p-1", new BigDecimal("102"));
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Giá tăng bất thường → SOFT_FLAG PRICE_ANOMALY")
    void priceIncreased_suspicious_softFlag() {
        // History ổn định quanh 100-110. Current = 10000 → bất thường.
        when(historyRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                eq("p-1"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(historyPrices(
                        new BigDecimal("100"), new BigDecimal("110"),
                        new BigDecimal("95"), new BigDecimal("105"),
                        new BigDecimal("100"), new BigDecimal("102")));

        Product p = productWithPrice("p-1", new BigDecimal("10000"));
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isSoftFlag()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("PRICE_ANOMALY");
        assertThat(v.getMessage()).contains("bất thường");
    }

    @Test
    @DisplayName("Giá giảm bất thường → SOFT_FLAG")
    void priceDropped_suspicious_softFlag() {
        when(historyRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                eq("p-1"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(historyPrices(
                        new BigDecimal("10000"), new BigDecimal("10500"),
                        new BigDecimal("9800"), new BigDecimal("10200"),
                        new BigDecimal("9900"), new BigDecimal("10100")));

        Product p = productWithPrice("p-1", new BigDecimal("10"));
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isSoftFlag()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("PRICE_ANOMALY");
    }

    @Test
    @DisplayName("Không đủ history (< minSamples=5) → PASS không đánh giá")
    void insufficientHistory_pass() {
        when(historyRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                eq("p-1"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(historyPrices(
                        new BigDecimal("100"), new BigDecimal("110"),
                        new BigDecimal("95")));  // 3 mẫu < 5

        Product p = productWithPrice("p-1", new BigDecimal("100000"));
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("History rỗng → PASS")
    void emptyHistory_pass() {
        when(historyRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                eq("p-1"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(List.of());

        Product p = productWithPrice("p-1", new BigDecimal("999999"));
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Null price → PASS")
    void nullPrice_pass() {
        when(historyRepository.findByProductIdAndRecordedAtGreaterThanEqual(
                eq("p-1"), any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(historyPrices(new BigDecimal("100"), new BigDecimal("110")));
        Product p = productWithPrice("p-1", null);
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Null product → PASS")
    void nullProduct_pass() {
        AutoCheckVerdict v = check.check(ctxFor(null));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Null ctx → PASS")
    void nullCtx_pass() {
        AutoCheckVerdict v = check.check(null);
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Product mới (id=null) → PASS (không có history)")
    void newProduct_pass() {
        Product p = productWithPrice(null, new BigDecimal("100"));
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("id() = 'ABNORMAL_PRICE'")
    void id_returnsConstant() {
        assertThat(check.id()).isEqualTo("ABNORMAL_PRICE");
    }

    @Test
    @DisplayName("Math: computeMedian odd → middle value")
    void math_medianOdd() {
        BigDecimal m = PriceAnomalyCheck.computeMedian(List.of(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30")));
        assertThat(m).isEqualByComparingTo(new BigDecimal("20"));
    }

    @Test
    @DisplayName("Math: computeMedian even → average of middle two")
    void math_medianEven() {
        BigDecimal m = PriceAnomalyCheck.computeMedian(List.of(
                new BigDecimal("10"),
                new BigDecimal("20"),
                new BigDecimal("30"),
                new BigDecimal("40")));
        assertThat(m).isEqualByComparingTo(new BigDecimal("25"));
    }

    @Test
    @DisplayName("Math: computeMad — median của |xi - median|")
    void math_mad() {
        // Data: 10, 20, 30 → median = 20, deviations: 10, 0, 10, median = 10.
        BigDecimal mad = PriceAnomalyCheck.computeMad(
                List.of(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("30")),
                new BigDecimal("20"));
        assertThat(mad).isEqualByComparingTo(new BigDecimal("10"));
    }
}
