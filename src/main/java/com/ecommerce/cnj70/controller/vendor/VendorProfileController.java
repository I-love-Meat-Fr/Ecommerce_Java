package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.dto.response.VendorProfileRes;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/vendor/profile")
@RequiredArgsConstructor
public class VendorProfileController {
    
    private final VendorService vendorService;
    
    @GetMapping
    public String profile(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        VendorProfileRes profile = vendorService.getVendorProfile(user);
        model.addAttribute("profile", profile);
        return "vendor/profile";
    }
}
