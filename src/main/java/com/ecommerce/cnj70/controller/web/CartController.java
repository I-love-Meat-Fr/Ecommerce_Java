package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.VoucherService;
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

    private final CartService cartService;
    private final VoucherService voucherService;

    /** Cộng dồn phí vận chuyển cố định theo quy tắc dự án hiện hữu (15.000đ). */
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("15000");

    @GetMapping("/cart")
    public String cartPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
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

        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("itemsByShop", itemsByShop);

        return "web/cart";
    }

    @PostMapping("/api/cart/add")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCartApi(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam String productId,
                                                           @RequestParam(defaultValue = "1") int quantity) {
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        try {
            Cart cart = cartService.addToCart(user.getId(), productId, quantity);
            int itemCount = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();
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
                            @RequestParam int quantity) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (quantity < 1) {
            return "redirect:/cart";
        }
        cartService.updateCartItem(user.getId(), productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@AuthenticationPrincipal CustomUserDetails user,
                                @RequestParam String productId) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        cartService.removeFromCart(user.getId(), productId);
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
     */
    @PostMapping("/api/cart/apply-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> applyVoucher(@AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam String code) {
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

            BigDecimal discount = computeDiscount(voucher, cartSubtotal);
            BigDecimal finalTotal = cartSubtotal.add(SHIPPING_FEE).subtract(discount);
            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
            }

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
     * Không lưu state voucher ở Cart; client chỉ cần reset hiển thị.
     */
    @PostMapping("/api/cart/remove-voucher")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeVoucher(@AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

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
     * Tính discount từ voucher + subtotal. Logic khớp với VoucherController.calculateDiscount().
     * Chỉ dùng để hiển thị Cart; quyết định cuối cùng vẫn do Voucher module xác nhận.
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
}