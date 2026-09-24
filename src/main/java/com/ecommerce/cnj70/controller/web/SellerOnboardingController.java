package com.ecommerce.cnj70.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Public landing page cho chương trình "Trở thành người bán trên CNJ70 Shop".
 * <p>
 * Mục đích:
 * <ul>
 *     <li>Marketing: giới thiệu lợi ích khi bán hàng trên sàn</li>
 *     <li>Liệt kê yêu cầu tối thiểu (CMND/CCCD, tài khoản NH, ...)</li>
 *     <li>Giải thích quy trình 5 bước onboarding (đăng ký → KYC → tạo shop → đăng sản phẩm)</li>
 *     <li>CTA dẫn tới {@code /auth/register} (kèm param {@code role=VENDOR})
 *         hoặc {@code /auth/login} nếu đã có tài khoản</li>
 * </ul>
 * Public route, không yêu cầu auth (đã được SecurityConfig permitAll mặc định
 * cho mọi request không khớp pattern đặc biệt).
 *
 * @author CNJ70
 */
@Controller
public class SellerOnboardingController {

    /**
     * Trang landing giới thiệu chương trình Seller.
     *
     * @param model Spring UI model
     * @return view template {@code web/seller-onboarding.html}
     */
    @GetMapping("/seller")
    public String sellerLanding(Model model) {
        // Hiển thị CTA khác nhau tuỳ theo đã đăng nhập hay chưa.
        // Template sẽ tự dùng sec:authorize để đổi nhãn nút.
        model.addAttribute("pageTitle", "Trở thành người bán trên CNJ70 Shop");
        model.addAttribute("activeNav", "seller");
        return "web/seller-onboarding";
    }
}
