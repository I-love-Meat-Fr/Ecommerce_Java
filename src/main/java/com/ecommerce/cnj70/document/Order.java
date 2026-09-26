package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.PaymentStatus;
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

    /**
     * Số tiền được giảm bởi voucher (server-calculated).
     * 0 nếu không áp dụng voucher.
     */
    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    /**
     * ID voucher đã áp dụng (nếu có). Null = không có voucher.
     * Lưu riêng để truy vết + tránh trùng sử dụng.
     */
    private String voucherId;

    /**
     * Mã voucher đã áp dụng (snapshot lúc checkout, không đổi khi voucher bị sửa).
     */
    private String voucherCode;

    /**
     * Tên voucher (snapshot để hiển thị trong Order history).
     */
    private String voucherName;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    private PaymentMethod paymentMethod;

    /**
     * Trạng thái thanh toán của Order — tách biệt khỏi {@link #status} (workflow đơn hàng)
     * và {@code shippingByShop[*].status} (vận chuyển theo shop).
     *
     * <p>Lifecycle: PENDING → PAID / FAILED → REFUNDED (optional, terminal).</p>
     *
     * <p>Field {@link #paid} được giữ để tương thích ngược với code cũ — sẽ được sync
     * theo {@code paymentStatus == PAID} trong service layer.</p>
     */
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Legacy boolean flag — được giữ để tương thích ngược. Mọi code mới phải dùng
     * {@link #paymentStatus} thay thế. Service layer đảm bảo
     * {@code paid == (paymentStatus == PAID)} tại mọi thời điểm.
     */
    @Builder.Default
    private boolean paid = false;

    /**
     * Thời điểm thanh toán thành công (paymentStatus chuyển sang PAID).
     * Null nếu chưa thanh toán. Dùng để hiển thị "Đã thanh toán lúc HH:mm dd/MM/yyyy"
     * trên UI.
     */
    private LocalDateTime paidAt;

    /**
     * Thời điểm hoàn tiền (paymentStatus chuyển sang REFUNDED).
     * Null nếu chưa hoàn tiền.
     */
    private LocalDateTime refundedAt;

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
     *
     * <p>Mỗi shop có trạng thái vận chuyển + phí ship + tracking riêng vì vendor tự xử lý
     * phần của mình. Phí ship được phân bổ từ {@link Order#getShippingFee()} tổng khi tạo
     * Order (chia đều cho các shop, shop cuối nhận phần dư để tổng luôn khớp).</p>
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
        /** Phí vận chuyển được phân bổ cho sub-order của shop này. */
        @Builder.Default
        private BigDecimal shippingFee = BigDecimal.ZERO;
        private String trackingNumber;
        private String carrier;
        private LocalDateTime shippedAt;
        private LocalDateTime deliveredAt;
        private String failureReason;
        private String note;
        private LocalDateTime updatedAt;
    }
}
