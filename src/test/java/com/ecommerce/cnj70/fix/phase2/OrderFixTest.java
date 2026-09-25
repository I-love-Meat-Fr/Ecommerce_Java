package com.ecommerce.cnj70.fix.phase2;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Order.OrderItem;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.dto.response.AdminOrderRes;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.impl.AdminOrderServiceImpl;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho Phase 2 — Order Fix Contract (§10).
 *
 * <p>Verify rules:</p>
 * <ul>
 *   <li>{@code getOrderById}: raw Document query với String {@code _id} (Phase 6 fix).</li>
 *   <li>{@code getOrderById}: invalid id (blank/null) → throw ResourceNotFoundException.</li>
 *   <li>{@code getOrderById}: not found → throw ResourceNotFoundException.</li>
 *   <li>{@code toAdminOrderRes}: bind items, address, total, shops grouping.</li>
 *   <li>{@code listOrders}: status filter → findByStatus; null status → findAll.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminOrderService - Phase 2 fix verification")
class OrderFixTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;

    @InjectMocks private AdminOrderServiceImpl adminOrderService;

    @BeforeEach
    void setUp() {
        // Default: raw Document query returns null → ResourceNotFoundException
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("orders")).thenReturn(mongoCollection);
    }

    @SuppressWarnings("unchecked")
    private void stubRawGetOrderById(Order order) {
        Document raw = new Document("_id", order.getId())
                .append("userId", order.getUserId() != null ? order.getUserId() : "")
                .append("status", order.getStatus() != null ? order.getStatus().name() : null)
                .append("totalAmount", order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);

        FindIterable<Document> findIterable = mock(FindIterable.class);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoTemplate.getConverter()).thenReturn(
                mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        lenient().when(mongoTemplate.getConverter().read(eq(Order.class), eq(raw))).thenReturn(order);
    }

    private Order buildSampleOrder() {
        OrderItem item1 = OrderItem.builder()
                .productId("prod-1")
                .productName("Sản phẩm A")
                .quantity(2)
                .price(new BigDecimal("50000"))
                .subtotal(new BigDecimal("100000"))
                .shopId("shop-1")
                .build();
        OrderItem item2 = OrderItem.builder()
                .productId("prod-2")
                .productName("Sản phẩm B")
                .quantity(1)
                .price(new BigDecimal("30000"))
                .subtotal(new BigDecimal("30000"))
                .shopId("shop-1")
                .build();

        return Order.builder()
                .id("order-1")
                .userId("user-1")
                .userName("Nguyễn Văn A")
                .userEmail("a@example.com")
                .userPhone("0123456789")
                .shippingAddress("123 Nguyễn Trãi, Thanh Xuân, Hà Nội")
                .paymentMethod(PaymentMethod.COD)
                .status(OrderStatus.PENDING)
                .subtotal(new BigDecimal("130000"))
                .shippingFee(new BigDecimal("20000"))
                .totalAmount(new BigDecimal("150000"))
                .paid(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>(List.of(item1, item2)))
                .build();
    }

    // ============ §10.2 — getOrderById String _id lookup ============

    @Test
    @DisplayName("getOrderById: valid String _id → raw Document query returns Order")
    void getOrderById_validId_returnsOrder() {
        Order order = buildSampleOrder();
        stubRawGetOrderById(order);

        Order found = adminOrderService.getOrderById("order-1");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo("order-1");
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.getTotalAmount()).isEqualByComparingTo("150000");
    }

    @Test
    @DisplayName("getOrderById: invalid id (blank) → ResourceNotFoundException")
    void getOrderById_blankId_throws() {
        assertThatThrownBy(() -> adminOrderService.getOrderById(""))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderById: invalid id (null) → ResourceNotFoundException")
    void getOrderById_nullId_throws() {
        assertThatThrownBy(() -> adminOrderService.getOrderById(null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getOrderById: not found in MongoDB → ResourceNotFoundException")
    void getOrderById_notFound_throws() {
        // Default: raw query returns null

        assertThatThrownBy(() -> adminOrderService.getOrderById("nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ §10.3 — toAdminOrderRes binding ============

    @Test
    @DisplayName("toAdminOrderRes: valid Order → AdminOrderRes bind đầy đủ items, address, total")
    void toAdminOrderRes_validOrder_bindsAllFields() {
        Order order = buildSampleOrder();
        // ShopNameMap: empty (no shops in repository)
        when(shopRepository.findAll()).thenReturn(new ArrayList<>());

        AdminOrderRes res = adminOrderService.toAdminOrderRes(order);

        assertThat(res).isNotNull();
        assertThat(res.getId()).isEqualTo("order-1");
        assertThat(res.getUserId()).isEqualTo("user-1");
        assertThat(res.getUserName()).isEqualTo("Nguyễn Văn A");
        assertThat(res.getUserEmail()).isEqualTo("a@example.com");
        assertThat(res.getUserPhone()).isEqualTo("0123456789");
        assertThat(res.getShippingAddress()).isEqualTo("123 Nguyễn Trãi, Thanh Xuân, Hà Nội");
        assertThat(res.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(res.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(res.getSubtotal()).isEqualByComparingTo("130000");
        assertThat(res.getShippingFee()).isEqualByComparingTo("20000");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("150000");
        assertThat(res.isPaid()).isFalse();
        // Items grouped by shop
        assertThat(res.getShops()).hasSize(1);
        assertThat(res.getShops().get(0).getItems()).hasSize(2);
        assertThat(res.getShops().get(0).getItems().get(0).getProductName()).isEqualTo("Sản phẩm A");
        assertThat(res.getShops().get(0).getShopSubtotal()).isEqualByComparingTo("130000");
    }

    @Test
    @DisplayName("toAdminOrderRes: shop name map lookup — shops trong DB sẽ dùng shopName thực")
    void toAdminOrderRes_shopNameResolved() {
        Order order = buildSampleOrder();
        Shop shop = Shop.builder()
                .id("shop-1")
                .shopName("Cửa hàng ABC")
                .build();
        when(shopRepository.findAll()).thenReturn(List.of(shop));

        AdminOrderRes res = adminOrderService.toAdminOrderRes(order);

        assertThat(res.getShops()).hasSize(1);
        assertThat(res.getShops().get(0).getShopName()).isEqualTo("Cửa hàng ABC");
    }

    // ============ §10.2 — listOrders ============

    @Test
    @DisplayName("listOrders: status=null → gọi findAll")
    void listOrders_noFilter_callsFindAll() {
        Page<Order> expected = new PageImpl<>(List.of(buildSampleOrder()));
        when(orderRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(expected);

        Page<Order> result = adminOrderService.listOrders(PageRequest.of(0, 10), null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("listOrders: status=PENDING → gọi findByStatus")
    void listOrders_filterByStatus_callsFindByStatus() {
        Page<Order> expected = new PageImpl<>(List.of(buildSampleOrder()));
        when(orderRepository.findByStatus(eq(OrderStatus.PENDING), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(expected);

        Page<Order> result = adminOrderService.listOrders(
                PageRequest.of(0, 10), null, OrderStatus.PENDING);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByStatus(eq(OrderStatus.PENDING), any(org.springframework.data.domain.Pageable.class));
    }
}
