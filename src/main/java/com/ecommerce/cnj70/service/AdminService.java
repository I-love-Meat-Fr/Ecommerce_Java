package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.response.AdminDashboardRes;

public interface AdminService {
    AdminDashboardRes getDashboardStats();

    /**
     * Dashboard stats with a revenue-trend period filter.
     * Supported periods: DAY, WEEK, MONTH, QUARTER, YEAR, ALL.
     * Other fields (totalUsers, totalShops, totalProducts, totalOrders,
     * totalPlatformRevenue, recentActivities) are NOT affected by period —
     * they reflect all-time data per existing business definition.
     */
    AdminDashboardRes getDashboardStatsByPeriod(String period);
}
