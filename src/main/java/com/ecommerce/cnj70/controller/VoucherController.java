package com.ecommerce.cnj70.controller;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.VendorService;
import com.ecommerce.cnj70.service.VoucherApplicationGateway;
import com.ecommerce.cnj70.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final VoucherApplicationGateway voucherApplicationGateway;
    private final VendorService vendorService;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    
    // ==================== TRANG CÔNG KHAI ====================
    
    /**
     * Trang công khai - hiển thị WEB Voucher khả dụng.
     * Chỉ hiển thị type=WEB (voucher toàn hệ thống), không hiển thị SHOP Voucher.
     */
    @GetMapping("/vouchers")
    public String vouchersPage(Model model) {
        List<Voucher> availableVouchers = voucherService.getAvailableWebVouchersForCustomer();
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
     * Phase 12: Danh sách voucher WEB (của admin) — Search + Filter + Pagination 5/trang.
     * Chỉ hiển thị WEB Voucher (type=WEB, shopId=null); SHOP Voucher không xuất hiện.
     */
    @GetMapping("/admin/vouchers")
    public String adminVoucherList(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "5") int size,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) String active,
                                   Model model) {
        int safeSize = (size <= 0) ? 5 : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Boolean activeFilter = parseVoucherActive(active);
        Page<Voucher> result = voucherService.getWebVouchers(pageable, q, activeFilter);

        model.addAttribute("vouchers", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("active", activeFilter == null ? "" : (activeFilter ? "true" : "false"));
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        return "admin/voucher-list";
    }

    private static java.util.List<Integer> computePageRange(int current, int totalPages) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        if (totalPages <= 0) return out;
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }

    private static Boolean parseVoucherActive(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String norm = raw.trim().toLowerCase();
        if ("all".equals(norm)) return null;
        if ("true".equals(norm) || "1".equals(norm)) return Boolean.TRUE;
        if ("false".equals(norm) || "0".equals(norm)) return Boolean.FALSE;
        return null;
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

    // -------- Phase 11: Admin Edit --------

    /**
     * Phase 11 — Form edit voucher WEB cho admin.
     */
    @GetMapping("/admin/vouchers/edit/{id}")
    public String editAdminVoucherForm(@PathVariable String id,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "5") int size,
                                      @RequestParam(required = false) String q,
                                      @RequestParam(required = false) String active,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            Voucher voucher = voucherService.getVoucherById(id);

            // Guard: chỉ cho phép edit WEB Voucher
            if (voucher.getType() != com.ecommerce.cnj70.enums.VoucherType.WEB) {
                redirectAttributes.addFlashAttribute("error",
                        "Admin không được phép chỉnh sửa Voucher SHOP. Voucher này thuộc về Vendor.");
                return "redirect:/admin/vouchers";
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
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("q", q == null ? "" : q);
            model.addAttribute("active", active == null ? "" : active);
            return "admin/voucher-edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/vouchers";
        }
    }

    @PostMapping("/admin/vouchers/edit/{id}")
    public String editAdminVoucher(@PathVariable String id,
                                   @Valid @ModelAttribute("voucherForm") VoucherFormReq form,
                                   BindingResult result,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "5") int size,
                                   @RequestParam(required = false) String q,
                                   @RequestParam(required = false) String active,
                                   RedirectAttributes redirectAttributes) {
        try {
            voucherService.updateWebVoucher(id, form);
            redirectAttributes.addFlashAttribute("success", "Cập nhật voucher thành công!");
            return "redirect:/admin/vouchers?page=" + page + "&size=" + size
                    + (q != null && !q.isBlank() ? "&q=" + q : "")
                    + (active != null && !active.isBlank() ? "&active=" + active : "");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("voucherForm", form);
            return "redirect:/admin/vouchers/edit/" + id + "?page=" + page + "&size=" + size
                    + (q != null && !q.isBlank() ? "&q=" + q : "")
                    + (active != null && !active.isBlank() ? "&active=" + active : "");
        }
    }

    // -------- Phase 11: Admin Delete / Deactivate --------

    /**
     * Phase 11 — Soft delete voucher WEB (admin).
     * Nếu là SHOP Voucher → từ chối, không xóa.
     */
    @PostMapping("/admin/vouchers/delete/{id}")
    public String deleteAdminVoucher(@PathVariable String id,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "5") int size,
                                    @RequestParam(required = false) String q,
                                    @RequestParam(required = false) String active,
                                    RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteWebVoucher(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa (tạm dừng) voucher thành công!");
            return "redirect:/admin/vouchers?page=" + page + "&size=" + size
                    + (q != null && !q.isBlank() ? "&q=" + q : "")
                    + (active != null && !active.isBlank() ? "&active=" + active : "");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/vouchers";
        }
    }

    // -------- Phase 11: Admin Activate --------

    /**
     * Phase 11 — Activate voucher WEB (admin).
     * Set active = true. Từ chối nếu là SHOP Voucher.
     */
    @PostMapping("/admin/vouchers/activate/{id}")
    public String activateAdminVoucher(@PathVariable String id,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "5") int size,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(required = false) String active,
                                       RedirectAttributes redirectAttributes) {
        try {
            voucherService.activateWebVoucher(id);
            redirectAttributes.addFlashAttribute("success", "Đã kích hoạt voucher!");
            return "redirect:/admin/vouchers?page=" + page + "&size=" + size
                    + (q != null && !q.isBlank() ? "&q=" + q : "")
                    + (active != null && !active.isBlank() ? "&active=" + active : "");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/vouchers";
        }
    }

    /**
     * Phase 11 — Deactivate voucher WEB (admin).
     * Set active = false. Từ chối nếu là SHOP Voucher.
     */
    @PostMapping("/admin/vouchers/deactivate/{id}")
    public String deactivateAdminVoucher(@PathVariable String id,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "5") int size,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) String active,
                                         RedirectAttributes redirectAttributes) {
        try {
            voucherService.deactivateWebVoucher(id);
            redirectAttributes.addFlashAttribute("success", "Đã tạm dừng voucher!");
            return "redirect:/admin/vouchers?page=" + page + "&size=" + size
                    + (q != null && !q.isBlank() ? "&q=" + q : "")
                    + (active != null && !active.isBlank() ? "&active=" + active : "");
        } catch (BadRequestException e) {
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
     * Áp dụng voucher khi đặt hàng (từ form POST).
     *
     * <p>Phase 4B — atomic voucher consumption through
     * {@link VoucherApplicationGateway#reserveVoucher}. The voucher is only
     * consumed if and when the order is successfully created. If the order
     * creation fails (Phase 5 / Order Backend owns), the caller MUST call
     * {@code releaseVoucher} to roll back the usedCount increment.</p>
     */
    @PostMapping("/checkout/place-order")
    public String placeOrderWithVoucher(@RequestParam(required = false) String voucherCode,
                                       @RequestParam(required = false) String voucherId,
                                       RedirectAttributes redirectAttributes) {
        if (voucherCode != null && !voucherCode.isBlank()) {
            try {
                Voucher voucher = voucherService.validateForCheckout(voucherCode, null, null);

                // Phase 4B — atomic, race-safe increment via gateway.
                // Order Backend (Phase 5) will own the full reserve/release
                // lifecycle including rollback on order failure.
                String orderRef = "ORDER_PLACEHOLDER";
                boolean reserved = voucherApplicationGateway.reserveVoucher(voucher.getId(), orderRef);
                if (!reserved) {
                    redirectAttributes.addFlashAttribute("warning",
                            "Voucher '" + voucher.getCode() + "' đã hết lượt hoặc bị vô hiệu hóa trước khi đặt hàng.");
                } else {
                    redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Voucher đã được sử dụng.");
                }
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
