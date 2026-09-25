package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.VoucherService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CartController {

    /** Cộng dồn phí vận chuyển cố định theo quy tắc dự án hiện hữu (15.000đ). */
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("15000");

    /** Session key lưu Voucher đang áp dụng cho Cart. */
    public static final String SESSION_APPLIED_VOUCHER = "appliedVoucher";

    private final CartService cartService;
    private final VoucherService voucherService;

    @GetMapping("/cart")
    public String cartPage(@AuthenticationPrincipal CustomUserDetails user, Model model,
                           HttpSession session) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        Cart cart = cartService.getCartByUserId(user.getId());
        BigDecimal cartTotal = cartService.calculateTotal(cart);
        int totalQuantity = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();

        Map<String, List<Cart.CartItem>> itemsByShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getShopId() != null ? item.getShopId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));

        // Apply persisted voucher (nếu có). State đọc từ Cart document trong MongoDB (KHÔNG
        // từ HttpSession vì app chạy SessionCreationPolicy.STATELESS) - đảm bảo voucher
        // được giữ xuyên qua add/update/remove qty. Nếu code còn trên Cart nhưng voucher
        // bị xóa/hết hạn -> clear code trên Cart để khỏi hiển thị sai.
        AppliedVoucher applied = readAppliedVoucher(user.getId());
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal finalTotal = cartTotal.add(SHIPPING_FEE);
        if (applied != null) {
            try {
                Voucher fresh = voucherService.getVoucherByCode(applied.code);
                discount = computeDiscount(fresh, cartTotal.add(SHIPPING_FEE));
                finalTotal = cartTotal.add(SHIPPING_FEE).subtract(discount);
                if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                    finalTotal = BigDecimal.ZERO;
                }
                applied = new AppliedVoucher(fresh.getCode(), fresh.getId(), fresh.getName(), discount, finalTotal);
                // Mirror sang session cho OrderController.checkout() còn đọc được
                if (session != null) {
                    session.setAttribute(SESSION_APPLIED_VOUCHER, applied);
                }
            } catch (Exception e) {
                // Voucher đã bị xóa/hết hạn -> clear persisted code trên Cart + session
                cartService.setAppliedVoucherCode(user.getId(), null);
                if (session != null) {
                    session.removeAttribute(SESSION_APPLIED_VOUCHER);
                }
                applied = null;
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("itemsByShop", itemsByShop);
        model.addAttribute("shippingFee", SHIPPING_FEE);
        model.addAttribute("appliedVoucher", applied);
        model.addAttribute("discount", discount);
        model.addAttribute("finalTotal", finalTotal);

        return "web/cart";
    }

    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCartApi(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam String productId,
                                                           @RequestParam(defaultValue = "1") int quantity,
                                                           HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Cart cart = cartService.addToCart(user.getId(), productId, quantity);
            int itemCount = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();
            // Sau khi thêm item, subtotal có thể đã đổi -> re-evaluate voucher discount
            reEvaluateVoucher(session, user.getId());
            response.put("success", true);
            response.put("message", "Đã thêm sản phẩm vào giỏ hàng");
            response.put("itemCount", itemCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/api/cart/count")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> cartCountApi(@AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("itemCount", 0);
            response.put("totalQuantity", 0);
            return ResponseEntity.ok(response);
        }
        Cart cart = cartService.getCartByUserId(user.getId());
        int totalQuantity = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();
        response.put("itemCount", cart.getItems().size());
        response.put("totalQuantity", totalQuantity);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cart/update")
    public String updateCart(@AuthenticationPrincipal CustomUserDetails user,
                            @RequestParam String productId,
                            @RequestParam int quantity,
                            HttpSession session) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (quantity < 1) {
            return "redirect:/cart";
        }
        cartService.updateCartItem(user.getId(), productId, quantity);
        reEvaluateVoucher(session, user.getId());
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@AuthenticationPrincipal CustomUserDetails user,
                                @RequestParam String productId,
                                HttpSession session) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        cartService.removeFromCart(user.getId(), productId);
        reEvaluateVoucher(session, user.getId());
        return "redirect:/cart";
    }

    /**
     * Áp dụng voucher cho Cart.
     *
     * Cart chỉ gửi voucherCode từ client; subtotal và discount được tính phía server
     * dựa trên Cart hiện tại + VoucherService hiện hữu.
     *
     * Flow:
     *   client voucherCode
     *     → server: cartService.calculateTotal()   (server-side subtotal, không tin client)
     *     → VoucherService.validateForCheckout()  (Voucher module validate)
     *     → server: compute discount + final total
     *     → client: discount + finalTotal
     *     → server: lưu vào HttpSession để Checkout/Order dùng tiếp
     */
    @PostMapping("/api/cart/apply-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyVoucher(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam String code,
                                                           HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        if (code == null || code.isBlank()) {
            response.put("success", false);
            response.put("message", "Vui lòng nhập mã giảm giá");
            return ResponseEntity.badRequest().body(response);
        }

        Cart cart = cartService.getCartByUserId(user.getId());
        BigDecimal cartSubtotal = cartService.calculateTotal(cart);

        if (cartSubtotal.signum() <= 0) {
            response.put("success", false);
            response.put("message", "Giỏ hàng trống, không thể áp dụng voucher");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Voucher voucher = voucherService.validateForCheckout(code.trim(), null, null);

            BigDecimal discount = computeDiscount(voucher, cartSubtotal.add(SHIPPING_FEE));
            BigDecimal finalTotal = cartSubtotal.add(SHIPPING_FEE).subtract(discount);
            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
            }

            // Lưu vào Cart document (MongoDB) là nguồn chính - persist qua mọi request.
            cartService.setAppliedVoucherCode(user.getId(), voucher.getCode());

            // Mirror sang session để OrderController.checkout() còn đọc được (legacy code path)
            session.setAttribute(SESSION_APPLIED_VOUCHER,
                    new AppliedVoucher(voucher.getCode(), voucher.getId(), voucher.getName(), discount, finalTotal));

            response.put("success", true);
            response.put("message", "Áp dụng voucher thành công");
            response.put("voucherId", voucher.getId());
            response.put("voucherCode", voucher.getCode());
            response.put("voucherName", voucher.getName());
            response.put("voucherShopName", voucher.getShopName());
            response.put("discount", discount);
            response.put("cartSubtotal", cartSubtotal);
            response.put("shippingFee", SHIPPING_FEE);
            response.put("finalTotal", finalTotal);
            return ResponseEntity.ok(response);
        } catch (BadRequestException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("cartSubtotal", cartSubtotal);
            response.put("shippingFee", SHIPPING_FEE);
            response.put("finalTotal", cartSubtotal.add(SHIPPING_FEE));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Voucher không hợp lệ");
            response.put("cartSubtotal", cartSubtotal);
            response.put("shippingFee", SHIPPING_FEE);
            response.put("finalTotal", cartSubtotal.add(SHIPPING_FEE));
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Trả cart về trạng thái không voucher.
     * Xóa voucher khỏi session luôn.
     */
    @PostMapping("/api/cart/remove-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeVoucher(@AuthenticationPrincipal CustomUserDetails user,
                                                            HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        Cart cart = cartService.getCartByUserId(user.getId());
        cartService.setAppliedVoucherCode(user.getId(), null);
        session.removeAttribute(SESSION_APPLIED_VOUCHER);
        BigDecimal cartSubtotal = cartService.calculateTotal(cart);
        BigDecimal finalTotal = cartSubtotal.add(SHIPPING_FEE);

        response.put("success", true);
        response.put("message", "Đã hủy voucher");
        response.put("discount", BigDecimal.ZERO);
        response.put("cartSubtotal", cartSubtotal);
        response.put("shippingFee", SHIPPING_FEE);
        response.put("finalTotal", finalTotal);
        return ResponseEntity.ok(response);
    }

    /**
     * Tính discount từ voucher + subtotal. Logic khớp với VoucherController.calculateDiscount()
     * và OrderServiceImpl.computeDiscount().
     */
    private BigDecimal computeDiscount(Voucher voucher, BigDecimal orderTotal) {
        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getMinOrderValue() != null
                && orderTotal.compareTo(voucher.getMinOrderValue()) < 0) {
            return BigDecimal.ZERO;
        }
        if (voucher.getDiscountType() == DiscountType.PERCENT) {
            discount = orderTotal.multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscountAmount() != null
                    && discount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discount = voucher.getMaxDiscountAmount();
            }
        } else if (voucher.getDiscountType() == DiscountType.AMOUNT) {
            discount = voucher.getDiscountValue();
        }
        if (discount.compareTo(orderTotal) > 0) {
            discount = orderTotal;
        }
        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Đọc AppliedVoucher (cached sub-document) từ Cart document trong MongoDB.
     * Cart.appliedVoucherCode giữ mã voucher; voucher object + discount/finalTotal được
     * tính lại fresh mỗi lần (không cache) để khỏi bị stale.
     */
    private AppliedVoucher readAppliedVoucher(String userId) {
        if (userId == null) return null;
        String code = cartService.getAppliedVoucherCode(userId);
        if (code == null || code.isBlank()) return null;
        return new AppliedVoucher(code, null, null, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /**
     * Sau khi thay đổi Cart (add/update/remove), tính lại discount nếu đang có voucher.
     * Nếu subtotal đổi làm voucher không còn hợp lệ (vd: < minOrderValue), clear session.
     */
    private void reEvaluateVoucher(HttpSession session, String userId) {
        // Đọc applied code từ Cart document (persisted) thay vì HttpSession.
        String appliedCode = cartService.getAppliedVoucherCode(userId);
        if (appliedCode == null || appliedCode.isBlank()) {
            if (session != null) session.removeAttribute(SESSION_APPLIED_VOUCHER);
            return;
        }
        try {
            Voucher fresh = voucherService.getVoucherByCode(appliedCode);
            Cart cart = cartService.getCartByUserId(userId);
            BigDecimal subtotal = cartService.calculateTotal(cart).add(SHIPPING_FEE);
            if (subtotal.signum() <= 0) {
                cartService.setAppliedVoucherCode(userId, null);
                if (session != null) session.removeAttribute(SESSION_APPLIED_VOUCHER);
                return;
            }
            BigDecimal discount = computeDiscount(fresh, subtotal);
            if (discount.signum() <= 0) {
                // không đủ điều kiện nữa (vd: subtotal < minOrderValue) -> clear
                cartService.setAppliedVoucherCode(userId, null);
                if (session != null) session.removeAttribute(SESSION_APPLIED_VOUCHER);
                return;
            }
            BigDecimal finalTotal = subtotal.subtract(discount);
            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
            }
            // Mirror lên session để OrderController.checkout() còn đọc được
            if (session != null) {
                session.setAttribute(SESSION_APPLIED_VOUCHER,
                        new AppliedVoucher(fresh.getCode(), fresh.getId(), fresh.getName(), discount, finalTotal));
            }
        } catch (Exception e) {
            // Voucher không còn khả dụng -> clear persisted code trên Cart + session
            cartService.setAppliedVoucherCode(userId, null);
            if (session != null) session.removeAttribute(SESSION_APPLIED_VOUCHER);
        }
    }

    /**
     * DTO session-scope lưu thông tin voucher đang áp dụng.
     * Implements Serializable để Spring Session có thể persist nếu cần.
     */
    public static class AppliedVoucher implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private final String code;
        private final String voucherId;
        private final String voucherName;
        private final BigDecimal discount;
        private final BigDecimal finalTotal;

        public AppliedVoucher(String code, String voucherId, String voucherName,
                              BigDecimal discount, BigDecimal finalTotal) {
            this.code = code;
            this.voucherId = voucherId;
            this.voucherName = voucherName;
            this.discount = discount;
            this.finalTotal = finalTotal;
        }

        public String getCode() { return code; }
        public String getVoucherId() { return voucherId; }
        public String getVoucherName() { return voucherName; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal getFinalTotal() { return finalTotal; }
    }
}
