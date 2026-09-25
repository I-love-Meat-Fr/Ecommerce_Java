package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.review.VerifiedPurchaseResult;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.service.VerifiedPurchaseGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Phase 2B — Order-based Verified Purchase Gateway.
 *
 * <p>Reads from existing {@code Order} collection. Verified Purchase
 * definition (Phase 2B §19):</p>
 *
 * <pre>
 *   Order exists where
 *     - userId     == review.userId
 *     - item.productId == review.productId
 *     - status     == DELIVERED
 *     - deliveredAt != null
 * </pre>
 *
 * <p>Phase 2B §43 forbids fake production data; this implementation
 * queries the real Order collection via {@code OrderRepository}.</p>
 */
@Slf4j
@Component
@Profile("verified-purchase")
@RequiredArgsConstructor
public class OrderBasedVerifiedPurchaseGateway implements VerifiedPurchaseGateway {

    private final OrderRepository orderRepository;

    @Override
    public VerifiedPurchaseResult check(String userId, String productId) {
        if (userId == null || userId.isBlank() || productId == null || productId.isBlank()) {
            return VerifiedPurchaseResult.notVerified();
        }
        List<Order> orders = orderRepository.findByUserId(userId);
        for (Order order : orders) {
            if (order.getStatus() != OrderStatus.DELIVERED) {
                continue;
            }
            if (order.getDeliveredAt() == null) {
                continue;
            }
            if (order.getItems() == null) {
                continue;
            }
            for (Order.OrderItem item : order.getItems()) {
                if (productId.equals(item.getProductId())) {
                    return new VerifiedPurchaseResult(
                            true,
                            order.getId(),
                            item.getProductId(),
                            order.getDeliveredAt().toString());
                }
            }
        }
        return VerifiedPurchaseResult.notVerified();
    }
}
