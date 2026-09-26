package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.service.automation.AutoCheckStrategy.AutoCheckContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link BlacklistKeywordCheck} (task #3.0).
 *
 * <p>Covers:
 * <ol>
 *   <li>Product sạch → PASS.</li>
 *   <li>Title chứa blacklist keyword → HARD_REJECT.</li>
 *   <li>Description chứa keyword → HARD_REJECT.</li>
 *   <li>Case-insensitive (input hoa, config thường).</li>
 *   <li>Empty/null description không crash.</li>
 *   <li>Config rỗng → PASS.</li>
 *   <li>Word boundary: keyword "ma" không match "mama".</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BlacklistKeywordCheck - unit test")
class BlacklistKeywordCheckTest {

    private BlacklistKeywordCheck check;

    @BeforeEach
    void setUp() {
        // Cấu hình mặc định — mỗi test inject lại list qua forTest().
        check = BlacklistKeywordCheck.forTest(List.of("vũ khí", "fake", "lừa đảo"));
    }

    private AutoCheckContext ctxFor(Product product) {
        return AutoCheckContext.builder()
                .product(product)
                .sharedState(new HashMap<>())
                .build();
    }

    private Product cleanProduct() {
        return Product.builder()
                .id("p-1")
                .name("Điện thoại thông minh Galaxy")
                .description("Sản phẩm chính hãng, bảo hành 12 tháng")
                .price(new BigDecimal("5000000"))
                .shopId("s-1")
                .build();
    }

    @Test
    @DisplayName("Product sạch → PASS")
    void cleanProduct_pass() {
        AutoCheckVerdict v = check.check(ctxFor(cleanProduct()));
        assertThat(v.isPass()).isTrue();
        assertThat(v.getFlagCode()).isNull();
    }

    @Test
    @DisplayName("Title chứa blacklist keyword → HARD_REJECT với BLACKLIST_HIT")
    void titleContainsBlacklist_hardReject() {
        Product p = cleanProduct();
        p.setName("Súng vũ khí hàng nặng");
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isHardReject()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("BLACKLIST_HIT");
        assertThat(v.getMessage()).contains("vũ khí");
    }

    @Test
    @DisplayName("Description chứa blacklist keyword → HARD_REJECT")
    void descriptionContainsBlacklist_hardReject() {
        Product p = cleanProduct();
        p.setDescription("Đây là sản phẩm lừa đảo khách hàng");
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isHardReject()).isTrue();
        assertThat(v.getMessage()).contains("lừa đảo");
    }

    @Test
    @DisplayName("Case-insensitive: input HOA, config thường vẫn match")
    void caseInsensitive_match() {
        Product p = cleanProduct();
        p.setName("Hàng FAKE giả");
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isHardReject()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("BLACKLIST_HIT");
    }

    @Test
    @DisplayName("Empty/null description không crash → PASS")
    void nullDescription_pass() {
        Product p = cleanProduct();
        p.setDescription(null);
        p.setRichDescription(null);
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Config rỗng → luôn PASS")
    void emptyConfig_alwaysPass() {
        BlacklistKeywordCheck empty = BlacklistKeywordCheck.forTest(List.of());
        AutoCheckVerdict v = empty.check(ctxFor(cleanProduct()));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Word boundary: keyword 'ma' KHÔNG match 'mama'")
    void wordBoundary_noFalsePositiveOnSubstring() {
        BlacklistKeywordCheck ma = BlacklistKeywordCheck.forTest(List.of("ma"));
        Product p = cleanProduct();
        p.setName("Mama bear baby clothes");
        AutoCheckVerdict v = ma.check(ctxFor(p));
        // "ma" đứng riêng (sau 'M', không phải giữa từ "mama") vẫn match với word boundary.
        // Test thiết kế để xác nhận: substring match (vd 'ma' trong 'mama') KHÔNG bị match.
        // 'mama' chứa 'ma' nhưng ký tự liền kề 'ma' / 'mama' là alpha (không phải non-word),
        // nên pattern \bma\b KHÔNG match 'mama'.
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("id() trả về constant đúng 'BLACKLIST_KEYWORD'")
    void id_returnsConstant() {
        assertThat(check.id()).isEqualTo("BLACKLIST_KEYWORD");
    }

    @Test
    @DisplayName("Null Product → PASS (không crash)")
    void nullProduct_pass() {
        AutoCheckVerdict v = check.check(ctxFor(null));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Null context (defensive) → PASS")
    void nullContext_pass() {
        AutoCheckVerdict v = check.check(null);
        assertThat(v.isPass()).isTrue();
    }
}
