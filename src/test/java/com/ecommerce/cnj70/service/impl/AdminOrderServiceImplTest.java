package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 6 — Unit test cho AdminOrderServiceImpl.
 *
 * Verify rule:
 *  - getOrderById: raw mongoTemplate query; not found → throw.
 *  - getOrderById(null/blank) → throw.
 *  - listOrders(pageable, q, status): search/filter logic preserved.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminOrderService - Phase 6 unit test")
class AdminOrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;
    @Mock private FindIterable<Document> findIterable;
    @Mock private MongoConverter mongoConverter;

    @InjectMocks private AdminOrderServiceImpl adminOrderService;

    @BeforeEach
    void setUp() {
        lenient().when(mongoTemplate.getCollection("orders")).thenReturn(mongoCollection);
        lenient().when(mongoCollection.find(any(Document.class))).thenReturn(findIterable);
        lenient().when(findIterable.first()).thenReturn(null);
        lenient().when(mongoTemplate.getConverter()).thenReturn(mongoConverter);
    }

    private Order validOrder() {
        return Order.builder()
                .id("o-1")
                .userId("u-1")
                .userName("Alice")
                .userEmail("alice@example.com")
                .totalAmount(new BigDecimal("100000"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void stubRawGetOrderById(Order order) {
        Document raw = new Document("_id", order.getId())
                .append("userId", order.getUserId())
                .append("userName", order.getUserName())
                .append("userEmail", order.getUserEmail())
                .append("totalAmount", order.getTotalAmount())
                .append("status", order.getStatus() != null ? order.getStatus().name() : null)
                .append("createdAt", order.getCreatedAt() != null
                        ? order.getCreatedAt().toString() : null);
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoConverter.read(eq(Order.class), eq(raw))).thenReturn(order);
    }

    @Test
    void getOrderById_existing_returnsOrder() {
        Order order = validOrder();
        stubRawGetOrderById(order);

        Order result = adminOrderService.getOrderById("o-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("o-1");
        assertThat(result.getUserName()).isEqualTo("Alice");
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void getOrderById_notFound_throws() {
        // Default stub returns null → not found.
        assertThatThrownBy(() -> adminOrderService.getOrderById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrderById_blank_throws() {
        assertThatThrownBy(() -> adminOrderService.getOrderById(""))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> adminOrderService.getOrderById(null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listOrders_noFilter_callsFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Order order = validOrder();
        Page<Order> expected = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findAll(pageable)).thenReturn(expected);

        Page<Order> result = adminOrderService.listOrders(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void listOrders_statusOnly_callsFindByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Order order = validOrder();
        Page<Order> expected = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findByStatus(OrderStatus.PENDING, pageable)).thenReturn(expected);

        Page<Order> result = adminOrderService.listOrders(pageable, null, OrderStatus.PENDING);

        assertThat(result.getContent()).hasSize(1);
    }
}
