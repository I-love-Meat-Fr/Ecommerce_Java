package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.enums.BannerStatus;
import com.ecommerce.cnj70.service.AdminBannerService;
import com.ecommerce.cnj70.service.impl.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Phase 17 — Admin Banner / PR Controller.
 *
 * Routes:
 *   GET  /admin/banners              - Banner List
 *   GET  /admin/banners/create       - Create Form
 *   POST /admin/banners/create       - Create Banner (with optional image upload)
 *   GET  /admin/banners/{id}         - Banner Detail
 *   GET  /admin/banners/{id}/edit    - Edit Form
 *   POST /admin/banners/{id}/edit    - Update Banner
 *   POST /admin/banners/{id}/publish - Publish
 *   POST /admin/banners/{id}/unpublish - Unpublish
 *   POST /admin/banners/{id}/delete  - Delete
 *
 * Security: /admin/** đã bị SecurityConfig.hasRole("ADMIN") lock.
 */
@Controller
@RequestMapping("/admin/banners")
@RequiredArgsConstructor
public class AdminBannerController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AdminBannerService adminBannerService;
    private final StorageService storageService;

    @GetMapping
    public String bannerList(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String status,
                             Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.ASC, "sortOrder", "createdAt"));

        Page<Banner> result = adminBannerService.listBanners(pageable, q, status);

        model.addAttribute("banners", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("statuses", Arrays.asList(
                BannerStatus.PUBLISHED, BannerStatus.UNPUBLISHED));
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        return "admin/banner-list";
    }

    @GetMapping("/create")
    public String createBannerForm(Model model) {
        model.addAttribute("banner", new Banner());
        model.addAttribute("themes", getAvailableThemes());
        model.addAttribute("positions", getAvailablePositions());
        model.addAttribute("isEdit", false);
        return "admin/banner-form";
    }

    @PostMapping("/create")
    public String createBanner(@ModelAttribute("banner") Banner banner,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            // Upload ảnh nếu có — REUSE StorageService hiện có (LOCK 6 + LOCK 8)
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = storageService.save(imageFile);
                banner.setImageUrl(uploadedUrl);
            }
            Banner saved = adminBannerService.createBanner(banner);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã tạo banner \"" + saved.getTitle() + "\"");
            return "redirect:/admin/banners/" + saved.getId();
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("banner", banner);
            model.addAttribute("themes", getAvailableThemes());
            model.addAttribute("positions", getAvailablePositions());
            model.addAttribute("isEdit", false);
            return "admin/banner-form";
        }
    }

    @GetMapping("/{id}")
    public String bannerDetail(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) String status,
                               Model model) {
        Banner banner = adminBannerService.getBannerById(id);
        model.addAttribute("banner", banner);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        return "admin/banner-detail";
    }

    @GetMapping("/{id}/edit")
    public String editBannerForm(@PathVariable String id,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(required = false) String status,
                                 Model model) {
        Banner banner = adminBannerService.getBannerById(id);
        model.addAttribute("banner", banner);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("themes", getAvailableThemes());
        model.addAttribute("positions", getAvailablePositions());
        model.addAttribute("isEdit", true);
        return "admin/banner-form";
    }

    @PostMapping("/{id}/edit")
    public String updateBanner(@PathVariable String id,
                               @ModelAttribute("banner") Banner banner,
                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        try {
            if (imageFile != null && !imageFile.isEmpty()) {
                String uploadedUrl = storageService.save(imageFile);
                banner.setImageUrl(uploadedUrl);
            }
            Banner updated = adminBannerService.updateBanner(id, banner);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã cập nhật banner \"" + updated.getTitle() + "\"");
            return "redirect:/admin/banners/" + id;
        } catch (RuntimeException ex) {
            Banner existing = adminBannerService.getBannerById(id);
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("banner", existing);
            model.addAttribute("page", 0);
            model.addAttribute("themes", getAvailableThemes());
            model.addAttribute("positions", getAvailablePositions());
            model.addAttribute("isEdit", true);
            return "admin/banner-form";
        }
    }

    @PostMapping("/{id}/publish")
    public String publishBanner(@PathVariable String id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                RedirectAttributes redirectAttributes) {
        try {
            Banner banner = adminBannerService.publishBanner(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã Publish banner \"" + banner.getTitle() + "\"");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, status);
    }

    @PostMapping("/{id}/unpublish")
    public String unpublishBanner(@PathVariable String id,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(required = false) String status,
                                  RedirectAttributes redirectAttributes) {
        try {
            Banner banner = adminBannerService.unpublishBanner(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã Unpublish banner \"" + banner.getTitle() + "\"");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, status);
    }

    @PostMapping("/{id}/delete")
    public String deleteBanner(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) String status,
                               RedirectAttributes redirectAttributes) {
        try {
            adminBannerService.deleteBanner(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã xóa banner (ID: " + id + ")");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, status);
    }

    private String buildRedirectUrl(int page, int size, String q, String status) {
        StringBuilder url = new StringBuilder("/admin/banners?")
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

    private static List<String> getAvailableThemes() {
        return Arrays.asList("primary", "teal", "purple", "orange", "red", "slate");
    }

    private static List<String> getAvailablePositions() {
        // Phase 17 Customer Display positions
        return Arrays.asList("HERO_SLIDER", "PROMO_GRID");
    }
}
