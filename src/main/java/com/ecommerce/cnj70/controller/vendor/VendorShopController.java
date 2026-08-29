package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.dto.request.ShopFormReq;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.VendorService;
import com.ecommerce.cnj70.service.impl.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vendor/shop")
@RequiredArgsConstructor
public class VendorShopController {
    
    private final VendorService vendorService;
    private final StorageService storageService;
    
    @GetMapping
    public String shopProfile(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        try {
            Shop shop = vendorService.getShopByCurrentVendor(user);

            if (!shop.isVerified()) {
                model.addAttribute("shop", shop);
                model.addAttribute("pendingApproval", true);
                return "vendor/shop-pending";
            }

            model.addAttribute("shop", shop);
            model.addAttribute("shopFormReq", ShopFormReq.builder()
                    .shopName(shop.getShopName())
                    .description(shop.getDescription())
                    .logoUrl(shop.getLogoUrl())
                    .bannerUrl(shop.getBannerUrl())
                    .build());
            return "vendor/shop-profile";
        } catch (BadRequestException e) {
            model.addAttribute("noShop", true);
            model.addAttribute("shopFormReq", new ShopFormReq());
            return "vendor/shop-create";
        }
    }
    
    @GetMapping("/create")
    public String createShopForm(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("shopFormReq", new ShopFormReq());
        return "vendor/shop-create";
    }
    
    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public String createShop(@AuthenticationPrincipal CustomUserDetails user,
                            @ModelAttribute @Valid ShopFormReq request,
                            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                            @RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile,
                            RedirectAttributes redirectAttributes) {
        try {
            if (logoFile != null && !logoFile.isEmpty()) {
                request.setLogoUrl(storageService.save(logoFile));
            }
            if (bannerFile != null && !bannerFile.isEmpty()) {
                request.setBannerUrl(storageService.save(bannerFile));
            }
            vendorService.createShop(user, request);
            redirectAttributes.addFlashAttribute("success", "Tạo shop thành công!");
            return "redirect:/vendor/shop";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/shop/create";
        }
    }

    @PostMapping(value = "/update", consumes = {"multipart/form-data"})
    public String updateShop(@AuthenticationPrincipal CustomUserDetails user,
                            @ModelAttribute @Valid ShopFormReq request,
                            @RequestParam(value = "logoFile", required = false) MultipartFile logoFile,
                            @RequestParam(value = "bannerFile", required = false) MultipartFile bannerFile,
                            RedirectAttributes redirectAttributes) {
        try {
            Shop shop = vendorService.getShopByCurrentVendor(user);
            if (!shop.isVerified()) {
                redirectAttributes.addFlashAttribute("error",
                        "Không thể cập nhật cửa hàng khi đang chờ duyệt. Vui lòng chờ quản trị viên duyệt.");
                return "redirect:/vendor/shop";
            }
            if (logoFile != null && !logoFile.isEmpty()) {
                request.setLogoUrl(storageService.save(logoFile));
            }
            if (bannerFile != null && !bannerFile.isEmpty()) {
                request.setBannerUrl(storageService.save(bannerFile));
            }
            vendorService.updateShop(user, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật shop thành công!");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vendor/shop";
    }
}
