package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.FlashSaleStat;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.repository.CategoryRepository;
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

    @GetMapping("/home")
    public String homePage(Model model) {
        List<Product> products = productService.getActiveProducts();
        List<Product> newArrivals = productService.getNewArrivals(8);
        List<Product> featuredProducts = productService.getActiveProducts().stream().limit(8).toList();
        List<FlashSaleStat> flashSaleStats = buildFlashSaleStats(featuredProducts);
        List<Category> categories = categoryRepository.findByActiveTrueOrderBySortOrderAsc();
        List<Voucher> availableVouchers = voucherService.getAvailableVouchers();

        model.addAttribute("products", products);
        model.addAttribute("newArrivals", newArrivals);
        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("flashSaleStats", flashSaleStats);
        model.addAttribute("categories", categories);
        model.addAttribute("availableVouchers", availableVouchers);
        return "web/index";
    }

    /**
     * Build the Flash Sale stats for the strip on the home page.
     * Discount percentage and sold count are derived deterministically
     * from the product ID so each product shows a stable value across
     * page reloads, similar to the previous MoneyHelper helpers.
     */
    private List<FlashSaleStat> buildFlashSaleStats(List<Product> featuredProducts) {
        List<FlashSaleStat> stats = new ArrayList<>();
        if (featuredProducts == null) {
            return stats;
        }
        for (Product product : featuredProducts) {
            int hash = Math.abs(String.valueOf(product.getId()).hashCode());
            int discountPercent = 20 + (hash % 41); // 20..60
            int soldCount = 20 + (hash % 171);      // 20..190
            boolean hasDiscount = discountPercent > 0;
            stats.add(FlashSaleStat.builder()
                    .product(product)
                    .hasDiscount(hasDiscount)
                    .discountPercent(discountPercent)
                    .soldCount(soldCount)
                    .build());
        }
        return stats;
    }

    @GetMapping("/")
    public String rootRedirect() {
        return "redirect:/home";
    }
}