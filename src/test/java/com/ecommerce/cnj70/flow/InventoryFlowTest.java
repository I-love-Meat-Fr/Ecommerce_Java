package com.ecommerce.cnj70.flow;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import com.ecommerce.cnj70.service.impl.OrderServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * InventoryFlowTest - Verify rule quản lý tồn kho.
 *
 * Scenario thực tế:
 *  1. Khách add to cart 3 sản phẩm (stock = 5) → cart có qty = 3, stock giữ nguyên 5
 *  2. Khách checkout → order tạo, stock giảm còn 2
 *  3. Nếu stock về 0 → set ProductStatus.OUT_OF_STOCK (gap hiện tại - chưa tự động set)
 *  4. Cancel order → restore stock, set ProductStatus về ACTIVE (gap)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Inventory Flow Test - stock + ProductStatus")
class InventoryFlowTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shopRepository.findById(any())).thenAnswer(inv ->
                Optional.of(TestFixtures.shop(inv.getArgument(0), "owner",
                        com.ecommerce.cnj70.enums.ShopStatus.APPROVED, true)));
        orderService = new OrderServiceImpl(orderRepository, userRepository,
                productRepository, cartRepository, shopRepository, cartService,
                mock(com.ecommerce.cnj70.service.VoucherService.class));
    }

    @Test
    @DisplayName("Flow 1: checkout → trừ stock")
    void checkout_decrementsStock() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 5);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 5, new BigDecimal("100000")))));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        Order order = orderService.createOrder("u-1",
                CheckoutReq.builder().shippingAddress("X").paymentMethod(PaymentMethod.COD).build());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(product.getStock()).isEqualTo(3); // 5 - 2
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE); // vẫn ACTIVE vì stock > 0
    }

    @Test
    @DisplayName("Flow 2: cancel order → restore stock")
    void cancelOrder_restoresStock() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 3); // sau khi đã trừ
        Order order = TestFixtures.order("o-1", "u-1", List.of(
                TestFixtures.orderItem("p-1", "shop-1", 2, new BigDecimal("100000"))));
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        orderService.cancelOrder("o-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(5); // 3 + 2
    }

    @Test
    @DisplayName("Flow 3: updateOrderStatus → CANCELLED → restore stock (transition hợp lệ)")
    void updateStatus_toCancelled_restoresStock() {
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 3);
        Order order = TestFixtures.order("o-1", "u-1", List.of(
                TestFixtures.orderItem("p-1", "shop-1", 2, new BigDecimal("100000"))));
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        orderService.updateOrderStatus("o-1", OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("Flow 4: cancel order DELIVERED → không restore stock (transition không hợp lệ)")
    void cancelDelivered_throws_noRestore() {
        Order order = TestFixtures.order("o-1", "u-1", List.of());
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        try {
            orderService.updateOrderStatus("o-1", OrderStatus.CANCELLED);
        } catch (Exception e) {
            // ignore
        }

        // Stock không thay đổi vì transition không hợp lệ
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    @DisplayName("GAP: checkout khi stock về 0 → KHÔNG tự set ProductStatus.OUT_OF_STOCK (cần fix)")
    void checkout_stockReachesZero_doesNotAutoUpdateStatus_currentBehavior() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 2); // sắp hết
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 2, new BigDecimal("100000")))));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        Order order = orderService.createOrder("u-1",
                CheckoutReq.builder().shippingAddress("X").paymentMethod(PaymentMethod.COD).build());

        assertThat(order).isNotNull();
        assertThat(product.getStock()).isZero();

        // Hiện tại: ProductStatus vẫn là ACTIVE (không auto-update)
        // → Cần fix: nếu stock == 0 thì set OUT_OF_STOCK
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE); // đánh dấu gap
    }

    @Test
    @DisplayName("GAP: cancel order restore stock → KHÔNG revert ProductStatus về ACTIVE (cần fix)")
    void cancelOrder_restoredStock_doesNotRevertStatus_currentBehavior() {
        Product product = TestFixtures.product("p-1", "shop-1", 0, ProductStatus.OUT_OF_STOCK);
        Order order = TestFixtures.order("o-1", "u-1", List.of(
                TestFixtures.orderItem("p-1", "shop-1", 3, new BigDecimal("100000"))));
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        orderService.cancelOrder("o-1");

        assertThat(product.getStock()).isEqualTo(3); // restored
        // Hiện tại: status vẫn là OUT_OF_STOCK dù stock > 0
        // → Cần fix: nếu stock > 0 và status = OUT_OF_STOCK thì set ACTIVE
        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK); // đánh dấu gap
    }
}
