package com.ecommerce.cnj70.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardRes {

    private long totalUsers;
    private long totalShops;
    private long totalProducts;
    private long totalOrders;
    private BigDecimal totalPlatformRevenue;
    private List<RecentActivity> recentActivities;
    private List<DailyMetric> revenueTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type;
        private String description;
        private String time;
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
