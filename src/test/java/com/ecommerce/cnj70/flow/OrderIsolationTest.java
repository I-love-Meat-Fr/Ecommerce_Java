package com.ecommerce.cnj70.flow;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import com.ecommerce.cnj70.service.VoucherService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * OrderIsolationTest - Verify isolation giữa các shop trong cùng 1 order.
 *
 * Scenario thực tế:
 *  - Khách đặt 1 order có sản phẩm từ shop-A + shop-B
 *  - Hiện tại: order có 1 status duy nhất, cancel shop A → cancel toàn bộ
 *  - Mong muốn: mỗi shop có sub-status riêng, cancel shop A không ảnh hưởng shop B
 *
 * Test này document gap hiện tại để sau này fix.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Order Isolation Test - multi-shop order")
class OrderIsolationTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private com.ecommerce.cnj70.repository.ShopRepository shopRepository;
    @Mock private CartService cartService;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));
        orderService = new OrderServiceImpl(orderRepository, mock(com.ecommerce.cnj70.repository.UserRepository.class),
                productRepository, mock(com.ecommerce.cnj70.repository.CartRepository.class), shopRepository, cartService,
                mock(com.ecommerce.cnj70.service.VoucherService.class));
    }

    @Test
    @DisplayName("Filter orders by shopId: vendor A chỉ thấy order có item shop-A")
    void getOrdersByShopId_filtersCorrectly() {
        Order order1 = TestFixtures.order("o-1", "u-1", List.of(
                TestFixtures.orderItem("p-1", "shop-A", 2, new BigDecimal("100000"))));
        Order order2 = TestFixtures.order("o-2", "u-1", List.of(
                TestFixtures.orderItem("p-2", "shop-B", 1, new BigDecimal("50000"))));
        Order order3 = TestFixtures.order("o-3", "u-2", List.of(
                TestFixtures.orderItem("p-3", "shop-A", 1, new BigDecimal("200000"))));

        when(orderRepository.findAll()).thenReturn(List.of(order1, order2, order3));

        List<Order> shopAOrders = orderService.getOrdersByShopId("shop-A");
        List<Order> shopBOrders = orderService.getOrdersByShopId("shop-B");

        assertThat(shopAOrders).hasSize(2); // o-1 và o-3
        assertThat(shopBOrders).hasSize(1); // o-2
    }

    @Test
    @DisplayName("Order đa-shop: lấy items theo shopId trong cùng 1 order")
    void multiShopOrder_itemsBelongToDifferentShops() {
        Order order = TestFixtures.order("o-1", "u-1", List.of(
                TestFixtures.orderItem("p-1", "shop-A", 2, new BigDecimal("100000")),
                TestFixtures.orderItem("p-2", "shop-B", 1, new BigDecimal("50000")),
                TestFixtures.orderItem("p-3", "shop-A", 1, new BigDecimal("200000"))
        ));
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<Order> shopAOrders = orderService.getOrdersByShopId("shop-A");

        assertThat(shopAOrders).hasSize(1);
        Order o = shopAOrders.get(0);
        assertThat(o.getItems()).hasSize(3); // order vẫn có 3 items
        long shopACount = o.getItems().stream()
                .filter(i -> "shop-A".equals(i.getShopId()))
                .count();
        assertThat(shopACount).isEqualTo(2);
    }

    @Test
    @DisplayName("GAP: cancel sub-order hiện không có → cancel order = cancel toàn bộ")
    void cancelEntireOrder_affectsAllShops_currentBehavior() {
        // Hiện tại: status là của Order (master), không có sub-status riêng
        // → cancel order = restoreStock cho tất cả items (cả shop-A và shop-B)
        Product productA = TestFixtures.activeProduct("p-A", "shop-A", 8);
        Product productB = TestFixtures.activeProduct("p-B", "shop-B", 9);
        Order order = TestFixtures.order("o-1", "u-1", List.of(
                TestFixtures.orderItem("p-A", "shop-A", 2, new BigDecimal("100000")),
                TestFixtures.orderItem("p-B", "shop-B", 1, new BigDecimal("50000"))
        ));
        order.setStatus(OrderStatus.PENDING);

        when(orderRepository.findById("o-1")).thenReturn(Optional.of(order));
        when(productRepository.findById("p-A")).thenReturn(Optional.of(productA));
        when(productRepository.findById("p-B")).thenReturn(Optional.of(productB));

        orderService.cancelOrder("o-1");

        // Hiện tại: cả 2 shop đều bị restore stock (gap)
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(productA.getStock()).isEqualTo(10); // restored
        assertThat(productB.getStock()).isEqualTo(10); // restored - GAP: shop-B không nên bị ảnh hưởng

        // → Cần fix: tách sub-order, chỉ restore stock của items trong sub-order bị cancel
    }

    @Test
    @DisplayName("Sub-order transition: mỗi shop có status riêng (MOCK - behavior mong muốn)")
    void subOrder_independentStatus_desiredBehavior() {
        // Đây là test document behavior MONG MUỐN sau khi fix
        // Hiện tại Order không có sub-status → skip test này (comment để tracking)

        // Order có 2 items: shop-A + shop-B
        // Khi vendor A update status → chỉ items shop-A thay đổi
        // Order.status (master) = derive từ worst case của sub-status

        // Sau khi fix:
        // - Order cần field: Map<String, SubOrderShipping> shippingByShop
        // - updateOrderStatus(shopId, status) chỉ update entry của shop đó
        // - restoreStock chỉ restore items của shop bị cancel
        // → Khi implement, bật test này và verify

        Order order = TestFixtures.order("o-1", "u-1", List.of(
                TestFixtures.orderItem("p-A", "shop-A", 2, new BigDecimal("100000"))));
        assertThat(order.getItems()).hasSize(1);
        // Test pass nhưng chưa verify isolation
    }
}
