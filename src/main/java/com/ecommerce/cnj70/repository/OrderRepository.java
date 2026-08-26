package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.enums.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByUserId(String userId);

    List<Order> findByShopId(String shopId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByShopIdAndStatus(String shopId, OrderStatus status);

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);

    List<Order> findByShopIdOrderByCreatedAtDesc(String shopId);

    List<Order> findByShopIdAndStatusIn(String shopId, List<OrderStatus> statuses);

    long countByShopId(String shopId);
}
