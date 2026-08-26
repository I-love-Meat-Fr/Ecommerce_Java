package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ShopController {

    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReviewRepository reviewRepository;
    private final CategoryRepository categoryRepository;

    private static final int PAGE_SIZE = 20;

    @GetMapping("/shop/{id}")
    public String shopDetail(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priceRange,
            @RequestParam(required = false, defaultValue = "1") int page,
            Model model) {

        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy cửa hàng"));

        // ===== Sort =====
        Sort sortOrder;
        switch (sort) {
            case "price-asc":
                sortOrder = Sort.by(Sort.Direction.ASC, "price");
                break;
            case "price-desc":
                sortOrder = Sort.by(Sort.Direction.DESC, "price");
                break;
            case "rating":
                sortOrder = Sort.by(Sort.Direction.DESC, "rating");
                break;
            case "best-selling":
                sortOrder = Sort.by(Sort.Direction.DESC, "reviewCount");
                break;
            default:
                sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        // ===== Lấy tất cả sản phẩm ACTIVE của shop để tính stats =====
        List<Product> allShopProducts = productRepository.findByShopIdAndStatus(id, ProductStatus.ACTIVE);
        List<String> productIds = allShopProducts.stream().map(Product::getId).toList();

        // ===== Lấy đơn hàng của shop để tính tổng sold =====
        List<Order> shopOrders = orderRepository.findByShopIdOrderByCreatedAtDesc(id);
        long totalSold = shopOrders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .flatMap(o -> o.getItems().stream())
                .filter(it -> id.equals(it.getShopId()) || productIds.contains(it.getProductId()))
                .mapToInt(it -> it.getQuantity())
                .sum();

        // ===== Reviews gần đây của shop =====
        List<Review> shopReviews = productIds.isEmpty()
                ? new ArrayList<>()
                : reviewRepository.findByProductIdInOrderByCreatedAtDesc(productIds);

        // ===== Đếm review + tính rating trung bình =====
        double avgRating = allShopProducts.stream()
                .filter(p -> p.getRating() > 0)
                .mapToDouble(Product::getRating)
                .average()
                .orElse(0.0);

        int totalReviewCount = allShopProducts.stream()
                .mapToInt(Product::getReviewCount)
                .sum();

        // ===== Tỉ lệ phản hồi (giả định dựa trên số đơn không bị hủy) =====
        long totalOrders = shopOrders.size();
        long completedOrders = shopOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .count();
        int responseRate = totalOrders == 0 ? 100
                : Math.min(100, (int) Math.round((double) completedOrders / totalOrders * 100 + 18));
        if (responseRate > 99) responseRate = 99;

        // ===== Categories có trong shop =====
        Map<String, Long> categoryCount = allShopProducts.stream()
                .filter(p -> p.getCategoryId() != null)
                .collect(Collectors.groupingBy(Product::getCategoryId, Collectors.counting()));

        List<Map<String, Object>> shopCategories = categoryCount.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", entry.getKey());
                    map.put("name", allShopProducts.stream()
                            .filter(p -> entry.getKey().equals(p.getCategoryId()))
                            .findFirst()
                            .map(Product::getCategoryName)
                            .orElse("Khác"));
                    map.put("count", entry.getValue());
                    return map;
                })
                .sorted((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")))
                .limit(8)
                .collect(Collectors.toList());

        // ===== Filter products theo category/price range =====
        List<Product> filteredProducts = new ArrayList<>(allShopProducts);
        String selectedCategoryName = null;
        if (category != null && !category.isBlank()) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> category.equals(p.getCategoryId()))
                    .collect(Collectors.toList());
            for (Map<String, Object> cat : shopCategories) {
                if (category.equals(cat.get("id"))) {
                    selectedCategoryName = (String) cat.get("name");
                    break;
                }
            }
        }
        if (priceRange != null && !priceRange.isBlank()) {
            filteredProducts = applyPriceFilter(filteredProducts, priceRange);
        }

        // ===== Apply sort =====
        filteredProducts = applySort(filteredProducts, sort);

        // ===== Phân trang =====
        int totalFiltered = filteredProducts.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalFiltered / PAGE_SIZE));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int from = Math.min((page - 1) * PAGE_SIZE, totalFiltered);
        int to = Math.min(from + PAGE_SIZE, totalFiltered);
        List<Product> products = filteredProducts.subList(from, to);

        // ===== Top sản phẩm nổi bật =====
        List<Product> topProducts = allShopProducts.stream()
                .sorted(Comparator.comparingDouble(Product::getRating).reversed())
                .limit(4)
                .collect(Collectors.toList());

        // ===== Recent reviews (top 4) =====
        List<Review> recentReviews = shopReviews.stream().limit(4).collect(Collectors.toList());

        // ===== Tính giá thấp nhất / cao nhất =====
        BigDecimal minPrice = allShopProducts.stream()
                .map(Product::getPrice)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        BigDecimal maxPrice = allShopProducts.stream()
                .map(Product::getPrice)
                .filter(java.util.Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // ===== Tính tổng số sản phẩm theo price range cho filter =====
        long countUnder100k = allShopProducts.stream()
                .filter(p -> p.getPrice() != null && p.getPrice().compareTo(new BigDecimal("100000")) <= 0).count();
        long count100to500k = allShopProducts.stream()
                .filter(p -> p.getPrice() != null
                        && p.getPrice().compareTo(new BigDecimal("100000")) > 0
                        && p.getPrice().compareTo(new BigDecimal("500000")) <= 0).count();
        long count500kTo1m = allShopProducts.stream()
                .filter(p -> p.getPrice() != null
                        && p.getPrice().compareTo(new BigDecimal("500000")) > 0
                        && p.getPrice().compareTo(new BigDecimal("1000000")) <= 0).count();
        long countAbove1m = allShopProducts.stream()
                .filter(p -> p.getPrice() != null && p.getPrice().compareTo(new BigDecimal("1000000")) > 0).count();

        // ===== Pass data =====
        model.addAttribute("shop", shop);
        model.addAttribute("products", products);
        model.addAttribute("sort", sort);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedCategoryName", selectedCategoryName != null ? selectedCategoryName : "");
        model.addAttribute("selectedPriceRange", priceRange);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProducts", allShopProducts.size());
        model.addAttribute("totalFiltered", totalFiltered);
        model.addAttribute("outOfStock",
                allShopProducts.stream().filter(p -> p.getStock() <= 0).count());
        model.addAttribute("avgRating", avgRating);
        model.addAttribute("totalSold", totalSold);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("completedOrders", completedOrders);
        model.addAttribute("responseRate", responseRate);
        model.addAttribute("totalReviewCount", totalReviewCount);
        model.addAttribute("shopCategories", shopCategories);
        model.addAttribute("topProducts", topProducts);
        model.addAttribute("recentReviews", recentReviews);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("countUnder100k", countUnder100k);
        model.addAttribute("count100to500k", count100to500k);
        model.addAttribute("count500kTo1m", count500kTo1m);
        model.addAttribute("countAbove1m", countAbove1m);

        return "web/shop";
    }

    private List<Product> applyPriceFilter(List<Product> products, String priceRange) {
        return products.stream()
                .filter(p -> {
                    if (p.getPrice() == null) return false;
                    switch (priceRange) {
                        case "under-100k":
                            return p.getPrice().compareTo(new BigDecimal("100000")) <= 0;
                        case "100k-500k":
                            return p.getPrice().compareTo(new BigDecimal("100000")) > 0
                                    && p.getPrice().compareTo(new BigDecimal("500000")) <= 0;
                        case "500k-1m":
                            return p.getPrice().compareTo(new BigDecimal("500000")) > 0
                                    && p.getPrice().compareTo(new BigDecimal("1000000")) <= 0;
                        case "above-1m":
                            return p.getPrice().compareTo(new BigDecimal("1000000")) > 0;
                        default:
                            return true;
                    }
                })
                .collect(Collectors.toList());
    }

    private List<Product> applySort(List<Product> products, String sort) {
        return products.stream()
                .sorted((a, b) -> {
                    switch (sort) {
                        case "price-asc":
                            return a.getPrice().compareTo(b.getPrice());
                        case "price-desc":
                            return b.getPrice().compareTo(a.getPrice());
                        case "rating":
                            return Double.compare(b.getRating(), a.getRating());
                        case "best-selling":
                            return Integer.compare(b.getReviewCount(), a.getReviewCount());
                        default:
                            return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                })
                .collect(Collectors.toList());
    }
}
