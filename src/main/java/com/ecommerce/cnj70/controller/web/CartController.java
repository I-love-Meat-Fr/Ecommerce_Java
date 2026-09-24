package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.cart.CartItemValidation;
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

        // ===== Validate real-time: Product/Shop status + stock =====
        // Bất kỳ CartItem nào không còn ACTIVE hoặc Shop không hợp lệ sẽ
        // bị đánh dấu invalid và Cart UI sẽ vô hiệu hóa nút Checkout.
        Map<String, CartItemValidation> invalidItems = cartService.validateCartItems(cart);
        boolean hasInvalidItems = !invalidItems.isEmpty();

        BigDecimal cartTotal = cartService.calculateTotal(cart);
        int totalQuantity = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();

        Map<String, List<Cart.CartItem>> itemsByShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getShopId() != null ? item.getShopId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));

        // ===== Per-shop aggregates =====
        // Subtotal & số lượng của mỗi shop, dùng để hiển thị "Tạm tính của shop này"
        // và badge số lượng trên header shop. Giúp customer thấy rõ mình đang
        // mua bao nhiêu từ từng shop khi giỏ hàng có nhiều shop.
        Map<String, BigDecimal> shopSubtotals = new LinkedHashMap<>();
        Map<String, Integer> shopQuantities = new LinkedHashMap<>();
        for (Map.Entry<String, List<Cart.CartItem>> entry : itemsByShop.entrySet()) {
            String shopId = entry.getKey();
            List<Cart.CartItem> shopItems = entry.getValue();
            BigDecimal shopSub = shopItems.stream()
                    .map(Cart.CartItem::getSubtotal)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            int shopQty = shopItems.stream().mapToInt(Cart.CartItem::getQuantity).sum();
            shopSubtotals.put(shopId, shopSub);
            shopQuantities.put(shopId, shopQty);
        }

        // Apply persisted voucher (nếu có) để hiển thị đúng summary
        AppliedVoucher applied = readAppliedVoucher(session);
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
                // Cập nhật session với voucher mới nhất (tên/used/etc có thể đã đổi)
                applied = new AppliedVoucher(fresh.getCode(), fresh.getId(), fresh.getName(), discount, finalTotal);
                session.setAttribute(SESSION_APPLIED_VOUCHER, applied);
            } catch (Exception e) {
                // Voucher đã bị xóa/hết hạn -> clear session
                session.removeAttribute(SESSION_APPLIED_VOUCHER);
                applied = null;
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("itemsByShop", itemsByShop);
        model.addAttribute("shopSubtotals", shopSubtotals);
        model.addAttribute("shopQuantities", shopQuantities);
        model.addAttribute("shopCount", itemsByShop.size());
        model.addAttribute("shippingFee", SHIPPING_FEE);
        model.addAttribute("appliedVoucher", applied);
        model.addAttribute("discount", discount);
        model.addAttribute("finalTotal", finalTotal);
        model.addAttribute("invalidItems", invalidItems);
        model.addAttribute("hasInvalidItems", hasInvalidItems);
        model.addAttribute("invalidItemCount", invalidItems.size());

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
     * Bulk-remove tất cả CartItem không còn hợp lệ (Product không ACTIVE,
     * Shop bị suspend, hết hàng, v.v.). UI gọi khi user bấm nút
     * "Xóa sản phẩm không hợp lệ".
     */
    @PostMapping("/cart/remove-invalid")
    public String removeInvalidItems(@AuthenticationPrincipal CustomUserDetails user,
                                     HttpSession session) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        cartService.removeInvalidItems(user.getId());
        reEvaluateVoucher(session, user.getId());
        return "redirect:/cart";
    }

    /**
     * Bulk-remove tất cả CartItem thuộc về một Shop. UI gọi khi user bấm nút
     * "Xóa tất cả sản phẩm của shop này" trong từng shop-group.
     * <p>
     * Lưu ý: shopId = "default" sẽ được map sang null khi filter (giữ tương thích
     * với các cart item không có shopId).
     */
    @PostMapping("/cart/remove-by-shop")
    public String removeByShop(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam String shopId,
                               HttpSession session) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        String effectiveShopId = "default".equals(shopId) ? null : shopId;
        cartService.removeByShop(user.getId(), effectiveShopId);
        reEvaluateVoucher(session, user.getId());
        return "redirect:/cart";
    }

    /**
     * API endpoint (AJAX) trả về JSON — cho phép UI làm bulk-remove không cần
     * reload toàn trang. Trả về số lượng item đã xóa.
     */
    @PostMapping("/api/cart/remove-invalid")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeInvalidItemsApi(
            @AuthenticationPrincipal CustomUserDetails user,
            HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }
        Cart before = cartService.getCartByUserId(user.getId());
        Map<String, CartItemValidation> invalidBefore = cartService.validateCartItems(before);
        int invalidCount = invalidBefore.size();
        cartService.removeInvalidItems(user.getId());
        reEvaluateVoucher(session, user.getId());
        response.put("success", true);
        response.put("removedCount", invalidCount);
        response.put("message", invalidCount > 0
                ? "Đã xóa " + invalidCount + " sản phẩm không hợp lệ khỏi giỏ hàng."
                : "Giỏ hàng không có sản phẩm không hợp lệ.");
        return ResponseEntity.ok(response);
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

            // Lưu vào session để Checkout/Order dùng tiếp (TASK #13)
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

        session.removeAttribute(SESSION_APPLIED_VOUCHER);

        Cart cart = cartService.getCartByUserId(user.getId());
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
     * Đọc AppliedVoucher từ session.
     */
    @SuppressWarnings("unchecked")
    private AppliedVoucher readAppliedVoucher(HttpSession session) {
        if (session == null) return null;
        Object attr = session.getAttribute(SESSION_APPLIED_VOUCHER);
        return (attr instanceof AppliedVoucher av) ? av : null;
    }

    /**
     * Sau khi thay đổi Cart (add/update/remove), tính lại discount nếu đang có voucher.
     * Nếu subtotal đổi làm voucher không còn hợp lệ (vd: < minOrderValue), clear session.
     */
    private void reEvaluateVoucher(HttpSession session, String userId) {
        AppliedVoucher applied = readAppliedVoucher(session);
        if (applied == null) return;
        try {
            Voucher fresh = voucherService.getVoucherByCode(applied.code);
            Cart cart = cartService.getCartByUserId(userId);
            BigDecimal subtotal = cartService.calculateTotal(cart).add(SHIPPING_FEE);
            if (subtotal.signum() <= 0) {
                session.removeAttribute(SESSION_APPLIED_VOUCHER);
                return;
            }
            BigDecimal discount = computeDiscount(fresh, subtotal);
            if (discount.signum() <= 0) {
                // không đủ điều kiện nữa (vd: subtotal < minOrderValue) -> clear
                session.removeAttribute(SESSION_APPLIED_VOUCHER);
                return;
            }
            BigDecimal finalTotal = subtotal.subtract(discount);
            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
            }
            session.setAttribute(SESSION_APPLIED_VOUCHER,
                    new AppliedVoucher(fresh.getCode(), fresh.getId(), fresh.getName(), discount, finalTotal));
        } catch (Exception e) {
            // Voucher không còn khả dụng -> clear
            session.removeAttribute(SESSION_APPLIED_VOUCHER);
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
