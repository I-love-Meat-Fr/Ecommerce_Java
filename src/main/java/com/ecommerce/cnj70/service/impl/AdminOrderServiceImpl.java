package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.dto.response.AdminOrderRes;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;

    @Override
    public Page<Order> listOrders(Pageable pageable) {
        return orderRepository.findAll(pageable);
    }

    @Override
    public Page<Order> listOrders(Pageable pageable, String q) {
        if (q == null || q.isBlank()) {
            return orderRepository.findAll(pageable);
        }
        return orderRepository.findByUserNameContainingIgnoreCaseOrUserIdContaining(
                q, q, pageable);
    }

    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
    }

    @Override
    public AdminOrderRes toAdminOrderRes(Order order) {
        Map<String, String> shopNameMap = buildShopNameMap();
        return AdminOrderRes.fromOrder(order, shopNameMap);
    }

    private Map<String, String> buildShopNameMap() {
        Map<String, String> map = new HashMap<>();
        List<Shop> shops = shopRepository.findAll();
        for (Shop shop : shops) {
            map.put(shop.getId(), shop.getShopName());
        }
        return map;
    }
}
