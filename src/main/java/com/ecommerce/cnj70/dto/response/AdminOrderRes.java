package com.ecommerce.cnj70.dto.response;

import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderRes {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String shippingAddress;
    private List<ShopGroup> shops;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private boolean paid;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deliveredAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopGroup {
        private String shopId;
        private String shopName;
        private List<OrderItemRes> items;
        private BigDecimal shopSubtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRes {
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }

    public static AdminOrderRes fromOrder(com.ecommerce.cnj70.document.Order order,
                                          Map<String, String> shopNameMap) {
        if (order == null) return null;

        List<ShopGroup> shopGroups = new ArrayList<>();
        if (order.getItems() != null) {
            Map<String, List<com.ecommerce.cnj70.document.Order.OrderItem>> itemsByShop =
                    order.getItems().stream()
                            .collect(java.util.stream.Collectors.groupingBy(
                                    item -> item.getShopId() != null ? item.getShopId() : "unknown"
                            ));

            for (Map.Entry<String, List<com.ecommerce.cnj70.document.Order.OrderItem>> entry : itemsByShop.entrySet()) {
                String shopId = entry.getKey();
                List<com.ecommerce.cnj70.document.Order.OrderItem> shopItems = entry.getValue();

                List<OrderItemRes> itemResList = shopItems.stream()
                        .map(item -> OrderItemRes.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .imageUrl(item.getImageUrl())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .subtotal(item.getSubtotal())
                                .build())
                        .toList();

                BigDecimal shopSubtotal = shopItems.stream()
                        .map(com.ecommerce.cnj70.document.Order.OrderItem::getSubtotal)
                        .filter(java.util.Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                shopGroups.add(ShopGroup.builder()
                        .shopId(shopId)
                        .shopName(shopNameMap.getOrDefault(shopId, "Shop " + shopId.substring(0, Math.min(8, shopId.length()))))
                        .items(itemResList)
                        .shopSubtotal(shopSubtotal)
                        .build());
            }
        }

        return AdminOrderRes.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .userName(order.getUserName())
                .userEmail(order.getUserEmail())
                .userPhone(order.getUserPhone())
                .shippingAddress(order.getShippingAddress())
                .shops(shopGroups)
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paid(order.isPaid())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .build();
    }
}
