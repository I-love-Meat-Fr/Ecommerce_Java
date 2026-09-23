package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.VendorService;
import com.ecommerce.cnj70.service.ViolationService;
import com.ecommerce.cnj70.service.VoucherService;
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
    private final KycProfileRepository kycProfileRepository;
    private final VoucherService voucherService;
    private final ViolationService violationService;

    /**
     * Thêm hasShop + kycStatus + activeViolations vào model cho vendor pages
     */
    @ModelAttribute
    public void addVendorAttributes(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                boolean hasShop = user.getShopId() != null && !user.getShopId().isBlank();
                model.addAttribute("hasShop", hasShop);

                // Đọc kycStatus trực tiếp từ User (denormalized)
                KycStatus kycStatus = user.getKycStatus() != null
                        ? user.getKycStatus()
                        : KycStatus.NOT_SUBMITTED;
                model.addAttribute("kycStatus", kycStatus.name());
                model.addAttribute("kycApproved", kycStatus == KycStatus.APPROVED);

                // Đếm violations chưa xử lý của shop hiện tại (cho badge sidebar)
                if (hasShop) {
                    try {
                        long activeViolations = violationService.countActiveViolations(user.getShopId());
                        model.addAttribute("activeViolations", activeViolations);
                    } catch (Exception e) {
                        model.addAttribute("activeViolations", 0L);
                    }
                } else {
                    model.addAttribute("activeViolations", 0L);
                }
            } else {
                model.addAttribute("hasShop", false);
                model.addAttribute("kycStatus", "NOT_SUBMITTED");
                model.addAttribute("kycApproved", false);
                model.addAttribute("activeViolations", 0L);
            }
        } else {
            model.addAttribute("hasShop", false);
            model.addAttribute("kycStatus", "NOT_SUBMITTED");
            model.addAttribute("kycApproved", false);
            model.addAttribute("activeViolations", 0L);
        }
    }

    /**
     * Thêm số lượng voucher khả dụng để hiển thị badge ở header
     */
    @ModelAttribute
    public void addVoucherCount(Model model) {
        try {
            long count = voucherService.countAvailableVouchers();
            model.addAttribute("availableVoucherCount", count);
        } catch (Exception e) {
            model.addAttribute("availableVoucherCount", 0L);
        }
    }
}
