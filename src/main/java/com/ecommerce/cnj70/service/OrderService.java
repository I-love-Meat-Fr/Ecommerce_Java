package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ShippingStatus;

import java.util.List;

public interface OrderService {

    Order createOrder(String userId, CheckoutReq request);

    Order getOrderById(String id);

    List<Order> getOrdersByUserId(String userId);

    List<Order> getOrdersByShopId(String shopId);

    void updateOrderStatus(String orderId, OrderStatus status);

    void cancelOrder(String orderId);

    /**
     * TASK #14/#20/#21 — Kiểm tra Customer đã mua và ĐÃ NHẬN được Product chưa.
     * Điều kiện: Order có productId của user VÀ trạng thái DELIVERED.
     * Chỉ khi đã giao hàng thành công (DELIVERED) mới cho phép Review.
     *
     * @return true nếu Order đã DELIVERED và chứa productId
     */
    boolean hasUserReceivedProduct(String userId, String productId);

    /**
     * TASK #21 (legacy) — Kiểm tra Customer đã mua Product chưa (không tính CANCELLED).
     * Dùng để hiển thị trạng thái mua hàng (chưa giao = vẫn hiển thị "đã mua").
     * KHÔNG dùng cho Review validation — dùng hasUserReceivedProduct thay thế.
     *
     * @return true nếu Order không CANCELLED và chứa productId
     */
    boolean hasUserPurchasedProduct(String userId, String productId);
}
