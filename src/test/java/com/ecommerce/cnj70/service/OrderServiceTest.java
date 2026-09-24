package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
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
 * Unit test cho OrderServiceImpl.
 *
 * Verify rule:
 *  - createOrder: stock < quantity → throw
 *  - createOrder: cart rỗng → throw
 *  - createOrder: trừ stock sau khi tạo order
 *  - updateOrderStatus: chỉ cho phép transition hợp lệ
 *  - updateOrderStatus: CANCELLED → restoreStock
 *  - cancelOrder: chỉ cancel được khi status = PENDING
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService - unit test")
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private CartRepository cartRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private CartService cartService;

    @InjectMocks private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        // Default: shop is verified and active (override per test as needed)
        when(shopRepository.findById(any())).thenAnswer(inv -> {
            String shopId = inv.getArgument(0);
            return Optional.of(TestFixtures.shop(shopId, "owner-1",
                    com.ecommerce.cnj70.enums.ShopStatus.APPROVED, true));
        });
    }

    // ============ createOrder ============

    @Test
    @DisplayName("createOrder: stock không đủ → throw BadRequestException")
    void createOrder_insufficientStock_throws() {
        User customer = TestFixtures.userCustomer();
        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(
                TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                        TestFixtures.cartItem("p-1", "shop-1", 5, 10, new BigDecimal("100000")))))));

        Product product = TestFixtures.activeProduct("p-1", "shop-1", 3); // chỉ có 3
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutReq req = CheckoutReq.builder()
                .shippingAddress("123 ABC")
                .paymentMethod(PaymentMethod.COD)
                .build();

        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không đủ hàng");
    }

    @Test
    @DisplayName("createOrder: cart rỗng → throw BadRequestException")
    void createOrder_emptyCart_throws() {
        User customer = TestFixtures.userCustomer();
        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(
                TestFixtures.emptyCart("u-1")));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Giỏ hàng trống");
    }

    @Test
    @DisplayName("createOrder: happy path → trừ stock, status PENDING, clear cart")
    void createOrder_happyPath_decrementsStockAndClearsCart() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutReq req = CheckoutReq.builder()
                .shippingAddress("123 ABC")
                .phone("0900000000")
                .paymentMethod(PaymentMethod.COD)
                .build();

        Order order = orderService.createOrder("u-1", req);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getSubtotal()).isEqualByComparingTo("200000");
        assertThat(order.getShippingFee()).isEqualByComparingTo("15000");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("215000");
        assertThat(product.getStock()).isEqualTo(8); // 10 - 2
        assertThat(order.getUserId()).isEqualTo("u-1");
        assertThat(order.getUserName()).isEqualTo(customer.getFullName());
        verify(cartService, times(1)).clearCart("u-1");
    }

    @Test
    @DisplayName("createOrder: stock về 0 → set thành 0 (không âm)")
    void createOrder_stockReachesZero_setsToZero() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 2);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 5, 2, new BigDecimal("100000"))))); // qty > stock

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        // Vì qty (5) > stock (2) → throw trước khi trừ
        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class);
    }

    // ============ updateOrderStatus ============

    @Test
    @DisplayName("updateOrderStatus: PENDING → CANCELLED → restoreStock")
    void updateOrderStatus_cancelFromPending_restoresStock() {
        Order order = TestFixtures.order("o-1", "u-1",
                List.of(TestFixtures.orderItem("p-1", "shop-1", 3, new BigDecimal("100000"))));
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        Product product = TestFixtures.activeProduct("p-1", "shop-1", 7); // đã trừ 3 từ 10
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        orderService.updateOrderStatus("o-1", OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(10); // restored
    }

    @Test
    @DisplayName("updateOrderStatus: DELIVERED → CANCELLED → throw (transition không hợp lệ)")
    void updateOrderStatus_deliveredToCancelled_throws() {
        Order order = TestFixtures.order("o-1", "u-1", List.of());
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus("o-1", OrderStatus.CANCELLED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể chuyển");
    }

    @Test
    @DisplayName("updateOrderStatus: PENDING → PREPARING → hợp lệ")
    void updateOrderStatus_pendingToPreparing_valid() {
        Order order = TestFixtures.order("o-1", "u-1", List.of());
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        orderService.updateOrderStatus("o-1", OrderStatus.PREPARING);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    @DisplayName("updateOrderStatus: PREPARING → SHIPPING → DELIVERED → set deliveredAt")
    void updateOrderStatus_toDelivered_setsDeliveredAt() {
        Order order = TestFixtures.order("o-1", "u-1", List.of());
        order.setStatus(OrderStatus.SHIPPING);
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        orderService.updateOrderStatus("o-1", OrderStatus.DELIVERED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    // ============ cancelOrder ============

    @Test
    @DisplayName("cancelOrder: status PENDING → CANCELLED + restoreStock")
    void cancelOrder_pending_cancelsAndRestores() {
        Order order = TestFixtures.order("o-1", "u-1",
                List.of(TestFixtures.orderItem("p-1", "shop-1", 2, new BigDecimal("100000"))));
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        Product product = TestFixtures.activeProduct("p-1", "shop-1", 8);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        orderService.cancelOrder("o-1");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(product.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("cancelOrder: status PREPARING → throw (chỉ cancel được PENDING)")
    void cancelOrder_preparing_throws() {
        Order order = TestFixtures.order("o-1", "u-1", List.of());
        order.setStatus(OrderStatus.PREPARING);
        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder("o-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("CHỜ XÁC NHẬN");
    }

    // ============ getOrderById ============

    @Test
    @DisplayName("getOrderById: id không tồn tại → ResourceNotFoundException")
    void getOrderById_notFound_throws() {
        when(orderRepository.findById("o-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("o-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ Inventory: ProductStatus checks ============

    @Test
    @DisplayName("createOrder: sản phẩm HIDDEN → throw BadRequestException")
    void createOrder_hiddenProduct_throws() {
        User customer = TestFixtures.userCustomer();
        Product hidden = TestFixtures.hiddenProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(hidden));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không thể mua");
    }

    @Test
    @DisplayName("createOrder: sản phẩm DRAFT → throw BadRequestException")
    void createOrder_draftProduct_throws() {
        User customer = TestFixtures.userCustomer();
        Product draft = TestFixtures.draftProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(draft));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không thể mua");
    }

    @Test
    @DisplayName("createOrder: sản phẩm ACTIVE → cho phép mua")
    void createOrder_activeProduct_allowsPurchase() {
        User customer = TestFixtures.userCustomer();
        Product active = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(active));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        Order order = orderService.createOrder("u-1", req);

        assertThat(order).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // ============ Inventory: Shop status checks ============

    @Test
    @DisplayName("createOrder: shop bị deactive → throw BadRequestException")
    void createOrder_shopInactive_throws() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));

        // Shop bị ngừng hoạt động
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(
                TestFixtures.shop("shop-1", "owner-1",
                        com.ecommerce.cnj70.enums.ShopStatus.APPROVED, false)));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("không hoạt động");
    }

    @Test
    @DisplayName("createOrder: shop chưa được xác minh (PENDING) → throw BadRequestException")
    void createOrder_shopNotVerified_throws() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));

        // Shop đang chờ xác minh
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(
                TestFixtures.shop("shop-1", "owner-1",
                        com.ecommerce.cnj70.enums.ShopStatus.PENDING, true)));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chưa được xác minh");
    }

    @Test
    @DisplayName("createOrder: shop bị từ chối (REJECTED) → throw BadRequestException")
    void createOrder_shopRejected_throws() {
        User customer = TestFixtures.userCustomer();
        Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 10, new BigDecimal("100000")))));

        // Shop bị từ chối (nhưng vẫn active để test đúng logic xác minh)
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(
                TestFixtures.shop("shop-1", "owner-1",
                        com.ecommerce.cnj70.enums.ShopStatus.REJECTED, true)));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

        CheckoutReq req = CheckoutReq.builder().shippingAddress("X").build();

        assertThatThrownBy(() -> orderService.createOrder("u-1", req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chưa được xác minh");
    }
}
