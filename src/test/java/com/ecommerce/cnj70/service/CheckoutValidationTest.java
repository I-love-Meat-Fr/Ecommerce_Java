package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.checkout.CheckoutValidationRes;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.VoucherType;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.impl.OrderServiceImpl;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link OrderServiceImpl#validateCheckout(String, String)} —
 * Pre-submit validation kiểm tra lại price / stock / shop / voucher từ DB.
 *
 * <p>Verify rule:</p>
 * <ul>
 *   <li>Cart trống → valid=false, có error message</li>
 *   <li>Product đã xóa → invalid, reasonCode = PRODUCT_NOT_FOUND</li>
 *   <li>Product HIDDEN → invalid, reasonCode = INACTIVE_PRODUCT</li>
 *   <li>Stock = 0 → invalid, reasonCode = OUT_OF_STOCK</li>
 *   <li>Stock < qty → invalid, reasonCode = INSUFFICIENT_STOCK</li>
 *   <li>Shop bị SUSPENDED → invalid, reasonCode = SHOP_SUSPENDED</li>
 *   <li>Shop không ACTIVE → invalid, reasonCode = SHOP_INACTIVE</li>
 *   <li>Giá DB khác cart snapshot → priceChanged=true, có warning</li>
 *   <li>Happy path → valid=true, summary đúng với currentPrice</li>
 *   <li>Voucher hết lượt → invalid, voucher.valid=false</li>
 *   <li>Voucher không tồn tại → invalid + errors</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService.validateCheckout - pre-submit validation")
class CheckoutValidationTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private CartService cartService;
    @Mock private VoucherService voucherService;

    @InjectMocks private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        // Default: shop APPROVED + active
        when(shopRepository.findById(any())).thenAnswer(inv -> {
            String shopId = inv.getArgument(0);
            return Optional.of(TestFixtures.shop(shopId, "owner-1",
                    ShopStatus.APPROVED, true));
        });
    }

    // ============ Cart trống ============

    @Test
    @DisplayName("validateCheckout: cart trống → valid=false, error message")
    void validateCheckout_emptyCart_invalid() {
        when(cartRepository.findByUserId("u-1"))
                .thenReturn(Optional.of(TestFixtures.emptyCart("u-1")));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getErrors()).isNotEmpty();
        assertThat(res.getItems()).isEmpty();
        assertThat(res.getSummary().getFinalTotal()).isEqualByComparingTo("15000"); // chỉ còn ship
    }

    // ============ Product deleted / hidden ============

    @Test
    @DisplayName("validateCheckout: product đã xóa khỏi DB → invalid + PRODUCT_NOT_FOUND")
    void validateCheckout_productDeleted_invalid() {
        Product activeProduct = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.empty()); // deleted

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getErrors().get(0)).contains("ngừng bán");
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).getReasonCode()).isEqualTo("PRODUCT_NOT_FOUND");
        assertThat(res.getItems().get(0).isValid()).isFalse();
    }

    @Test
    @DisplayName("validateCheckout: product HIDDEN → invalid + INACTIVE_PRODUCT")
    void validateCheckout_productHidden_invalid() {
        Product hidden = TestFixtures.hiddenProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(hidden));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getItems().get(0).getReasonCode()).isEqualTo("INACTIVE_PRODUCT");
        assertThat(res.getItems().get(0).getProductStatus()).isEqualTo("HIDDEN");
    }

    // ============ Stock checks ============

    @Test
    @DisplayName("validateCheckout: stock = 0 → invalid + OUT_OF_STOCK")
    void validateCheckout_outOfStock_invalid() {
        Product noStock = TestFixtures.activeProduct("p-1", "shop-1", 0);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 5, new BigDecimal("100000")))));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(noStock));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getItems().get(0).getReasonCode()).isEqualTo("OUT_OF_STOCK");
    }

    @Test
    @DisplayName("validateCheckout: stock < qty → invalid + INSUFFICIENT_STOCK")
    void validateCheckout_insufficientStock_invalid() {
        Product lowStock = TestFixtures.activeProduct("p-1", "shop-1", 3);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 5, 3, new BigDecimal("100000")))));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(lowStock));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getItems().get(0).getReasonCode()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(res.getItems().get(0).getCurrentStock()).isEqualTo(3);
        assertThat(res.getItems().get(0).getRequestedQuantity()).isEqualTo(5);
    }

    // ============ Shop status checks ============

    @Test
    @DisplayName("validateCheckout: shop SUSPENDED → invalid + SHOP_SUSPENDED")
    void validateCheckout_shopSuspended_invalid() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));
        Shop suspended = TestFixtures.shop("shop-1", "owner-1", ShopStatus.SUSPENDED, true);

        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(suspended));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getItems().get(0).getReasonCode()).isEqualTo("SHOP_SUSPENDED");
        assertThat(res.getItems().get(0).isShopActive()).isFalse();
    }

    @Test
    @DisplayName("validateCheckout: shop !active → invalid + SHOP_INACTIVE")
    void validateCheckout_shopInactive_invalid() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));
        Shop inactive = TestFixtures.shop("shop-1", "owner-1", ShopStatus.APPROVED, false);

        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(inactive));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isFalse();
        assertThat(res.getItems().get(0).getReasonCode()).isEqualTo("SHOP_INACTIVE");
    }

    // ============ Price drift detection ============

    @Test
    @DisplayName("validateCheckout: giá DB khác cart snapshot → priceChanged=true + warning")
    void validateCheckout_priceDrift_warning() {
        // Cart có price = 100000 nhưng DB hiện tại = 120000
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        product.setPrice(new BigDecimal("120000"));
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isTrue(); // giá tăng không chặn submit (chỉ warning)
        assertThat(res.isChanged()).isTrue();
        assertThat(res.getItems().get(0).isPriceChanged()).isTrue();
        assertThat(res.getItems().get(0).getOldPrice()).isEqualByComparingTo("100000");
        assertThat(res.getItems().get(0).getCurrentPrice()).isEqualByComparingTo("120000");
        assertThat(res.getItems().get(0).getSubtotal()).isEqualByComparingTo("240000"); // dùng currentPrice
        assertThat(res.getSummary().getSubtotal()).isEqualByComparingTo("240000");
        assertThat(res.getSummary().getFinalTotal()).isEqualByComparingTo("255000"); // 240000 + 15000 ship
        assertThat(res.getWarnings()).anyMatch(w -> w.contains("tăng"));
    }

    // ============ Happy path ============

    @Test
    @DisplayName("validateCheckout: happy path → valid=true, summary đúng")
    void validateCheckout_happyPath_valid() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", null);

        assertThat(res.isValid()).isTrue();
        assertThat(res.isChanged()).isFalse();
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).isValid()).isTrue();
        assertThat(res.getItems().get(0).isStockOk()).isTrue();
        assertThat(res.getItems().get(0).isShopActive()).isTrue();
        assertThat(res.getSummary().getSubtotal()).isEqualByComparingTo("200000");
        assertThat(res.getSummary().getShippingFee()).isEqualByComparingTo("15000");
        assertThat(res.getSummary().getFinalTotal()).isEqualByComparingTo("215000");
        assertThat(res.getVoucher()).isNull();
    }

    // ============ Voucher checks ============

    @Test
    @DisplayName("validateCheckout: voucher hợp lệ → áp dụng discount vào summary")
    void validateCheckout_validVoucher_appliesDiscount() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

        Voucher voucher = Voucher.builder()
                .id("v-1")
                .code("SALE10")
                .name("Giảm 10%")
                .type(VoucherType.WEB)
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10"))
                .maxDiscountAmount(new BigDecimal("50000"))
                .minOrderValue(BigDecimal.ZERO)
                .quantity(100)
                .used(0)
                .active(true)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(1))
                .build();

        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(voucherService.validateForCheckout(eq("SALE10"), any(), any()))
                .thenReturn(voucher);

        CheckoutValidationRes res = orderService.validateCheckout("u-1", "SALE10");

        assertThat(res.isValid()).isTrue();
        assertThat(res.getVoucher()).isNotNull();
        assertThat(res.getVoucher().isValid()).isTrue();
        assertThat(res.getVoucher().getCode()).isEqualTo("SALE10");
        // subtotal = 200000, ship = 15000, totalBeforeDiscount = 215000
        // discount = 215000 * 10% = 21500 (cap 50000 → giữ nguyên)
        assertThat(res.getSummary().getDiscount()).isEqualByComparingTo("21500");
        assertThat(res.getSummary().getFinalTotal()).isEqualByComparingTo("193500");
    }

    @Test
    @DisplayName("validateCheckout: voucher không tồn tại → valid=false + error")
    void validateCheckout_voucherNotFound_invalid() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(voucherService.validateForCheckout(eq("GHOST"), any(), any()))
                .thenThrow(new com.ecommerce.cnj70.exception.ResourceNotFoundException("Không tìm thấy voucher với mã: GHOST"));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", "GHOST");

        assertThat(res.isValid()).isFalse();
        assertThat(res.getVoucher()).isNotNull();
        assertThat(res.getVoucher().isValid()).isFalse();
        // ResourceNotFoundException (voucher không tồn tại) được phân loại là VOUCHER_NOT_FOUND
        assertThat(res.getVoucher().getReasonCode()).isEqualTo("VOUCHER_NOT_FOUND");
        assertThat(res.getErrors()).anyMatch(e -> e.contains("Voucher"));
        assertThat(res.getSummary().getDiscount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("validateCheckout: voucher exhausted → invalid + VOUCHER_INVALID")
    void validateCheckout_voucherExhausted_invalid() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));
        when(voucherService.validateForCheckout(eq("USED"), any(), any()))
                .thenThrow(new com.ecommerce.cnj70.exception.BadRequestException("Voucher đã hết lượt sử dụng"));

        CheckoutValidationRes res = orderService.validateCheckout("u-1", "USED");

        assertThat(res.isValid()).isFalse();
        assertThat(res.getVoucher().isValid()).isFalse();
        assertThat(res.getErrors()).anyMatch(e -> e.contains("Voucher không hợp lệ"));
    }
}
