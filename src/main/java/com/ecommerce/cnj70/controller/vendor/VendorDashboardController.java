package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class VendorDashboardController {
    
    @GetMapping("/vendor/dashboard")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("shopId", user.getShopId());
        return "vendor/dashboard";
    }
}
