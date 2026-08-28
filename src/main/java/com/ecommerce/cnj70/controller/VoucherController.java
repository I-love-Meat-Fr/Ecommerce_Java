package com.ecommerce.cnj70.controller;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.VendorService;
import com.ecommerce.cnj70.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller xử lý voucher
 * - /vouchers: Trang công khai hiển thị voucher
 * - /vendor/vouchers/*: Vendor quản lý voucher của shop mình
 * - /admin/vouchers/*: Admin quản lý voucher WEB
 * - /checkout/apply-voucher: Áp dụng voucher khi checkout
 */
@Controller
@RequiredArgsConstructor
public class VoucherController {
    
    private final VoucherService voucherService;
    private final VendorService vendorService;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    
    // ==================== TRANG CÔNG KHAI ====================
    
    /**
     * Trang công khai - hiển thị tất cả voucher khả dụng
     */
    @GetMapping("/vouchers")
    public String vouchersPage(Model model) {
        List<Voucher> availableVouchers = voucherService.getAvailableVouchers();
        model.addAttribute("vouchers", availableVouchers);
        return "vouchers/list";
    }
    
    // ==================== VENDOR VOUCHER ====================
    
    /**
     * Danh sách voucher của vendor
     */
    @GetMapping("/vendor/vouchers")
    public String vendorVoucherList(@AuthenticationPrincipal UserDetails user, Model model, 
                                    RedirectAttributes redirectAttributes) {
        // Kiểm tra vendor có shop chưa
        try {
            String shopId = vendorService.getShopIdFromUser(user);
            Shop shop = vendorService.getShopByCurrentVendor(user);
            
            List<Voucher> vouchers = voucherService.getVouchersByShop(shopId);
            model.addAttribute("vouchers", vouchers);
            model.addAttribute("shop", shop);
            
            return "vendor/voucher-list";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/shop";
        }
    }
    
    /**
     * Form tạo voucher cho vendor
     */
    @GetMapping("/vendor/vouchers/create")
    public String createVendorVoucherForm(@AuthenticationPrincipal UserDetails user, Model model,
                                         RedirectAttributes redirectAttributes) {
        try {
            String shopId = vendorService.getShopIdFromUser(user);
            Shop shop = vendorService.getShopByCurrentVendor(user);
            
            if (!model.containsAttribute("voucherForm")) {
                model.addAttribute("voucherForm", new VoucherFormReq());
            }
            model.addAttribute("shop", shop);
            
            return "vendor/voucher-create";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/shop";
        }
    }
    
    /**
     * Xử lý tạo voucher cho vendor
     */
    @PostMapping("/vendor/vouchers/create")
    public String createVendorVoucher(@AuthenticationPrincipal UserDetails user,
                                      @Valid @ModelAttribute("voucherForm") VoucherFormReq form,
                                      BindingResult result,
                                      RedirectAttributes redirectAttributes) {
        try {
            Shop shop = vendorService.getShopByCurrentVendor(user);
            User vendor = vendorService.getCurrentVendor(user);
            
            Voucher voucher = voucherService.createVoucher(form, shop.getId(), shop.getShopName(), vendor.getId());
            
            redirectAttributes.addFlashAttribute("success", "Tạo voucher thành công!");
            return "redirect:/vendor/vouchers";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("voucherForm", form);
            return "redirect:/vendor/vouchers/create";
        }
    }
    
