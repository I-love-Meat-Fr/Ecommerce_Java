package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.dto.response.AdminOrderRes;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        return listOrders(pageable, q, null);
    }

    @Override
    public Page<Order> listOrders(Pageable pageable, String q, OrderStatus status) {
        boolean hasQ = (q != null && !q.isBlank());
        boolean hasStatus = (status != null);

        // Case 1: no status, no q
        if (!hasStatus && !hasQ) {
            return orderRepository.findAll(pageable);
        }

        // Case 2: status only
        if (hasStatus && !hasQ) {
            return orderRepository.findByStatus(status, pageable);
        }

        // Case 3: q only (Phase 6 behavior preserved)
        if (!hasStatus) {
            return orderRepository.findByUserNameContainingIgnoreCaseOrUserIdContaining(q, q, pageable);
        }

        // Case 4: status + q → status AND (userName OR userId)
        String trimmed = q.trim();
        Page<Order> byName = orderRepository.findByStatusAndUserNameContainingIgnoreCase(status, trimmed, pageable);
        Page<Order> byUserId = orderRepository.findByStatusAndUserIdContainingIgnoreCase(status, trimmed, pageable);

        // Merge by id, sort by createdAt desc
        Map<String, Order> merged = new LinkedHashMap<>();
        for (Order o : byName.getContent()) merged.putIfAbsent(o.getId(), o);
        for (Order o : byUserId.getContent()) merged.putIfAbsent(o.getId(), o);

        List<Order> sorted = new ArrayList<>(merged.values());
        sorted.sort(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        long total = byName.getTotalElements() + byUserId.getTotalElements();
        // Approximate total; actual content size capped by union count
        return new PageImpl<>(sorted, pageable, Math.max(total, sorted.size()));
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
