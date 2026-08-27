package com.ecommerce.cnj70.controller;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;
    private final ShopRepository shopRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    // ==================== TRANG CÔNG KHAI ====================

    /**
     * Trang danh sách voucher công khai.
     * Có thể truyền ?shopId=xxx để lọc theo shop cụ thể (dùng ở trang shop).
     */
    @GetMapping("/vouchers")
    public String listVouchers(@RequestParam(required = false) String shopId,
                                Model model) {
        List<Voucher> vouchers;
        if (shopId != null && !shopId.isBlank()) {
            // Lấy voucher của shop + voucher WEB (toàn sàn)
            List<Voucher> shopVouchers = voucherService.findAvailableByShopId(shopId);
            List<Voucher> webVouchers = voucherService.findAvailableWeb();
            vouchers = new ArrayList<>();
            vouchers.addAll(shopVouchers);
            vouchers.addAll(webVouchers);
            model.addAttribute("filterShopId", shopId);
        } else {
            // Tất cả voucher WEB + SHOP đang khả dụng
            vouchers = voucherService.findAvailable();
        }
        model.addAttribute("vouchers", vouchers);
        return "vouchers/list";
    }

    // ==================== VENDOR ====================

    /**
     * Trang tạo voucher SHOP cho vendor.
     * Tự động lấy shopId của vendor đang đăng nhập.
     */
    @GetMapping("/vendor/vouchers/create")
    public String vendorCreateForm(Authentication auth, Model model) {
        Shop shop = shopRepository.findByOwnerId(auth.getName()).orElse(null);
        model.addAttribute("voucher", new Voucher());
        model.addAttribute("shop", shop);
        model.addAttribute("isEdit", false);
        return "vendor/vouchers/create";
    }

    /**
     * Vendor tạo voucher SHOP — tự động gắn shopId.
     */
    @PostMapping("/vendor/vouchers/create")
    public String vendorCreateSubmit(@Valid @ModelAttribute VoucherFormReq req,
                                     Authentication auth,
                                     Model model) {
        try {
            Shop shop = shopRepository.findByOwnerId(auth.getName())
                    .orElseThrow(() -> new BadRequestException("Bạn chưa có cửa hàng. Vui lòng tạo shop trước."));
            Voucher voucher = buildVoucher(req, auth.getName(), shop.getId());
            voucherService.save(voucher);
            return "redirect:/vendor/vouchers/create?success=true";
        } catch (BadRequestException e) {
            Shop shop = shopRepository.findByOwnerId(auth.getName()).orElse(null);
            model.addAttribute("shop", shop);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("voucher", req);
            return "vendor/vouchers/create";
        }
    }

    // ==================== ADMIN ====================

    /**
     * Trang tạo voucher WEB cho admin.
     */
    @GetMapping("/admin/vouchers/create")
    public String adminCreateForm(Model model) {
        model.addAttribute("voucher", new Voucher());
        model.addAttribute("isEdit", false);
        return "admin/vouchers/create";
    }

    /**
     * Admin tạo voucher WEB — shopId = null.
     */
    @PostMapping("/admin/vouchers/create")
    public String adminCreateSubmit(@Valid @ModelAttribute VoucherFormReq req,
                                    Authentication auth,
                                    Model model) {
        try {
            Voucher voucher = buildVoucher(req, auth.getName(), null);
            voucherService.save(voucher);
            return "redirect:/admin/vouchers/create?success=true";
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("voucher", req);
            return "admin/vouchers/create";
        }
    }

    // ==================== CHECKOUT - ÁP DỤNG VOUCHER ====================

    /**
     * Khách nhập mã voucher -> validate đầy đủ:
     * 1. Tồn tại + active
     * 2. Chưa hết hạn
     * 3. Còn lượt (used < quantity)
     * 4. Nếu là voucher SHOP thì shopId phải khớp với shop của sản phẩm
     */
    @PostMapping("/checkout/apply-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyVoucher(@RequestParam String code,
                                                            @RequestParam(required = false) String productId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Voucher voucher = voucherService.findByCode(code);

            if (!voucher.isActive()) {
                return badResponse(response, "Voucher đã bị vô hiệu hóa");
            }

            LocalDateTime now = LocalDateTime.now();
            if (voucher.getStartDate() != null && now.isBefore(voucher.getStartDate())) {
                return badResponse(response, "Voucher chưa đến ngày sử dụng");
            }
            if (voucher.getEndDate() != null && now.isAfter(voucher.getEndDate())) {
                return badResponse(response, "Voucher đã hết hạn");
            }

            if (voucher.getUsed() >= voucher.getQuantity()) {
                return badResponse(response, "Voucher đã hết lượt sử dụng");
            }

            // Nếu là voucher SHOP -> phải đúng shop của sản phẩm
            if (voucher.getShopId() != null && !voucher.getShopId().isBlank() && productId != null) {
                Product product = productService.getProductById(productId);
                if (!voucher.getShopId().equals(product.getShopId())) {
                    return badResponse(response, "Voucher này chỉ áp dụng cho cửa hàng khác");
                }
            }

            response.put("success", true);
            response.put("voucher", voucher);
            response.put("message", "Áp dụng voucher thành công!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return badResponse(response, "Mã voucher không hợp lệ");
        }
    }

    /**
     * Trừ lượt voucher sau khi đặt hàng thành công
     */
    @PostMapping("/checkout/place-order")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> placeOrder(@RequestParam String voucherId,
                                                            @RequestParam BigDecimal orderTotal) {
        Map<String, Object> response = new HashMap<>();
        try {
            Voucher voucher = voucherService.findById(voucherId);

            if (voucher.getUsed() >= voucher.getQuantity()) {
                return badResponse(response, "Voucher đã hết lượt");
            }

            voucherService.incrementUsed(voucherId);
            response.put("success", true);
            response.put("message", "Đặt hàng thành công, voucher đã được sử dụng");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return badResponse(response, "Có lỗi xảy ra: " + e.getMessage());
        }
    }

    // ==================== API PUBLIC CHO TRANG SẢN PHẨM ====================

    /**
     * API lấy danh sách voucher khả dụng cho 1 sản phẩm:
     * - Voucher của shop bán sản phẩm đó
     * - Voucher WEB (toàn sàn)
     */
    @GetMapping("/api/vouchers/for-product")
    @ResponseBody
    public ResponseEntity<List<Voucher>> vouchersForProduct(@RequestParam String productId) {
        try {
            Product product = productService.getProductById(productId);
            List<Voucher> shopVouchers = voucherService.findAvailableByShopId(product.getShopId());
            List<Voucher> webVouchers = voucherService.findAvailableWeb();
            List<Voucher> all = new ArrayList<>();
            all.addAll(shopVouchers);
            all.addAll(webVouchers);
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    // ==================== HELPER ====================

    private Voucher buildVoucher(VoucherFormReq req, String createdBy, String shopId) {
        return Voucher.builder()
                .code(req.getCode().toUpperCase().trim())
                .name(req.getName())
                .type(req.getType())
                .shopId(shopId)
                .discountType(req.getDiscountType())
                .discountValue(req.getDiscountValue())
                .minOrderValue(req.getMinOrderValue() == null ? BigDecimal.ZERO : req.getMinOrderValue())
                .quantity(req.getQuantity())
                .used(0)
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .active(true)
                .createdBy(createdBy)
                .build();
    }

    private ResponseEntity<Map<String, Object>> badResponse(Map<String, Object> response, String message) {
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }
}