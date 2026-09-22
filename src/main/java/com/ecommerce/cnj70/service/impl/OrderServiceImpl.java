package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.*;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShippingStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final ShopRepository shopRepository;
    private final CartService cartService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(String userId, CheckoutReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Không tìm thấy giỏ hàng"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống");
        }

        List<Order.OrderItem> orderItems = new ArrayList<>();
        Map<String, String> shopNames = new HashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (Cart.CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + cartItem.getProductId()));

            // Inventory rule: chỉ bán sản phẩm có trạng thái ACTIVE
            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BadRequestException(String.format(
                        "Sản phẩm '%s' không thể mua (trạng thái: %s).",
                        product.getName(),
                        product.getStatus() != null ? product.getStatus().name() : "UNKNOWN"));
            }

            // Inventory rule: kiểm tra shop đang hoạt động và đã được xác minh
            Shop shop = shopRepository.findById(product.getShopId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng: " + product.getShopId()));
            if (!shop.isActive()) {
                throw new BadRequestException(String.format(
                        "Cửa hàng '%s' hiện không hoạt động. Không thể đặt hàng.",
                        shop.getShopName()));
            }
            if (!shop.isVerified() || shop.getStatus() != ShopStatus.APPROVED) {
                throw new BadRequestException(String.format(
                        "Cửa hàng '%s' chưa được xác minh. Không thể đặt hàng.",
                        shop.getShopName()));
            }

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BadRequestException(String.format(
                        "Sản phẩm '%s' không đủ hàng. Chỉ còn %d sản phẩm.",
                        product.getName(), product.getStock()));
            }

            Order.OrderItem orderItem = Order.OrderItem.builder()
                    .shopId(product.getShopId())
                    .shopName(product.getShopName())
                    .productId(product.getId())
                    .productName(product.getName())
                    .imageUrl(product.getThumbnailUrl())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(cartItem.getSubtotal())
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(cartItem.getSubtotal());
            shopNames.put(product.getShopId(), product.getShopName());

            product.setStock(product.getStock() - cartItem.getQuantity());
            if (product.getStock() < 0) {
                product.setStock(0);
            }
            productRepository.save(product);
        }

        BigDecimal shippingFee = BigDecimal.valueOf(15000);
        BigDecimal totalAmount = subtotal.add(shippingFee);

        // Khởi tạo shipping info cho mỗi shop (sub-order)
        Map<String, Order.SubOrderShipping> shippingByShop = new HashMap<>();
        for (Map.Entry<String, String> entry : shopNames.entrySet()) {
            shippingByShop.put(entry.getKey(), Order.SubOrderShipping.builder()
                    .shopId(entry.getKey())
                    .shopName(entry.getValue())
                    .status(ShippingStatus.PENDING)
                    .build());
        }

        Order order = Order.builder()
                .userId(userId)
                .userName(user.getFullName())
                .userEmail(user.getEmail())
                .userPhone(request.getPhone() != null ? request.getPhone() : user.getPhone())
                .shippingAddress(request.getShippingAddress())
                .items(orderItems)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(totalAmount)
                .status(OrderStatus.PENDING)
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.COD)
                .paid(false)
                .shippingByShop(shippingByShop)
                .build();

        Order savedOrder = orderRepository.save(order);

        cartService.clearCart(userId);

        return savedOrder;
    }

    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));
    }

    @Override
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Order> getOrdersByShopId(String shopId) {
        List<Order> allOrders = orderRepository.findAll();
        return allOrders.stream()
                .filter(order -> order.getItems() != null &&
                        order.getItems().stream()
                                .anyMatch(item -> shopId.equals(item.getShopId())))
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = getOrderById(orderId);
        OrderStatus currentStatus = order.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        if (newStatus == OrderStatus.CANCELLED && currentStatus != OrderStatus.CANCELLED) {
            restoreStock(order);
        }

        orderRepository.save(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(String orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Chỉ có thể hủy đơn hàng đang ở trạng thái CHỜ XÁC NHẬN");
        }

        restoreStock(order);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus next) {
        boolean valid = switch (current) {
            case PENDING -> next == OrderStatus.PREPARING || next == OrderStatus.CANCELLED;
            case PREPARING -> next == OrderStatus.SHIPPING || next == OrderStatus.CANCELLED;
            case SHIPPING -> next == OrderStatus.DELIVERED || next == OrderStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BadRequestException(String.format(
                    "Không thể chuyển từ trạng thái '%s' sang '%s'",
                    current.name(), next.name()));
        }
    }

    private void restoreStock(Order order) {
        if (order.getItems() == null) return;

        for (Order.OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStock(product.getStock() + item.getQuantity());
                productRepository.save(product);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order updateShippingStatus(String orderId, String shopId, ShippingStatus status,
                                     String trackingNumber, String carrier, String note) {
        Order order = getOrderById(orderId);

        if (order.getShippingByShop() == null || !order.getShippingByShop().containsKey(shopId)) {
            throw new BadRequestException("Đơn hàng không chứa sản phẩm của shop này");
        }

        Order.SubOrderShipping shipping = order.getShippingByShop().get(shopId);
        shipping.setStatus(status);
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            shipping.setTrackingNumber(trackingNumber);
        }
        if (carrier != null && !carrier.isBlank()) {
            shipping.setCarrier(carrier);
        }
        if (note != null && !note.isBlank()) {
            shipping.setNote(note);
        }
        shipping.setUpdatedAt(LocalDateTime.now());

        if (status == ShippingStatus.DELIVERED) {
            shipping.setDeliveredAt(LocalDateTime.now());
        }
        if (status == ShippingStatus.PICKED_UP || status == ShippingStatus.IN_TRANSIT) {
            shipping.setShippedAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }
}
