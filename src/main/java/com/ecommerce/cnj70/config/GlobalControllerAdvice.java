package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice - thêm các attribute chung cho tất cả view
 */
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {
    
    private final VendorService vendorService;
    private final UserRepository userRepository;
    
    /**
     * Thêm hasShop vào model cho vendor pages
     * Để sidebar có thể ẩn/hiện menu Voucher
     */
    @ModelAttribute
    public void addVendorAttributes(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user != null && user.getShopId() != null && !user.getShopId().isBlank()) {
                model.addAttribute("hasShop", true);
            } else {
                model.addAttribute("hasShop", false);
            }
        } else {
            model.addAttribute("hasShop", false);
        }
    }
}
