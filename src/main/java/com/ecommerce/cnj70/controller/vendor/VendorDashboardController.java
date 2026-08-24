package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.dto.response.VendorDashboardRes;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class VendorDashboardController {
    
    private final VendorService vendorService;
    
    @GetMapping("/vendor/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        VendorDashboardRes stats = vendorService.getDashboardStats(user);
        model.addAttribute("stats", stats);
        return "vendor/dashboard";
    }
    
    @GetMapping("/vendor")
    public String vendorHome(@AuthenticationPrincipal CustomUserDetails user) {
        return "redirect:/vendor/dashboard";
    }
}
