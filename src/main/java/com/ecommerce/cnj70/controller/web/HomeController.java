package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.FlashSaleStat;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.service.CustomerBannerService;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final VoucherService voucherService;
    private final CustomerBannerService customerBannerService;

    @GetMapping("/home")
    public String homePage(Model model) {
        List<Product> products = productService.getActiveProducts();
        List<Product> newArrivals = productService.getNewArrivals(8);
        List<Product> featuredProducts = productService.getActiveProducts().stream().limit(8).toList();
        // Flash-sale strip: only surface products that already have real
        // sales activity in the database so the section reflects genuine
        // movement, not every active SKU. Falls back to featuredProducts
        // when nothing has sold yet so the section is never empty.
        List<Product> flashSaleSource = productService.getActiveProducts().stream()
                .filter(p -> p.getSold() > 0)
                .limit(8)
                .toList();
        if (flashSaleSource.isEmpty()) {
            flashSaleSource = featuredProducts;
        }
        List<FlashSaleStat> flashSaleStats = buildFlashSaleStats(flashSaleSource);
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        List<Voucher> availableVouchers = voucherService.getAvailableVouchers();

        // Phase 17 — Banner Dynamic Display (Task 17.19)
        List<Banner> heroBanners = customerBannerService.getVisibleBanners("HERO_SLIDER");
        List<Banner> promoBanners = customerBannerService.getVisibleBanners("PROMO_GRID");

        model.addAttribute("products", products);
        model.addAttribute("newArrivals", newArrivals);
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("flashSaleStats", flashSaleStats);
        model.addAttribute("categories", categories);
        model.addAttribute("availableVouchers", availableVouchers);
        model.addAttribute("heroBanners", heroBanners);
        model.addAttribute("promoBanners", promoBanners);
        return "web/index";
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