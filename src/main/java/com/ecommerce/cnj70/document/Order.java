package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
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
import java.util.List;

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
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }
}