    /**
     * Form chỉnh sửa voucher
     */
    @GetMapping("/vendor/vouchers/edit/{id}")
    public String editVendorVoucherForm(@AuthenticationPrincipal UserDetails user,
                                        @PathVariable String id, Model model,
                                        RedirectAttributes redirectAttributes) {
        try {
            Voucher voucher = voucherService.getVoucherById(id);
            
            // Validate ownership
            String shopId = vendorService.getShopIdFromUser(user);
            if (!shopId.equals(voucher.getShopId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền sửa voucher này");
                return "redirect:/vendor/vouchers";
            }
            
            if (!model.containsAttribute("voucherForm")) {
                model.addAttribute("voucherForm", VoucherFormReq.builder()
                        .code(voucher.getCode())
                        .name(voucher.getName())
                        .discountType(voucher.getDiscountType())
                        .discountValue(voucher.getDiscountValue())
                        .maxDiscountAmount(voucher.getMaxDiscountAmount())
                        .minOrderValue(voucher.getMinOrderValue())
                        .quantity(voucher.getQuantity())
                        .startDate(voucher.getStartDate())
                        .endDate(voucher.getEndDate())
                        .build());
            }
            model.addAttribute("voucher", voucher);
            
            return "vendor/voucher-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/vouchers";
        }
    }
    
    /**
     * Xử lý chỉnh sửa voucher
     */
    @PostMapping("/vendor/vouchers/edit/{id}")
    public String editVendorVoucher(@AuthenticationPrincipal UserDetails user,
                                   @PathVariable String id,
                                   @Valid @ModelAttribute("voucherForm") VoucherFormReq form,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {
        try {
            Voucher voucher = voucherService.getVoucherById(id);
            
            // Validate ownership
            String shopId = vendorService.getShopIdFromUser(user);
            if (!shopId.equals(voucher.getShopId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền sửa voucher này");
                return "redirect:/vendor/vouchers";
            }
            
            voucherService.updateVoucher(id, form);
            redirectAttributes.addFlashAttribute("success", "Cập nhật voucher thành công!");
            
            return "redirect:/vendor/vouchers";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("voucherForm", form);
            return "redirect:/vendor/vouchers/edit/" + id;
        }
    }
    
    /**
     * Xóa voucher (soft delete)
     */
    @PostMapping("/vendor/vouchers/delete/{id}")
    public String deleteVendorVoucher(@AuthenticationPrincipal UserDetails user,
                                       @PathVariable String id,
                                       RedirectAttributes redirectAttributes) {
        try {
            Voucher voucher = voucherService.getVoucherById(id);
            
            // Validate ownership
            String shopId = vendorService.getShopIdFromUser(user);
            if (!shopId.equals(voucher.getShopId())) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa voucher này");
                return "redirect:/vendor/vouchers";
            }
            
            voucherService.deleteVoucher(id);
            redirectAttributes.addFlashAttribute("success", "Xóa voucher thành công!");
            
            return "redirect:/vendor/vouchers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vendor/vouchers";
        }
    }
    
    // ==================== ADMIN VOUCHER ====================
    
    /**
     * Danh sách voucher WEB (của admin)
     */
    @GetMapping("/admin/vouchers")
    public String adminVoucherList(Model model) {
        List<Voucher> webVouchers = voucherService.getWebVouchers();
        model.addAttribute("vouchers", webVouchers);
        return "admin/voucher-list";
    }
    
    /**
     * Form tạo voucher WEB cho admin
     */
    @GetMapping("/admin/vouchers/create")
    public String createAdminVoucherForm(Model model) {
        if (!model.containsAttribute("voucherForm")) {
            model.addAttribute("voucherForm", new VoucherFormReq());
        }
        return "admin/voucher-create";
    }
    
    /**
     * Xử lý tạo voucher WEB cho admin
     */
    @PostMapping("/admin/vouchers/create")
    public String createAdminVoucher(@AuthenticationPrincipal UserDetails user,
                                    @Valid @ModelAttribute("voucherForm") VoucherFormReq form,
                                    BindingResult result,
                                    RedirectAttributes redirectAttributes) {
        try {
            User admin = vendorService.getCurrentVendor(user);
            voucherService.createWebVoucher(form, admin.getId());
            
            redirectAttributes.addFlashAttribute("success", "Tạo voucher thành công!");
            return "redirect:/admin/vouchers";
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("voucherForm", form);
            return "redirect:/admin/vouchers/create";
        }
    }
    
    /**
     * Xóa voucher WEB (admin)
     */
    @PostMapping("/admin/vouchers/delete/{id}")
    public String deleteAdminVoucher(@PathVariable String id,
                                    RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucher(id);
            redirectAttributes.addFlashAttribute("success", "Xóa voucher thành công!");
            return "redirect:/admin/vouchers";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/vouchers";
        }
    }
    
    // ==================== CHECKOUT APPLY VOUCHER ====================
    
    /**
     * Áp dụng voucher khi checkout
     * Validate 3 bước: 1. Tồn tại + active, 2. Còn hạn, 3. Còn lượt
     */
    @PostMapping("/checkout/apply-voucher")
    @ResponseBody
    public Map<String, Object> applyVoucher(@RequestParam String code,
                                            @RequestParam(required = false) String shopId,
                                            @RequestParam(required = false) String productId,
                                            @RequestParam BigDecimal orderTotal) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Voucher voucher = voucherService.validateForCheckout(code, shopId, productId);
            
            // Tính giảm giá
            BigDecimal discount = calculateDiscount(voucher, orderTotal);
            BigDecimal finalTotal = orderTotal.subtract(discount);
            
            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
            }
            
            response.put("success", true);
            response.put("message", "Áp dụng voucher thành công!");
            response.put("voucher", voucher);
            response.put("discount", discount);
            response.put("finalTotal", finalTotal);
            response.put("voucherId", voucher.getId());
            
        } catch (BadRequestException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Voucher không hợp lệ");
        }
        
        return response;
    }
    
    /**
     * Áp dụng voucher khi đặt hàng (từ form POST)
     */
    @PostMapping("/checkout/place-order")
    public String placeOrderWithVoucher(@RequestParam(required = false) String voucherCode,
                                       @RequestParam(required = false) String voucherId,
                                       RedirectAttributes redirectAttributes) {
        if (voucherCode != null && !voucherCode.isBlank()) {
            try {
                Voucher voucher = voucherService.validateForCheckout(voucherCode, null, null);
                // Tăng used count sau khi đặt hàng thành công
                voucherService.incrementUsed(voucher.getId());
                redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Voucher đã được sử dụng.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("warning", "Voucher không hợp lệ: " + e.getMessage());
            }
        }
        // Redirect về trang orders hoặc confirmation
        return "redirect:/web/orders";
    }
    
    /**
     * Tính số tiền được giảm dựa trên loại voucher
     */
    private BigDecimal calculateDiscount(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal discount = BigDecimal.ZERO;
        
        // Kiểm tra minOrderValue
        if (voucher.getMinOrderValue() != null && 
            orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            return BigDecimal.ZERO;
        }
        
        if (voucher.getDiscountType() == com.ecommerce.cnj70.enums.DiscountType.PERCENT) {
            // Tính theo %
            discount = orderTotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            
            // Áp dụng maxDiscountAmount nếu có
            if (voucher.getMaxDiscountAmount() != null && 
                discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else {
            // Giảm theo số tiền cố định
            discount = voucher.getDiscountValue();
        }
        
        // Không vượt quá total
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }
        
        return discount;
    }
}
