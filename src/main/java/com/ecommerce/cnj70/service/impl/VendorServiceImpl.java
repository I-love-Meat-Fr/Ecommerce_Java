package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.ShopFormReq;
import com.ecommerce.cnj70.dto.response.VendorDashboardRes;
import com.ecommerce.cnj70.dto.response.VendorProfileRes;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {
    
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    
    @Override
    public User getCurrentVendor(UserDetails userDetails) {
        if (userDetails == null) {
            throw new UnauthorizedException("Vui lòng đăng nhập để tiếp tục");
        }
        
        String email = userDetails.getUsername();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy thông tin người dùng"));
    }
    
    @Override
    public String getShopIdFromUser(UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        if (vendor.getShopId() == null || vendor.getShopId().isBlank()) {
            throw new BadRequestException("Bạn chưa có shop. Vui lòng tạo shop trước.");
        }
        return vendor.getShopId();
    }
    
    @Override
    public Shop getShopByCurrentVendor(UserDetails userDetails) {
        String shopId = getShopIdFromUser(userDetails);
        return shopRepository.findById(shopId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin shop"));
    }
    
    @Override
    public Shop createShop(UserDetails userDetails, ShopFormReq request) {
        User vendor = getCurrentVendor(userDetails);
        
        if (vendor.getShopId() != null && !vendor.getShopId().isBlank()) {
            throw new BadRequestException("Bạn đã có shop. Không thể tạo shop mới.");
        }
        
        if (shopRepository.existsByShopName(request.getShopName())) {
            throw new BadRequestException("Tên shop đã tồn tại. Vui lòng chọn tên khác.");
        }
        
        Shop shop = Shop.builder()
                .ownerId(vendor.getId())
                .shopName(request.getShopName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .bannerUrl(request.getBannerUrl())
                .status(ShopStatus.PENDING)
                .active(true)
                .build();
        
        Shop savedShop = shopRepository.save(shop);
        
        vendor.setShopId(savedShop.getId());
        userRepository.save(vendor);
        
        return savedShop;
    }
    
    @Override
    public Shop updateShop(UserDetails userDetails, ShopFormReq request) {
        Shop shop = getShopByCurrentVendor(userDetails);
        
        if (request.getShopName() != null && !request.getShopName().equals(shop.getShopName())) {
            if (shopRepository.existsByShopName(request.getShopName())) {
                throw new BadRequestException("Tên shop đã tồn tại. Vui lòng chọn tên khác.");
            }
            shop.setShopName(request.getShopName());
        }
        
        if (request.getDescription() != null) {
            shop.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            shop.setLogoUrl(request.getLogoUrl());
        }
        if (request.getBannerUrl() != null) {
            shop.setBannerUrl(request.getBannerUrl());
        }
        
        return shopRepository.save(shop);
    }
    
    @Override
    public VendorProfileRes getVendorProfile(UserDetails userDetails) {
        User vendor = getCurrentVendor(userDetails);
        
        VendorProfileRes.VendorProfileResBuilder builder = VendorProfileRes.builder()
                .id(vendor.getId())
                .email(vendor.getEmail())
                .fullName(vendor.getFullName())
                .phone(vendor.getPhone())
                .address(vendor.getAddress())
                .avatarUrl(vendor.getAvatarUrl())
                .role(vendor.getRole() != null ? vendor.getRole().name() : null)
                .status(vendor.getStatus() != null ? vendor.getStatus().name() : null)
                .createdAt(vendor.getCreatedAt());
        
        if (vendor.getShopId() != null) {
            Shop shop = shopRepository.findById(vendor.getShopId()).orElse(null);
            if (shop != null) {
                builder.shop(VendorProfileRes.ShopInfo.builder()
                        .id(shop.getId())
                        .shopName(shop.getShopName())
                        .description(shop.getDescription())
                        .logoUrl(shop.getLogoUrl())
                        .bannerUrl(shop.getBannerUrl())
                        .verified(shop.isVerified())
                        .active(shop.isActive())
                        .createdAt(shop.getCreatedAt())
                        .build());
            }
        }
        
        return builder.build();
    }
    
    @Override
    public VendorDashboardRes getDashboardStats(UserDetails userDetails) {
        String shopId;
        Shop shop = null;
        
        try {
            shopId = getShopIdFromUser(userDetails);
            shop = getShopByCurrentVendor(userDetails);
        } catch (BadRequestException e) {
            return VendorDashboardRes.builder()
                    .totalProducts(0)
                    .outOfStockProducts(0)
                    .totalOrders(0)
                    .pendingOrders(0)
                    .processingOrders(0)
                    .completedOrders(0)
                    .cancelledOrders(0)
                    .totalRevenue(BigDecimal.ZERO)
                    .monthlyRevenue(BigDecimal.ZERO)
                    .build();
        }
        
        List<Product> products = productRepository.findByShopId(shopId);
        int totalProducts = products.size();
        int outOfStockProducts = (int) products.stream()
                .filter(p -> p.getStock() <= 0)
                .count();
        
        List<Order> orders = orderRepository.findByShopIdOrderByCreatedAtDesc(shopId);
        int totalOrders = orders.size();
        int pendingOrders = countOrdersByStatus(orders, OrderStatus.PENDING);
        int processingOrders = countOrdersByStatus(orders, OrderStatus.PREPARING);
        int completedOrders = countOrdersByStatus(orders, OrderStatus.DELIVERED);
        int cancelledOrders = countOrdersByStatus(orders, OrderStatus.CANCELLED);
        
        BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        
        BigDecimal monthlyRevenue = orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .filter(o -> o.getCreatedAt() != null && 
                           (o.getCreatedAt().isAfter(startOfMonth) || o.getCreatedAt().isEqual(startOfMonth)) &&
                           (o.getCreatedAt().isBefore(endOfMonth) || o.getCreatedAt().isEqual(endOfMonth)))
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<LocalDate, BigDecimal> revByDay = new LinkedHashMap<>();
        Map<LocalDate, Long> orderByDay = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = LocalDate.now().minusDays(i);
            revByDay.put(d, BigDecimal.ZERO);
            orderByDay.put(d, 0L);
        }
        for (Order o : orders) {
            if (o.getCreatedAt() == null) continue;
            LocalDate d = o.getCreatedAt().toLocalDate();
            if (!revByDay.containsKey(d)) continue;
            orderByDay.merge(d, 1L, Long::sum);
            if (o.getStatus() == OrderStatus.DELIVERED && o.getTotalAmount() != null) {
                revByDay.merge(d, o.getTotalAmount(), BigDecimal::add);
            }
        }
        List<VendorDashboardRes.DailyMetric> revenueTrend = new ArrayList<>();
        for (Map.Entry<LocalDate, BigDecimal> e : revByDay.entrySet()) {
            revenueTrend.add(VendorDashboardRes.DailyMetric.builder()
                    .label(e.getKey().getDayOfWeek().getDisplayName(TextStyle.SHORT, new Locale("vi")).toUpperCase())
                    .revenue(e.getValue())
                    .orders(orderByDay.getOrDefault(e.getKey(), 0L))
                    .build());
        }
        
        VendorDashboardRes.VendorDashboardResBuilder builder = VendorDashboardRes.builder()
                .totalProducts(totalProducts)
                .outOfStockProducts(outOfStockProducts)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .monthlyRevenue(monthlyRevenue)
                .revenueTrend(revenueTrend);
        
        if (shop != null) {
            builder.shopSummary(VendorDashboardRes.ShopSummary.builder()
                    .shopId(shop.getId())
                    .shopName(shop.getShopName())
                    .logoUrl(shop.getLogoUrl())
                    .verified(shop.isVerified())
                    .active(shop.isActive())
                    .createdAt(shop.getCreatedAt())
                    .build());
        }
        
        return builder.build();
    }
    
    @Override
    public void validateShopOwnership(String shopId, UserDetails userDetails) {
        String userShopId = getShopIdFromUser(userDetails);
        if (!userShopId.equals(shopId)) {
            throw new UnauthorizedException("Bạn không có quyền truy cập shop này");
        }
    }
    
    @Override
    public void validateProductOwnership(String productId, UserDetails userDetails) {
        String shopId = getShopIdFromUser(userDetails);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        
        if (!shopId.equals(product.getShopId())) {
            throw new UnauthorizedException("Bạn không có quyền sửa/xóa sản phẩm này");
        }
    }
    
    @Override
    public boolean isShopOwner(String shopId, UserDetails userDetails) {
        try {
            String userShopId = getShopIdFromUser(userDetails);
            return userShopId.equals(shopId);
        } catch (Exception e) {
            return false;
        }
    }
    
    private int countOrdersByStatus(List<Order> orders, OrderStatus status) {
        return (int) orders.stream()
                .filter(o -> o.getStatus() == status)
                .count();
    }
}
