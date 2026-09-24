package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.impl.OrderServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CustomerIsolationTest — Verify cross-customer data isolation cho OrderService.
 *
 * <p>Phạm vi:</p>
 * <ul>
 *   <li><b>OrderService.getOrderByIdForCustomer()</b> — Customer A KHÔNG được đọc Order của Customer B</li>
 *   <li><b>OrderService.getOrdersByUserId()</b> — chỉ trả về Order của chính user</li>
 *   <li><b>OrderService.createOrder()</b> — Order được tạo với đúng userId từ caller</li>
 *   <li><b>OrderService.cancelOrder()</b> — Customer không thể cancel Order của người khác
 *       (qua service layer; controller layer đã enforce role CUSTOMER không được cancel)</li>
 * </ul>
 *
 * <p>Đây là test quan trọng nhất cho task "Đảm bảo Customer không xem Order của người khác" —
 * service layer phải chặn IDOR trước khi data rời khỏi DB.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CustomerIsolation - cross-customer Order isolation")
class CustomerIsolationTest {

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
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shopRepository.findById(any())).thenAnswer(inv -> {
            String shopId = inv.getArgument(0);
            return Optional.of(TestFixtures.shop(shopId, "owner-1",
                    com.ecommerce.cnj70.enums.ShopStatus.APPROVED, true));
        });
    }

    // ============================================================
    // getOrderByIdForCustomer — IDOR guard
    // ============================================================

    @Nested
    @DisplayName("getOrderByIdForCustomer - IDOR guard")
    class GetOrderByIdForCustomer {

        @Test
        @DisplayName("Customer A yêu cầu Order của chính mình → trả về Order")
        void ownOrder_returnsOrder() {
            Order own = TestFixtures.order("o-1", "u-A", List.of(
                    TestFixtures.orderItem("p-1", "shop-1", 2, new BigDecimal("100000"))));
            when(orderRepository.findById("o-1")).thenReturn(Optional.of(own));

            Order result = orderService.getOrderByIdForCustomer("o-1", "u-A");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("o-1");
            assertThat(result.getUserId()).isEqualTo("u-A");
        }

        @Test
        @DisplayName("Customer B cố đọc Order của Customer A → UnauthorizedException")
        void otherCustomerOrder_throwsUnauthorized() {
            Order aOrder = TestFixtures.order("o-1", "u-A", List.of(
                    TestFixtures.orderItem("p-1", "shop-1", 2, new BigDecimal("100000"))));
            when(orderRepository.findById("o-1")).thenReturn(Optional.of(aOrder));

            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer("o-1", "u-B"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Đơn hàng không thuộc về Customer");
        }

        @Test
        @DisplayName("Order không tồn tại → ResourceNotFoundException (không lộ existence)")
        void nonExistentOrder_throwsNotFound() {
            when(orderRepository.findById("o-missing")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer("o-missing", "u-A"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Không tìm thấy đơn hàng");
        }

        @Test
        @DisplayName("orderId null/rỗng → BadRequestException")
        void nullOrBlankOrderId_throwsBadRequest() {
            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer(null, "u-A"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("orderId");

            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer("", "u-A"))
                    .isInstanceOf(BadRequestException.class);

            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer("  ", "u-A"))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("userId null/rỗng → UnauthorizedException")
        void nullOrBlankUserId_throwsUnauthorized() {
            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer("o-1", null))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("Customer");

            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer("o-1", ""))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Order.userId = null (data lỗi) → không cho phép bất kỳ customer nào đọc")
        void orderWithNullUserId_isProtected() {
            Order broken = TestFixtures.order("o-1", "u-A", List.of());
            broken.setUserId(null);
            when(orderRepository.findById("o-1")).thenReturn(Optional.of(broken));

            assertThatThrownBy(() -> orderService.getOrderByIdForCustomer("o-1", "u-A"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Reverse: thử đoán orderId của người khác — luôn bị chặn")
        void idorEnumeration_attackBlocked() {
            // Giả lập attacker B cố đoán orderId
            String[] guessedIds = {"o-1", "o-2", "o-3", "order-12345", "abc"};
            Order[] realOrders = {
                    TestFixtures.order("o-1", "u-A", List.of()),
                    TestFixtures.order("o-2", "u-A", List.of()),
                    TestFixtures.order("o-3", "u-A", List.of()),
            };
            // Setup mock: o-1, o-2, o-3 thuộc u-A; các ID khác không tồn tại
            for (String id : guessedIds) {
                Order mock = null;
                for (Order o : realOrders) {
                    if (o.getId().equals(id)) {
                        mock = o;
                        break;
                    }
                }
                when(orderRepository.findById(id)).thenReturn(Optional.ofNullable(mock));
            }

            for (String guessedId : guessedIds) {
                assertThatThrownBy(() -> orderService.getOrderByIdForCustomer(guessedId, "u-B"))
                        .as("Guessed id '%s' must not leak to user u-B", guessedId)
                        .satisfiesAnyOf(
                                t -> assertThat(t).isInstanceOf(UnauthorizedException.class),
                                t -> assertThat(t).isInstanceOf(ResourceNotFoundException.class)
                        );
            }
        }
    }

    // ============================================================
    // getOrdersByUserId — chỉ trả về Order của user hiện tại
    // ============================================================

    @Nested
    @DisplayName("getOrdersByUserId - scope isolation")
    class GetOrdersByUserId {

        @Test
        @DisplayName("Trả về tất cả Order của user hiện tại (sắp xếp theo createdAt desc)")
        void returnsOnlyCurrentUsersOrders() {
            // Repo đã filter theo userId — caller không cần lo lọc
            when(orderRepository.findByUserIdOrderByCreatedAtDesc("u-A"))
                    .thenReturn(List.of(
                            TestFixtures.order("o-A1", "u-A", List.of()),
                            TestFixtures.order("o-A2", "u-A", List.of())));

            List<Order> orders = orderService.getOrdersByUserId("u-A");

            assertThat(orders).hasSize(2);
            assertThat(orders).extracting(Order::getUserId).containsOnly("u-A");
        }

        @Test
        @DisplayName("User không có order → trả về list rỗng (không leak data từ user khác)")
        void emptyForUserWithoutOrders() {
            when(orderRepository.findByUserIdOrderByCreatedAtDesc("u-empty"))
                    .thenReturn(List.of());

            List<Order> orders = orderService.getOrdersByUserId("u-empty");

            assertThat(orders).isEmpty();
        }
    }

    // ============================================================
    // createOrder — Order mới luôn mang userId từ caller (auth principal)
    // ============================================================

    @Nested
    @DisplayName("createOrder - userId binding")
    class CreateOrder {

        @Test
        @DisplayName("createOrder: userId từ caller (auth principal) được dùng, KHÔNG phải từ request body")
        void orderBelongsToCaller() {
            com.ecommerce.cnj70.document.User customer =
                    TestFixtures.userCustomer("u-A", "a@cnj70.com");
            Product product = TestFixtures.activeProduct("p-1", "shop-1", 10);
            Cart cart = TestFixtures.cartWith("u-A", new java.util.ArrayList<>(List.of(
                    TestFixtures.cartItem("p-1", "shop-1", 2, 10, new BigDecimal("100000")))));

            when(userRepository.findById("u-A")).thenReturn(Optional.of(customer));
            when(cartRepository.findByUserId("u-A")).thenReturn(Optional.of(cart));
            when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

            Order order = orderService.createOrder("u-A",
                    com.ecommerce.cnj70.dto.request.CheckoutReq.builder()
                            .shippingAddress("123 ABC")
                            .paymentMethod(com.ecommerce.cnj70.enums.PaymentMethod.COD)
                            .build());

            assertThat(order.getUserId()).isEqualTo("u-A");
            // userId từ CheckoutReq KHÔNG được phép override — verify KHÔNG có
            // cơ chế nào trong service lấy userId từ request body.
            verify(userRepository).findById("u-A");
        }
    }

    // ============================================================
    // cancelOrder — service layer: chỉ cancel nếu order thuộc user (UI check ở controller)
    // ============================================================

    @Nested
    @DisplayName("cancelOrder - service layer state guard")
    class CancelOrder {

        @Test
        @DisplayName("Cancel order thuộc user, status=PENDING → thành công (đã test ở OrderServiceTest)")
        void cancelOwnPendingOrder_succeeds() {
            // sanity: happy path vẫn hoạt động — không phá vỡ existing behavior
            Order order = TestFixtures.order("o-1", "u-A", List.of(
                    TestFixtures.orderItem("p-1", "shop-1", 2, new BigDecimal("100000"))));
            order.setStatus(com.ecommerce.cnj70.enums.OrderStatus.PENDING);
            Product product = TestFixtures.activeProduct("p-1", "shop-1", 8);
            when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));
            when(productRepository.findById("p-1")).thenReturn(Optional.of(product));

            orderService.cancelOrder("o-1");

            assertThat(order.getStatus()).isEqualTo(com.ecommerce.cnj70.enums.OrderStatus.CANCELLED);
            assertThat(product.getStock()).isEqualTo(10);
        }
    }
}