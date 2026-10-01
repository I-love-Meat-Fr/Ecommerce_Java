package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.PremiumPackage;
import com.ecommerce.cnj70.dto.request.PremiumPackageFormReq;
import com.ecommerce.cnj70.service.PremiumPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Admin CRUD Premium Packages.
 *
 * <p>Routes:</p>
 * <pre>
 *   GET  /admin/premium              - List + search
 *   GET  /admin/premium/create       - Create form
 *   POST /admin/premium/create       - Create package
 *   GET  /admin/premium/{id}/edit    - Edit form
 *   POST /admin/premium/{id}/edit    - Update package
 *   POST /admin/premium/{id}/delete  - Soft delete (set active=false)
 * </pre>
 *
 * <p>Authorization: {@code /admin/**} đã được SecurityConfig lock bằng
 * {@code hasRole("ADMIN")}.</p>
 */
@Controller
@RequestMapping("/admin/premium")
@RequiredArgsConstructor
public class AdminPremiumController {

    private final PremiumPackageService premiumPackageService;

    @GetMapping
    public String list(@RequestParam(required = false) String active,
                       Model model) {
        Boolean activeFilter = parseActive(active);
        List<PremiumPackage> packages = premiumPackageService.getAllPackages(activeFilter);

        model.addAttribute("packages", packages);
        model.addAttribute("active", active == null ? "" : active);
        model.addAttribute("totalItems", packages.size());
        return "admin/premium-list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("premiumForm", blankForm());
        model.addAttribute("isEdit", false);
        return "admin/premium-form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("premiumForm") PremiumPackageFormReq form,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            PremiumPackage saved = premiumPackageService.createPackage(form);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã tạo Premium Package: " + saved.getName());
            return "redirect:/admin/premium";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("premiumForm", form);
            model.addAttribute("isEdit", false);
            return "admin/premium-form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, Model model,
                           RedirectAttributes redirectAttributes) {
        try {
            PremiumPackage pkg = premiumPackageService.getPackageById(id);
            PremiumPackageFormReq form = PremiumPackageFormReq.builder()
                    .name(pkg.getName())
                    .description(pkg.getDescription())
                    .price(pkg.getPrice())
                    .durationDays(pkg.getDurationDays())
                    .priority(pkg.getPriority())
                    .active(pkg.isActive())
                    .build();
            model.addAttribute("premiumForm", form);
            model.addAttribute("packageId", id);
            model.addAttribute("isEdit", true);
            return "admin/premium-form";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/premium";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable String id,
                         @Valid @ModelAttribute("premiumForm") PremiumPackageFormReq form,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        try {
            premiumPackageService.updatePackage(id, form);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã cập nhật Premium Package");
            return "redirect:/admin/premium";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("premiumForm", form);
            model.addAttribute("packageId", id);
            model.addAttribute("isEdit", true);
            return "admin/premium-form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id,
                         @RequestParam(required = false) String active,
                         RedirectAttributes redirectAttributes) {
        try {
            premiumPackageService.deletePackage(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã ngừng bán Premium Package (ID: " + id + ")");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(active);
    }

    // ========== Helpers ==========

    private static PremiumPackageFormReq blankForm() {
        return PremiumPackageFormReq.builder()
                .durationDays(30)
                .price(new BigDecimal("199000"))
                .priority(0)
                .active(true)
                .build();
    }

    private static Boolean parseActive(String active) {
        if (active == null || active.isBlank()) return null;
        return Boolean.valueOf(active);
    }

    private static String buildRedirectUrl(String active) {
        StringBuilder url = new StringBuilder("redirect:/admin/premium");
        if (active != null && !active.isBlank()) {
            url.append("?active=").append(active);
        }
        return url.toString();
    }
}
