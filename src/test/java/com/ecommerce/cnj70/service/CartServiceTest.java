package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.impl.CartServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit test cho CartServiceImpl.
 *
 * Verify rule:
 *  - addToCart: maxStock clamp không vượt quá stock
 *  - updateCartItem: quantity <= 0 → remove
 *  - addToCart: product không tồn tại → throw
 *
 * NOTE: Hiện tại CartServiceImpl chưa check ProductStatus (HIDDEN/DRAFT vẫn add được).
 * Test này chỉ verify behavior hiện tại + đánh dấu gap (xem test cuối).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CartService - unit test")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private ProductRepository productRepository;

    @InjectMocks private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("addToCart: sản phẩm mới → thêm item, subtotal = price * quantity")
    void addToCart_newItem_addsToCart() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(
                TestFixtures.emptyCart("u-1")));

        Cart result = cartService.addToCart("u-1", "p-1", 3);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);
        assertThat(result.getItems().get(0).getSubtotal())
                .isEqualByComparingTo(new BigDecimal("300000")); // 100000 * 3
    }

    @Test
    @DisplayName("addToCart: sản phẩm đã có trong cart → cộng dồn quantity, không vượt stock")
    void addToCart_existingItem_incrementsAndClampsToStock() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 5);
        Cart existing = TestFixtures.cartWith("u-1", new ArrayList<>(java.util.List.of(
                TestFixtures.cartItem("p-1", "shop-1", 3, 5, new BigDecimal("100000")))));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(existing));

        // Thêm 5 vào (đã có 3, tổng 8 > stock 5) → clamp về 5
        Cart result = cartService.addToCart("u-1", "p-1", 5);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(result.getItems().get(0).getSubtotal())
                .isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    @DisplayName("addToCart: product không tồn tại → throw RuntimeException")
    void addToCart_productNotFound_throws() {
        when(productRepository.findById("p-x")).thenReturn(Optional.empty());
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(
                TestFixtures.emptyCart("u-1")));

        assertThatThrownBy(() -> cartService.addToCart("u-1", "p-x", 1))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("updateCartItem: quantity <= 0 → remove item khỏi cart")
    void updateCartItem_zeroOrNegative_removesItem() {
        Cart existing = TestFixtures.cartWith("u-1", new ArrayList<>(java.util.List.of(
                TestFixtures.cartItem("p-1", "shop-1", 3, 5, new BigDecimal("100000")))));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(existing));

        Cart result = cartService.updateCartItem("u-1", "p-1", 0);

        assertThat(result.getItems()).isEmpty();
    }

    @Test
    @DisplayName("updateCartItem: quantity > stock → clamp về stock")
    void updateCartItem_exceedsStock_clampsToStock() {
        Cart existing = TestFixtures.cartWith("u-1", new ArrayList<>(java.util.List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 3, new BigDecimal("100000")))));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(existing));

        Cart result = cartService.updateCartItem("u-1", "p-1", 100);

        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("clearCart: xóa hết items")
    void clearCart_emptiesCart() {
        Cart existing = TestFixtures.cartWith("u-1", new ArrayList<>(java.util.List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 5, new BigDecimal("100000")),
                TestFixtures.cartItem("p-2", "shop-1", 1, 5, new BigDecimal("100000"))
        )));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(existing));

        cartService.clearCart("u-1");

        assertThat(existing.getItems()).isEmpty();
    }

    @Test
    @DisplayName("GAP (cần fix): addToCart không check ProductStatus - sản phẩm HIDDEN vẫn add được")
    void addToCart_doesNotCheckProductStatus_currentBehavior() {
        // Hiện tại CartServiceImpl KHÔNG check status → đây là bug cần fix ở task Inventory
        // Test này document behavior hiện tại để khi fix sẽ thấy test fail
        Product hidden = TestFixtures.hiddenProduct("p-1", "shop-1", 5);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(hidden));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(
                TestFixtures.emptyCart("u-1")));

        // Hiện tại KHÔNG throw (gap)
        Cart result = cartService.addToCart("u-1", "p-1", 1);

        assertThat(result.getItems()).hasSize(1);
        assertThat(hidden.getStatus()).isEqualTo(ProductStatus.HIDDEN); // đã đánh dấu gap
    }
}
