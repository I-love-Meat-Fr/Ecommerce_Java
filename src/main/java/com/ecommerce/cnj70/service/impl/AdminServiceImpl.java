package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.dto.response.AdminDashboardRes.DailyMetric;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements com.ecommerce.cnj70.service.AdminService {
    
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    
    @Override
    public AdminDashboardRes getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalShops = shopRepository.count();
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        
        long activeVendors = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.VENDOR)
                .count();
        
        long activeProducts = productRepository.findByStatus(ProductStatus.ACTIVE).size();
        
        List<Order> deliveredOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .toList();
        
        BigDecimal totalRevenue = deliveredOrders.stream()
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Order> recentOrders = orderRepository.findAll().stream()
                .sorted((a, b) -> {
                    LocalDateTime aTime = a.getCreatedAt() != null ? a.getCreatedAt() : LocalDateTime.MIN;
                    LocalDateTime bTime = b.getCreatedAt() != null ? b.getCreatedAt() : LocalDateTime.MIN;
                    return bTime.compareTo(aTime);
                })
                .limit(5)
                .toList();
        
        DateTimeFormatter dayLabel = DateTimeFormatter.ofPattern("EEE", new Locale("vi"));
        
        List<DailyMetric> revenueTrend = new ArrayList<>();
        Map<LocalDate, BigDecimal> revByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> orderByDay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            revByDay.put(d, BigDecimal.ZERO);
            orderByDay.put(d, 0L);
        }
        for (Order order : orderRepository.findAll()) {
            if (order.getCreatedAt() == null) continue;
            LocalDate d = order.getCreatedAt().toLocalDate();
            if (!revByDay.containsKey(d)) continue;
            orderByDay.merge(d, 1L, Long::sum);
            if (order.getStatus() == OrderStatus.DELIVERED && order.getTotalAmount() != null) {
                revByDay.merge(d, order.getTotalAmount(), BigDecimal::add);
            }
        }
        for (Map.Entry<LocalDate, BigDecimal> e : revByDay.entrySet()) {
            revenueTrend.add(DailyMetric.builder()
                    .label(dayLabel.format(e.getKey()))
                    .revenue(e.getValue())
                    .orders(orderByDay.getOrDefault(e.getKey(), 0L))
                    .build());
        }
        
        List<AdminDashboardRes.RecentActivity> activities = new ArrayList<>();
        for (Order order : recentOrders) {
            if (order.getCreatedAt() != null) {
                long minutesAgo = ChronoUnit.MINUTES.between(order.getCreatedAt(), LocalDateTime.now());
                String timeAgo;
                if (minutesAgo < 60) {
                    timeAgo = minutesAgo + " phút trước";
                } else if (minutesAgo < 1440) {
                    timeAgo = (minutesAgo / 60) + " giờ trước";
                } else {
                    timeAgo = (minutesAgo / 1440) + " ngày trước";
                }
                
                activities.add(AdminDashboardRes.RecentActivity.builder()
                        .type("ORDER")
                        .description("Đơn hàng mới #" + order.getId().substring(0, Math.min(8, order.getId().length())) + " - " + order.getStatus())
                        .time(timeAgo)
                        .build());
            }
        }
        
        return AdminDashboardRes.builder()
                .totalUsers(totalUsers)
                .totalShops(totalShops)
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .activeVendors(activeVendors)
                .activeProducts(activeProducts)
                .totalPlatformRevenue(totalRevenue)
                .recentActivities(activities)
                .revenueTrend(revenueTrend)
                .build();
    }
}
