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
 * Unit test cho {@link ForbiddenCategoryCheck} + {@link ForbiddenCategoryPolicy}
 * (task #7.0).
 *
 * <p>Covers:
 * <ol>
 *   <li>Category hợp lệ → PASS.</li>
 *   <li>Forbidden category → HARD_REJECT với FORBIDDEN_CAT.</li>
 *   <li>Không có category → không crash → PASS.</li>
 *   <li>Config rỗng → PASS.</li>
 *   <li>id() trả về đúng constant.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ForbiddenCategory - unit test")
class ForbiddenCategoryCheckTest {

    private ForbiddenCategoryPolicy policy;
    private ForbiddenCategoryCheck check;

    @BeforeEach
    void setUp() {
        policy = ForbiddenCategoryPolicy.forTest(List.of("cat-forbidden-1", "cat-forbidden-2"));
        check = new ForbiddenCategoryCheck(policy);
    }

    private AutoCheckContext ctxFor(Product product) {
        return AutoCheckContext.builder()
                .product(product)
                .sharedState(new HashMap<>())
                .build();
    }

    private Product productWithCategory(String categoryId) {
        return Product.builder()
                .id("p-1")
                .name("Test")
                .price(new BigDecimal("100"))
                .categoryId(categoryId)
                .shopId("s-1")
                .build();
    }

    @Test
    @DisplayName("Category hợp lệ (không trong blacklist) → PASS")
    void validCategory_pass() {
        Product p = productWithCategory("cat-ok");
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Forbidden category → HARD_REJECT với FORBIDDEN_CAT")
    void forbiddenCategory_hardReject() {
        Product p = productWithCategory("cat-forbidden-1");
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isHardReject()).isTrue();
        assertThat(v.getFlagCode()).isEqualTo("FORBIDDEN_CAT");
        assertThat(v.getMessage()).contains("cat-forbidden-1");
    }

    @Test
    @DisplayName("Null category → PASS (không crash)")
    void nullCategory_pass() {
        Product p = productWithCategory(null);
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Empty category → PASS")
    void emptyCategory_pass() {
        Product p = productWithCategory("   ");
        AutoCheckVerdict v = check.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Config rỗng → PASS mọi category")
    void emptyConfig_allPass() {
        ForbiddenCategoryPolicy empty = ForbiddenCategoryPolicy.forTest(List.of());
        ForbiddenCategoryCheck chk = new ForbiddenCategoryCheck(empty);
        Product p = productWithCategory("any-id");
        AutoCheckVerdict v = chk.check(ctxFor(p));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("Null config + null product → PASS")
    void nullConfig_nullProduct_pass() {
        AutoCheckVerdict v = check.check(ctxFor(null));
        assertThat(v.isPass()).isTrue();
    }

    @Test
    @DisplayName("id() = 'FORBIDDEN_CATEGORY'")
    void id_returnsConstant() {
        assertThat(check.id()).isEqualTo("FORBIDDEN_CATEGORY");
    }

    @Test
    @DisplayName("Policy.isForbiddenAny() — hỗ trợ collection nhiều ID")
    void policy_isForbiddenAny() {
        // Trả về true nếu có ít nhất 1 ID forbidden.
        assertThat(policy.isForbiddenAny(List.of("cat-ok", "cat-forbidden-2"))).isTrue();
        assertThat(policy.isForbiddenAny(List.of("cat-ok-1", "cat-ok-2"))).isFalse();
        assertThat(policy.isForbiddenAny(null)).isFalse();
        assertThat(policy.isForbiddenAny(List.of())).isFalse();
    }
}
