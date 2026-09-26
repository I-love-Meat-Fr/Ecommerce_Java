package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ProductPriceHistory;
import com.ecommerce.cnj70.repository.ProductPriceHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * C4 — Price anomaly check (task #5.0).
 *
 * <p>Phát hiện khi giá mới của sản phẩm chênh lệch bất thường so với lịch
 * sử giá 30 ngày gần nhất. Sử dụng median + MAD (Median Absolute Deviation)
 * thay vì mean/SD để giảm false positive với outlier có sẵn trong data.
 *
 * <pre>
 *   Product.currentPrice
 *     ↓
 *   ProductPriceHistory (30 ngày)
 *     ↓
 *   median(prices) + MAD(prices)
 *     ↓
 *   z-score = |currentPrice - median| / max(MAD, epsilon)
 *     ↓
 *   z > threshold (default 3.0) → SOFT_FLAG "PRICE_ANOMALY"
 * </pre>
 *
 * <p>Không HARD_REJECT vì giá có thể tăng/giảm hợp lý theo campaign.
 * Moderator xem xét trước khi duyệt.
 *
 * <p>Fail-safe: nếu history rỗng / không đủ mẫu → PASS.
 *
 * <p>Configurable thresholds từ {@code application.yml}:
 * <ul>
 *   <li>{@code moderation.auto.price-history-window-days} (default 30)</li>
 *   <li>{@code moderation.auto.price-anomaly-z-threshold} (default 3.0)</li>
 *   <li>{@code moderation.auto.price-anomaly-min-samples} (default 5)</li>
 *   <li>{@code moderation.auto.price-anomaly-mad-epsilon} (default 1.0) để
 *       tránh chia cho 0.</li>
 * </ul>
 */
@Slf4j
@Component
public class PriceAnomalyCheck implements AutoCheckStrategy {

    /** ID cố định, dùng cho log + autoFlag. */
    public static final String ID = "ABNORMAL_PRICE";

    /** FlagCode trong {@code AutoModerationFlag#PRICE}. */
    public static final String FLAG_CODE = "PRICE_ANOMALY";

    private final ProductPriceHistoryRepository historyRepository;

    private final int windowDays;
    private final double zThreshold;
    private final int minSamples;
    private final BigDecimal madEpsilon;

    public PriceAnomalyCheck(ProductPriceHistoryRepository historyRepository,
                              @Value("${moderation.auto.price-history-window-days:30}")
                              int windowDays,
                              @Value("${moderation.auto.price-anomaly-z-threshold:3.0}")
                              double zThreshold,
                              @Value("${moderation.auto.price-anomaly-min-samples:5}")
                              int minSamples,
                              @Value("${moderation.auto.price-anomaly-mad-epsilon:1.0}")
                              double madEpsilon) {
        this.historyRepository = historyRepository;
        this.windowDays = windowDays;
        this.zThreshold = zThreshold;
        this.minSamples = minSamples;
        this.madEpsilon = BigDecimal.valueOf(madEpsilon);
        log.info("[PriceAnomaly] Initialized: windowDays={}, zThreshold={}, minSamples={}, madEpsilon={}",
                windowDays, zThreshold, minSamples, madEpsilon);
    }

    /** Test-only constructor. */
    public static PriceAnomalyCheck forTest(ProductPriceHistoryRepository repo,
                                             int windowDays, double zThreshold,
                                             int minSamples, double madEpsilon) {
        return new PriceAnomalyCheck(repo, windowDays, zThreshold, minSamples, madEpsilon);
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public AutoCheckVerdict check(AutoCheckContext ctx) {
        if (ctx == null || ctx.getProduct() == null) {
            return AutoCheckVerdict.pass();
        }
        Product product = ctx.getProduct();
        BigDecimal currentPrice = product.getPrice();
        if (currentPrice == null || currentPrice.signum() <= 0) {
            return AutoCheckVerdict.pass();
        }
        String productId = product.getId();
        if (productId == null || productId.isBlank()) {
            // Product mới, chưa có ID — không thể so sánh với history.
            return AutoCheckVerdict.pass();
        }

        // Load history (lấy tối đa minSamples+10 để có dư cho median/MAD).
        LocalDateTime since = LocalDateTime.now().minusDays(windowDays);
        List<ProductPriceHistory> history = historyRepository
                .findByProductIdAndRecordedAtGreaterThanEqual(
                        productId, since, PageRequest.of(0, Math.max(minSamples + 10, 20)));

        // Lấy tập price values (không tính null).
        List<BigDecimal> prices = new ArrayList<>();
        for (ProductPriceHistory h : history) {
            if (h.getPrice() != null) {
                prices.add(h.getPrice());
            }
        }

        // Bỏ chính giá hiện tại (nếu trùng) để không match chính nó.
        prices.removeIf(currentPrice::equals);

        if (prices.size() < minSamples) {
            // Không đủ data — KHÔNG đánh giá, để PASS.
            log.debug("[PriceAnomaly] Not enough history samples ({} < {}) for product id={}",
                    prices.size(), minSamples, productId);
            return AutoCheckVerdict.pass();
        }

        BigDecimal median = computeMedian(prices);
        BigDecimal mad = computeMad(prices, median);

        // Tránh chia cho 0: scale MAD xuống epsilon.
        BigDecimal denom = mad.signum() == 0 ? madEpsilon : mad;

        BigDecimal diff = currentPrice.subtract(median).abs();
        // z = diff / max(mad, epsilon)
        if (denom.signum() == 0) {
            // Vẫn chia cho 0 thì skip check.
            return AutoCheckVerdict.pass();
        }
        double z = diff.doubleValue() / denom.doubleValue();

        if (z > zThreshold) {
            String msg = String.format(
                    "Giá sản phẩm (%,.0f) chênh bất thường so với median (%,.0f) ± MAD (%,.0f) [z=%.2f > %.2f]",
                    currentPrice.doubleValue(), median.doubleValue(), mad.doubleValue(),
                    z, zThreshold);
            log.warn("[PriceAnomaly] SOFT_FLAG product id={} z={} median={} mad={}",
                    productId, z, median, mad);
            return AutoCheckVerdict.softFlag(FLAG_CODE, msg);
        }

        return AutoCheckVerdict.pass();
    }

    // ======================== Math helpers ========================

    static BigDecimal computeMedian(Collection<BigDecimal> values) {
        if (values == null || values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        BigDecimal a = sorted.get(n / 2 - 1);
        BigDecimal b = sorted.get(n / 2);
        return a.add(b).divide(BigDecimal.valueOf(2));
    }

    /**
     * MAD = median(|xi - median|).
     * Trả về epsilon nếu data degenerate.
     */
    static BigDecimal computeMad(Collection<BigDecimal> values, BigDecimal median) {
        if (values == null || values.isEmpty() || median == null) {
            return BigDecimal.ZERO;
        }
        List<BigDecimal> deviations = new ArrayList<>();
        for (BigDecimal v : values) {
            deviations.add(v.subtract(median).abs());
        }
        return computeMedian(deviations);
    }
}
