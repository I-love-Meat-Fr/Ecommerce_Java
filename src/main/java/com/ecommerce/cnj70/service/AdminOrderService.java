package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.response.AdminOrderRes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminOrderService {

    Page<Order> listOrders(Pageable pageable);

    Page<Order> listOrders(Pageable pageable, String q);

    Order getOrderById(String id);

    AdminOrderRes toAdminOrderRes(Order order);
}
