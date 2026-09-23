package com.ecommerce.cnj70.dto.response;

import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.ShippingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorOrderRes {

    private String orderId;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String shippingAddress;
    private List<OrderItemRes> items;
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
    public static class OrderItemRes {
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
        /** Shipping status riêng cho phần của shop này */
        private ShippingStatusRes shippingStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingStatusRes {
        private ShippingStatus status;
        private String trackingNumber;
        private String carrier;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
        private String failureReason;
    }
}
