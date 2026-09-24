package com.ecommerce.cnj70.dto.response;

import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentStatus;
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
public class OrderHistoryRes {
    
    private String orderId;
    private LocalDateTime orderDate;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private int totalItemCount;
    private String shopName; // Legacy field - kept for compatibility
    
    // ===== Master Order fields =====
    private String userName;
    private String userPhone;
    private String shippingAddress;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private String voucherCode;
    private String voucherName;
    private String paymentMethod;
    /** Trạng thái thanh toán (PENDING/PAID/FAILED/REFUNDED) — string để render trực tiếp trên UI. */
    private String paymentStatus;
    /** Thời điểm thanh toán thành công (null nếu chưa thanh toán). */
    private java.time.LocalDateTime paidAt;
    /** Thời điểm hoàn tiền (null nếu chưa hoàn tiền). */
    private java.time.LocalDateTime refundedAt;
    /**
     * Legacy boolean flag — được giữ để tương thích ngược. UI mới nên dùng
     * {@link #paymentStatus} để hiển thị chi tiết (PENDING/PAID/FAILED/REFUNDED).
     */
    private boolean paid;
    private LocalDateTime deliveredAt;
    
    // ===== SubOrders grouped by Shop =====
    private List<SubOrderRes> subOrders;
    
    // ===== Product images (all from this order) =====
    private List<String> productImages;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubOrderRes {
        private String shopId;
        private String shopName;
        private List<OrderItemRes> items;
        private int itemCount;
        private BigDecimal subtotal;
        /** Phí vận chuyển được phân bổ cho sub-order này (theo shop). */
        private BigDecimal shippingFee;
        private ShippingStatus shippingStatus;
        private String trackingNumber;
        private String carrier;
        private String note;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRes {
        private String shopId;
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
    }
}
