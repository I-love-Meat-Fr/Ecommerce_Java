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

/**
 * TASK #5 — Order document cho Multi-vendor marketplace.
 *
 * <h3>Cấu trúc Master Order + SubOrder</h3>
 * <p>Trong marketplace này, một Order duy nhất có thể chứa sản phẩm từ NHIỀU Shop.
 * Để xử lý:</p>
 * <ul>
 *   <li><b>Master Order</b> = {@link Order} document này — đại diện cho toàn bộ đơn hàng
 *       của Customer. Có {@link #status} (OrderStatus), paymentStatus, totalAmount, ...</li>
 *   <li><b>SubOrder</b> = mỗi entry trong {@link #shippingByShop} map — đại diện cho
 *       phần đơn hàng thuộc về 1 Shop cụ thể. Có trạng thái vận chuyển riêng
 *       (ShippingStatus), phí ship riêng, tracking riêng.</li>
 * </ul>
 *
 * <h3>Quan hệ Customer → Order → Shop</h3>
 * <pre>
 *   Customer
 *      │
 *      ▼ (1:N)
 *   Order (Master)              ← document này
 *      │
 *      ├─ items: List&lt;OrderItem&gt;         ← có shopId ở mỗi item
 *      │
 *      ├─ shopId (primary)       ← shop đầu tiên trong cart (legacy, giữ để tương thích)
 *      │
 *      └─ shippingByShop: Map    ← SubOrder theo từng shop
 *           shopId → SubOrderShipping {
 *              shopId, shopName,
 *              status (ShippingStatus — DELIVERED/PENDING/...),
 *              shippingFee, trackingNumber, carrier, ...
 *           }
 * </pre>
 *
 * <h3>Tại sao KHÔNG tách SubOrder thành collection riêng?</h3>
 * <ul>
 *   <li>Đồ án đã chốt: 1 Order = 1 document với shippingByShop map → đơn giản cho truy vấn.</li>
 *   <li>Mỗi Order KHÔNG BAO GIỜ có quá nhiều Shop (thực tế 1-3 shop, hiếm khi &gt;5).</li>
 *   <li>Truy vấn theo shop vẫn có thể dùng index trên {@code items.shopId} ở collection orders.</li>
 *   <li>Nếu sau này cần scale: có thể migrate sang {@code orders} + {@code sub_orders}
 *       collection riêng mà không phải đổi API nhiều (chỉ thay cách query).</li>
 * </ul>
 *
 * <h3>Master Order vs SubOrder status</h3>
 * <ul>
 *   <li><b>Master Order status</b> ({@link #status} = OrderStatus): trạng thái tổng của đơn.
 *       Hiện tại dùng status của SubOrder đầu tiên — KHÔNG khuyến khích suy diễn.</li>
 *   <li><b>SubOrder status</b> ({@link SubOrderShipping#getStatus()} = ShippingStatus):
 *       trạng thái vận chuyển riêng của từng shop.</li>
 * </ul>
 *
 * <h3>Revenue calculation (CHO ADMIN DASHBOARD)</h3>
 * <ul>
 *   <li><b>GMV</b>: sum(Order.totalAmount) where status=DELIVERED — toàn bộ đơn.</li>
 *   <li><b>Vendor Sales</b>: sum(SubOrderShipping.shippingFee đảo ngược? No) — cần
 *       xem lại logic; hiện tại dùng GMV × (1 - commissionRate) cho mỗi vendor.</li>
 *   <li><b>Platform Revenue</b>: sum(commissionAmount) — phí sàn nhận được.</li>
 * </ul>
 */
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

    /**
     * TASK #9 — Commission amount (phí sàn) tính trên Subtotal khi Order DELIVERED.
     * Null = chưa tính (đơn chưa giao xong).
     * Vendor nhận: subtotal - commissionAmount.
     */
    private BigDecimal commissionAmount;

    /**
     * TASK #9 — Số tiền Vendor thực nhận (= subtotal - commissionAmount).
     * Null = chưa tính.
     */
    private BigDecimal vendorNetAmount;

    /**
     * TASK #9 — CommissionRule ID đã áp dụng cho Order này (audit).
     */
    private String appliedCommissionRuleId;

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
    /**
     * TASK #5 — SubOrder (per-shop) trong Multi-vendor Order.
     *
     * <p>Mỗi SubOrder đại diện cho phần đơn hàng thuộc về 1 Shop cụ thể, bao gồm:</p>
     * <ul>
     *   <li><b>Trạng thái vận chuyển riêng</b> ({@link #status}) — Vendor tự quản lý.</li>
     *   <li><b>Phí ship phân bổ</b> ({@link #shippingFee}) — từ Order.shippingFee tổng,
     *       chia đều cho các shop (shop cuối nhận phần dư để tổng khớp).</li>
     *   <li><b>Tracking riêng</b> ({@link #trackingNumber}, {@link #carrier}) — mỗi
     *       ĐVVC khác nhau có thể có code riêng.</li>
     *   <li><b>Timeline riêng</b> ({@link #shippedAt}, {@link #deliveredAt}) — cho
     *       audit trail theo shop.</li>
     * </ul>
     *
     * <p>SubOrder KHÔNG có status tổng (PENDING/PREPARING/SHIPPING/DELIVERED) của Master —
     * status Master nằm trên {@link Order#getStatus()} (OrderStatus).</p>
     *
     * <p><b>Workflow điển hình:</b></p>
     * <pre>
     *   SubOrderShipping PENDING  →  (vendor prepare)  →  PICKED_UP/IN_TRANSIT
     *                              →  DELIVERED          (customer nhận hàng)
     *                              →  FAILED             (giao fail, hoàn về vendor)
     * </pre>
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
