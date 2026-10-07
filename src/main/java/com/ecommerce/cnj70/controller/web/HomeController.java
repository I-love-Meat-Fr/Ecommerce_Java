package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.FlashSaleStat;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.CustomerBannerService;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HomeController {

    /** PERFORMANCE #2 — chỉ render tối đa N sản phẩm cho mỗi list trên home. */
    private static final int HOME_LIST_LIMIT = 8;

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final VoucherService voucherService;
    private final CustomerBannerService customerBannerService;
    private final ShopRepository shopRepository;

    @GetMapping("/home")
    public String homePage(Model model) {
        // PERFORMANCE #2 — gộp 3 lần `getActiveProducts()` trước đây
        // thành 1 lần `getFeaturedProducts()` đã sort DESC theo rating
        // ngayở Mongo (xem ProductServiceImpl#getFeaturedProducts).
        // Trước: 3 query DB, mỗi query load full collection + sort in JVM.
        // Sau:  1 query DB với Mongo-side sort + projection + limit.
        List<Product> featuredProducts = productService.getFeaturedProducts(HOME_LIST_LIMIT);

        // Lấy danh sách active đầy đủ (giới hạn 50) cho tab "Tất cả".
        // Đã có field projection, không pull imageUrls/variants.
        List<Product> products = productService.getActiveProducts();

        List<Product> newArrivals = productService.getNewArrivals(HOME_LIST_LIMIT);

        // PERFORMANCE #2 — flash sale: query thẳng sold > 0 ở Mongo,
        // không cần load tất cả active rồi filter trong Java.
        List<Product> flashSaleSource = productService.getFlashSaleCandidates(HOME_LIST_LIMIT);
        if (flashSaleSource.isEmpty()) {
            // Fallback khi DB chưa có sản phẩm nào bán được → dùng
            // featuredProducts thay vì gọi lại DB.
            flashSaleSource = featuredProducts;
        }
        List<FlashSaleStat> flashSaleStats = buildFlashSaleStats(flashSaleSource);
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        List<Voucher> availableVouchers = voucherService.getAvailableVouchers();
        // Trang chỉ hiển thị tối đa 3 voucher để section không bị kéo dài/loãng.
        // Người dùng có nhu cầu xem thêm có thể bấm "Xem tất cả" → /vouchers.
        if (availableVouchers != null && availableVouchers.size() > 3) {
            availableVouchers = new ArrayList<>(availableVouchers.subList(0, 3));
        }

        // Phase 17 — Banner Dynamic Display (Task 17.19)
        List<Banner> heroBanners = customerBannerService.getVisibleBanners("HERO_SLIDER");
        List<Banner> promoBanners = customerBannerService.getVisibleBanners("PROMO_GRID");

        // Premium shop IDs — dùng cho badge HOT trên các product cards
        Set<String> premiumShopIds = loadActivePremiumShopIds();

        model.addAttribute("products", products);
        model.addAttribute("newArrivals", newArrivals);
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("flashSaleStats", flashSaleStats);
        model.addAttribute("categories", categories);
        model.addAttribute("availableVouchers", availableVouchers);
        model.addAttribute("heroBanners", heroBanners);
        model.addAttribute("promoBanners", promoBanners);
        model.addAttribute("premiumShopIds", premiumShopIds);
        return "web/index";
    }

    /** Lấy set các shopId đang có Premium ACTIVE (cho badge HOT). */
    private Set<String> loadActivePremiumShopIds() {
        try {
            return shopRepository
                    .findByPremiumActiveTrueAndPremiumExpiresAtAfter(LocalDateTime.now())
                    .stream()
                    .map(Shop::getId)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            return Set.of();
        }
    }

    /**
     * Build the Flash Sale stats for the strip on the home page.
     * Every value shown in the strip is derived from real data on the
     * product document (no hash-based placeholders):
     * <ul>
     *     <li>{@code soldCount} – mirrors {@link Product#getSold()} from the DB.</li>
     *     <li>{@code progressPercent} – {@code sold / (sold + stock)} capped at 99%.</li>
     *     <li>{@code hasDiscount} / {@code discountPercent} – only set when the product
     *         carries an explicit original price; otherwise the template renders the
     *         non-numeric HOT badge.</li>
     * </ul>
     */
    private List<FlashSaleStat> buildFlashSaleStats(List<Product> featuredProducts) {
        List<FlashSaleStat> stats = new ArrayList<>();
        if (featuredProducts == null) {
            return stats;
        }
        for (Product product : featuredProducts) {
            int sold = Math.max(0, product.getSold());
            int stock = Math.max(0, product.getStock());
            long total = (long) sold + (long) stock;
            int progressPercent = total > 0
                    ? (int) Math.min(99L, Math.round((double) sold * 100.0 / total))
                    : 0;
            boolean hasDiscount = false;
            int discountPercent = 0;

            stats.add(FlashSaleStat.builder()
                    .product(product)
                    .hasDiscount(hasDiscount)
                    .discountPercent(discountPercent)
                    .soldCount(sold)
                    .progressPercent(progressPercent)
                    .build());
        }
        return stats;
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/home";
    }
}