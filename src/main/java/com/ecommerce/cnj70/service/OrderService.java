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
     * Cập nhật shipping status của 1 shop trong đơn hàng (sub-order).
     * Vendor gọi method này để cập nhật trạng thái vận chuyển phần của mình.
     */
    Order updateShippingStatus(String orderId, String shopId, ShippingStatus status,
                               String trackingNumber, String carrier, String note);
}
