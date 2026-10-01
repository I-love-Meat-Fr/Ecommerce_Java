package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.PremiumPackage;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.VendorSubscription;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.PremiumPackageService;
import com.ecommerce.cnj70.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Vendor Premium Subscription Controller.
 *
 * <p>Routes:</p>
 * <pre>
 *   GET  /vendor/premium                 - List packages + current sub
 *   POST /vendor/premium/purchase/{id}   - Mock checkout, tạo sub
 *   POST /vendor/premium/cancel          - Hủy sub hiện tại
 * </pre>
 *
 * <p>Authorization: {@code /vendor/**} đã được SecurityConfig lock bằng
 * {@code hasRole("VENDOR")}.</p>
 */
@Controller
@RequestMapping("/vendor/premium")
@RequiredArgsConstructor
public class VendorPremiumController {

    private final PremiumPackageService premiumPackageService;
    private final VendorService vendorService;

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        // Try to load shop + sub (có thể chưa có → vendor chưa tạo shop)
        Shop shop = null;
        try {
            shop = vendorService.getShopByCurrentVendor(user);
        } catch (Exception ignored) {
            // Vendor chưa có shop → render empty state
        }

        List<PremiumPackage> packages = premiumPackageService.getActivePackages();
        Optional<VendorSubscription> currentSubOpt = premiumPackageService.getCurrentSubscription(user);
        VendorSubscription currentSub = currentSubOpt.orElse(null);

        long daysRemaining = 0;
        if (currentSub != null && currentSub.getEndDate() != null) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDateTime.now(), currentSub.getEndDate());
            if (daysRemaining < 0) daysRemaining = 0;
        }

        List<VendorSubscription> history = premiumPackageService.getSubscriptionHistory(user);

        model.addAttribute("shop", shop);
        model.addAttribute("packages", packages);
        model.addAttribute("currentSub", currentSub);
        model.addAttribute("daysRemaining", daysRemaining);
        model.addAttribute("history", history);
        return "vendor/premium";
    }

    @PostMapping("/purchase/{packageId}")
    public String purchase(@PathVariable String packageId,
                           @AuthenticationPrincipal CustomUserDetails user,
                           RedirectAttributes redirectAttributes) {
        try {
            // Validate vendor có shop APPROVED trước khi mua (pattern giống create product)
            try {
                Shop shop = vendorService.getShopByCurrentVendor(user);
                if (!shop.isVerified()) {
                    redirectAttributes.addFlashAttribute("error",
                            "Shop của bạn chưa được duyệt. Không thể đăng ký Premium.");
                    return "redirect:/vendor/premium";
                }
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error",
                        "Bạn cần tạo shop trước khi đăng ký Premium.");
                return "redirect:/vendor/premium";
            }

            VendorSubscription sub = premiumPackageService.purchasePackage(packageId, user);
            redirectAttributes.addFlashAttribute("success",
                    "Đăng ký Premium thành công! Hết hạn vào " + sub.getEndDate());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/vendor/premium";
    }

    @PostMapping("/cancel")
    public String cancel(@AuthenticationPrincipal CustomUserDetails user,
                         RedirectAttributes redirectAttributes) {
        try {
            premiumPackageService.cancelSubscription(user);
            redirectAttributes.addFlashAttribute("success",
                    "Đã hủy gói Premium. Shop của bạn sẽ không còn hiển thị badge HOT.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/vendor/premium";
    }
}
