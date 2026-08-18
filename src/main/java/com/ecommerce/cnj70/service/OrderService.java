package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.OrderStatus;

import java.util.List;

public interface OrderService {
    
    Order createOrder(String userId, CheckoutReq request);
    
    Order getOrderById(String id);
    
    List<Order> getOrdersByUserId(String userId);
    
    List<Order> getOrdersByShopId(String shopId);
    
    void updateOrderStatus(String orderId, OrderStatus status);
    
    void cancelOrder(String orderId);
}
