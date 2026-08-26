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
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
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
