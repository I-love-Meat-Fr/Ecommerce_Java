package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.service.AdminProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 14 — Admin Product Moderation Controller.
 *
 * Routes:
 *   GET  /admin/products              - Product List (search + filter + pagination)
 *   GET  /admin/products/{id}         - Product Detail
 *   POST /admin/products/{id}/hide    - Hide Product
 *   POST /admin/products/{id}/unhide  - Unhide Product
 *   POST /admin/products/{id}/delete  - Delete Violation
 *
 * Security: /admin/** đã được bảo vệ bởi SecurityConfig.hasRole("ADMIN").
 */
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AdminProductService adminProductService;

    @GetMapping
    public String productList(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String status,
                              Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        ProductStatus statusFilter = parseStatus(status);

        Page<Product> result = adminProductService.listProducts(pageable, q, statusFilter);

        model.addAttribute("products", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", statusFilter == null ? "" : statusFilter.name());
        model.addAttribute("statuses", ProductStatus.values());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        return "admin/product-list";
    }

    @GetMapping("/{id}")
    public String productDetail(@PathVariable String id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                Model model) {
        Product product = adminProductService.getProductById(id);

        model.addAttribute("product", product);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        return "admin/product-detail";
    }

    @PostMapping("/{id}/hide")
    public String hideProduct(@PathVariable String id,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String status,
                              RedirectAttributes redirectAttributes) {
        try {
            Product product = adminProductService.hideProduct(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã Ẩn sản phẩm \"" + product.getName() + "\"");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, status);
    }

    @PostMapping("/{id}/unhide")
    public String unhideProduct(@PathVariable String id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                RedirectAttributes redirectAttributes) {
        try {
            Product product = adminProductService.unhideProduct(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã Hiện sản phẩm \"" + product.getName() + "\"");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, status);
    }

    @PostMapping("/{id}/delete")
    public String deleteProduct(@PathVariable String id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                RedirectAttributes redirectAttributes) {
        try {
            adminProductService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã xóa sản phẩm vi phạm (ID: " + id + ")");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, status);
    }

    private String buildRedirectUrl(int page, int size, String q, String status) {
        StringBuilder url = new StringBuilder("/admin/products?")
                .append("page=").append(page)
                .append("&size=").append(size);
        if (q != null && !q.isBlank()) {
            url.append("&q=").append(q);
        }
        if (status != null && !status.isBlank()) {
            url.append("&status=").append(status);
        }
        return "redirect:" + url;
    }

    private static List<Integer> computePageRange(int current, int totalPages) {
        List<Integer> out = new ArrayList<>();
        if (totalPages <= 0) return out;
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }

    private static ProductStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String norm = raw.trim().toUpperCase();
        if ("ALL".equals(norm)) return null;
        try {
            return ProductStatus.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
