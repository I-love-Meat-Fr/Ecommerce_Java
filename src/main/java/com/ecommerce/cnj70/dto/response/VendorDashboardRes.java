package com.ecommerce.cnj70.dto.response;

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
public class VendorDashboardRes {

    private int totalProducts;
    private int outOfStockProducts;
    private int totalOrders;
    private int pendingOrders;
    private int processingOrders;
    private int completedOrders;
    private int cancelledOrders;

    /**
     * <b>Vendor Sales</b> — doanh số bán hàng của vendor (sum of OrderItem.subtotal cho
     * Order DELIVERED của shop này). Đã sửa từ "totalRevenue" cũ (gộp cả shipping &amp;
     * voucher discount) để khớp với định nghĩa Vendor Sales ở Admin dashboard.
     * <p>KHÔNG gồm phí ship và KHÔNG trừ voucher (vendor không "thu" ship, voucher là
     * khuyến mãi do vendor tự cấp).</p>
     */
    private BigDecimal vendorSales;

    /**
     * <b>Vendor Sales (tháng này)</b> — tương tự vendorSales nhưng giới hạn trong tháng hiện tại.
     */
    private BigDecimal monthlyVendorSales;

    /**
     * <b>Vendor Payable</b> — số tiền vendor thực nhận (= subtotal − commission). Hiện
     * Order.vendorNetAmount chưa wire nên giá trị này là {@code null} → UI hiển thị N/A.
     * <p>KHÁC Vendor Sales: Vendor Sales = doanh số bán; Vendor Payable = tiền vendor được nhận.</p>
     */
    private BigDecimal vendorPayable;

    private ShopSummary shopSummary;
    private List<TopProduct> topProducts;
    private List<DailyMetric> revenueTrend;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShopSummary {
        private String shopId;
        private String shopName;
        private String logoUrl;
        private boolean verified;
        private boolean active;
        private LocalDateTime createdAt;
        /** Số khách hàng duy nhất đã mua từ shop này */
        private long uniqueCustomers;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private String productId;
        private String productName;
        private String imageUrl;
        private int orderCount;
        private BigDecimal revenue;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetric {
        private String label;
        private BigDecimal revenue;
        private long orders;
    }
}
