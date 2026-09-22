package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.ShippingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    @Indexed
    private String userId;

    private String userName;

    private String userEmail;

    private String userPhone;

    private String shippingAddress;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal subtotal;

    private BigDecimal shippingFee;

    private BigDecimal totalAmount;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private PaymentMethod paymentMethod;

    @Builder.Default
    private boolean paid = false;

    private String shopId;

    private String shopName;

    /**
     * Shipping info per shop (key = shopId, value = SubOrderShipping).
     * Mỗi shop có trạng thái vận chuyển riêng vì vendor tự xử lý phần của mình.
     */
    @Builder.Default
    private Map<String, SubOrderShipping> shippingByShop = new HashMap<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private LocalDateTime deliveredAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String shopId;
        private String shopName;
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }

    /**
     * Thông tin vận chuyển cho phần của 1 shop trong đơn hàng (sub-order).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubOrderShipping {
        private String shopId;
        private String shopName;
        @Builder.Default
        private ShippingStatus status = ShippingStatus.PENDING;
        private String trackingNumber;
        private String carrier;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
        private String failureReason;
        private String note;
        private LocalDateTime updatedAt;
    }
}
