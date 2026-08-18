package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.*;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    
    @Override
    public Order createOrder(String userId, CheckoutReq request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        
        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }
        
        List<Order.OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        
        for (Cart.CartItem cartItem : cart.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found: " + cartItem.getProductId()));
            
            Order.OrderItem orderItem = Order.OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .imageUrl(product.getThumbnailUrl())
                    .price(cartItem.getPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(cartItem.getSubtotal())
                    .build();
            
            orderItems.add(orderItem);
            subtotal = subtotal.add(cartItem.getSubtotal());
            
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }
        
        BigDecimal shippingFee = BigDecimal.valueOf(15000);
        BigDecimal totalAmount = subtotal.add(shippingFee);
        
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
                .shopId(cart.getItems().get(0).getProductId())
                .build();
        
        Order savedOrder = orderRepository.save(order);
        
        cartService.clearCart(userId);
        
        return savedOrder;
    }
    
    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }
    
    @Override
    public List<Order> getOrdersByUserId(String userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Override
    public List<Order> getOrdersByShopId(String shopId) {
        return orderRepository.findByShopIdOrderByCreatedAtDesc(shopId);
    }
    
    @Override
    public void updateOrderStatus(String orderId, OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        
        if (status == OrderStatus.DELIVERED) {
            order.setDeliveredAt(java.time.LocalDateTime.now());
        }
        
        orderRepository.save(order);
    }
    
    @Override
    public void cancelOrder(String orderId) {
        Order order = getOrderById(orderId);
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("Cannot cancel order in current status");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
    }
}
