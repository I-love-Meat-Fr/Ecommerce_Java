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

    /**
     * TASK #21 — Kiểm tra Customer đã mua Product chưa.
     * Dùng để xác thực trước khi cho phép Review.
     *
     * Quy tắc "đã mua":
     *  - User phải có Order chứa productId
     *  - Order KHÔNG ở trạng thái CANCELLED
     *
     * @return true nếu đã mua (Order PENDING/PREPARING/SHIPPING/DELIVERED có chứa productId)
     */
    boolean hasUserPurchasedProduct(String userId, String productId);
}
