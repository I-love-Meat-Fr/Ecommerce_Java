package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.cart.CartItemValidation;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

        Map<String, BigDecimal> shopSubtotals = itemsByShop.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(Cart.CartItem::getSubtotal)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<String, Integer> shopQuantities = itemsByShop.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().mapToInt(Cart.CartItem::getQuantity).sum(),
                        (a, b) -> a,
                        LinkedHashMap::new));

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
            response.put("errorCode", classifyVoucherError(e.getMessage()));
            response.put("cartSubtotal", cartSubtotal);
            response.put("shippingFee", SHIPPING_FEE);
            response.put("finalTotal", cartSubtotal.add(SHIPPING_FEE));
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("errorCode", "NOT_FOUND");
            response.put("cartSubtotal", cartSubtotal);
            response.put("shippingFee", SHIPPING_FEE);
            response.put("finalTotal", cartSubtotal.add(SHIPPING_FEE));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Voucher không hợp lệ");
            response.put("errorCode", "UNKNOWN");
            response.put("cartSubtotal", cartSubtotal);
            response.put("shippingFee", SHIPPING_FEE);
            response.put("finalTotal", cartSubtotal.add(SHIPPING_FEE));
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Phân loại lỗi voucher để UI chọn icon / màu phù hợp.
     * Map theo message từ {@link com.ecommerce.cnj70.service.VoucherServiceImpl#validateForCheckout}.
     */
    private String classifyVoucherError(String message) {
        if (message == null) return "UNKNOWN";
        String m = message.toLowerCase();
        if (m.contains("vô hiệu hóa")) return "INACTIVE";
        if (m.contains("chưa bắt đầu")) return "NOT_STARTED";
        if (m.contains("hết hạn")) return "EXPIRED";
        if (m.contains("hết lượt")) return "EXHAUSTED";
        if (m.contains("shop này")) return "SHOP_MISMATCH";
        if (m.contains("sản phẩm này")) return "PRODUCT_MISMATCH";
        if (m.contains("không tìm thấy")) return "NOT_FOUND";
        return "INVALID";
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
     * API liệt kê các voucher KHẢ DỤNG cho cart hiện tại, chia làm 2 nhóm:
     *   1) WEB Voucher (toàn sàn) — áp dụng cho tổng đơn (subtotal + shipping)
     *      - Nếu voucher có productIds: chỉ liệt kê khi cart có ít nhất 1 sản phẩm nằm trong danh sách
     *      - Nếu productIds rỗng/null: áp dụng cho mọi đơn
     *   2) SHOP Voucher (theo shop) — chỉ liệt kê voucher thuộc các shop hiện có trong cart;
     *      phạm vi áp dụng = subtotal của shop đó + phần shipping chia theo tỉ lệ shop.
     *
     * Mỗi voucher card trả về:
     *   - code, name, type (WEB|SHOP)
     *   - discountType, discountValue, maxDiscountAmount, minOrderValue
     *   - estimatedDiscount: số tiền giảm dự kiến (đã áp dụng minOrderValue + max cap)
     *   - meetsMinOrder: true/false — UI dùng để khoá/mở nút "Áp dụng"
     *   - remainingQuantity, scopeLabel ("Toàn sàn" / "Cửa hàng X" / "Sản phẩm cụ thể")
     *   - applicableProductCount: số sản phẩm trong cart được voucher bao trùm (cho WEB có productIds)
     *   - endDate: yyyy-MM-dd HH:mm
     *
     * Phục vụ Voucher UI: cho Customer chọn WEB/SHOP Voucher phù hợp, hiển thị rõ phạm vi
     * và điều kiện (đơn tối thiểu, giảm tối đa, hạn dùng, còn lượt).
     */
    @GetMapping("/api/cart/available-vouchers")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> availableVouchers(@AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> response = new HashMap<>();

        if (user == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(response);
        }

        Cart cart = cartService.getCartByUserId(user.getId());
        BigDecimal cartSubtotal = cartService.calculateTotal(cart);

        if (cartSubtotal.signum() <= 0) {
            response.put("success", true);
            response.put("cartSubtotal", BigDecimal.ZERO);
            response.put("shippingFee", SHIPPING_FEE);
            response.put("webVouchers", List.of());
            response.put("shopVouchers", List.of());
            response.put("appliedCode", cartService.getAppliedVoucherCode(user.getId()));
            response.put("message", "Giỏ hàng trống");
            return ResponseEntity.ok(response);
        }

        // Group cart items by shopId, build set of all productIds in cart
        Map<String, List<Cart.CartItem>> itemsByShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getShopId() != null ? item.getShopId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));

        Set<String> cartProductIds = cart.getItems().stream()
                .map(Cart.CartItem::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Subtotal theo shop (chỉ subtotal sản phẩm — không cộng shipping)
        Map<String, BigDecimal> shopSubtotals = new LinkedHashMap<>();
        for (Map.Entry<String, List<Cart.CartItem>> e : itemsByShop.entrySet()) {
            BigDecimal sub = e.getValue().stream()
                    .map(Cart.CartItem::getSubtotal)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            shopSubtotals.put(e.getKey(), sub);
        }

        // Tổng đơn (subtotal + shipping) cho áp dụng WEB Voucher
        BigDecimal orderTotal = cartSubtotal.add(SHIPPING_FEE);

        // ===== WEB Vouchers (toàn sàn) =====
        List<Voucher> allWebVouchers = voucherService.getAvailableWebVouchersForCustomer();
        List<Map<String, Object>> webVoucherList = new ArrayList<>();
        for (Voucher v : allWebVouchers) {
            // Scope check: nếu voucher giới hạn productIds, cart phải có ít nhất 1 sp khớp
            int applicableProductCount = countApplicableProducts(v, cartProductIds);
            boolean isAllProducts = (v.getProductIds() == null || v.getProductIds().isEmpty());
            if (!isAllProducts && applicableProductCount == 0) {
                continue; // bỏ qua — không áp dụng cho bất kỳ sản phẩm nào trong cart
            }
            String scopeLabel = isAllProducts
                    ? "Toàn sàn"
                    : "Sản phẩm cụ thể (" + applicableProductCount + "/" + v.getProductIds().size() + ")";
            webVoucherList.add(toVoucherCard(v, orderTotal, "WEB", scopeLabel, applicableProductCount));
        }

        // ===== SHOP Vouchers (theo từng shop trong cart) =====
        List<Map<String, Object>> shopVoucherList = new ArrayList<>();
        for (Map.Entry<String, List<Cart.CartItem>> e : itemsByShop.entrySet()) {
            String shopKey = e.getKey();
            String shopId = "default".equals(shopKey) ? null : shopKey;
            if (shopId == null) continue; // cart items không có shopId thì không liệt kê shop voucher

            String shopName = e.getValue().get(0).getShopName() != null
                    ? e.getValue().get(0).getShopName()
                    : "Shop";
            List<Voucher> shopVouchers = voucherService.getAvailableVouchersByShop(shopId);

            List<Map<String, Object>> perShopList = new ArrayList<>();
            for (Voucher v : shopVouchers) {
                // Phạm vi áp dụng SHOP Voucher = subtotal của shop + phần shipping tỉ lệ theo shop
                BigDecimal shopSub = shopSubtotals.getOrDefault(shopKey, BigDecimal.ZERO);
                BigDecimal proportionalShipping = BigDecimal.ZERO;
                if (cartSubtotal.signum() > 0) {
                    proportionalShipping = SHIPPING_FEE.multiply(shopSub)
                            .divide(cartSubtotal, 2, RoundingMode.HALF_UP);
                }
                BigDecimal shopOrderTotal = shopSub.add(proportionalShipping);

                String scopeLabel = "Chỉ áp dụng cho " + shopName;
                perShopList.add(toVoucherCard(v, shopOrderTotal, "SHOP", scopeLabel, null));
            }

            // Chỉ trả group khi shop có ít nhất 1 voucher (kể cả bị khoá do minOrderValue)
            // để UI vẫn hiển thị khối shop + chip "Chưa đạt đơn tối thiểu"
            if (!perShopList.isEmpty()) {
                Map<String, Object> shopGroup = new LinkedHashMap<>();
                shopGroup.put("shopId", shopId);
                shopGroup.put("shopKey", shopKey);
                shopGroup.put("shopName", shopName);
                shopGroup.put("shopSubtotal", shopSubtotals.getOrDefault(shopKey, BigDecimal.ZERO));
                shopGroup.put("vouchers", perShopList);
                shopVoucherList.add(shopGroup);
            }
        }

        response.put("success", true);
        response.put("cartSubtotal", cartSubtotal);
        response.put("shippingFee", SHIPPING_FEE);
        response.put("webVouchers", webVoucherList);
        response.put("shopVouchers", shopVoucherList);
        response.put("appliedCode", cartService.getAppliedVoucherCode(user.getId()));
        return ResponseEntity.ok(response);
    }

    /**
     * Đếm số sản phẩm trong cart nằm trong productIds của voucher (nếu voucher có productIds).
     * Trả về 0 nếu voucher không giới hạn sản phẩm.
     */
    private int countApplicableProducts(Voucher voucher, Set<String> cartProductIds) {
        if (voucher.getProductIds() == null || voucher.getProductIds().isEmpty()) {
            return 0;
        }
        int count = 0;
        for (String pid : voucher.getProductIds()) {
            if (cartProductIds.contains(pid)) count++;
        }
        return count;
    }

    /**
     * Build một voucher card (Map) để trả về cho UI. Áp dụng:
     *   - minOrderValue check
     *   - maxDiscountAmount cap (PERCENT)
     *   - estimatedDiscount preview
     */
    private Map<String, Object> toVoucherCard(Voucher v, BigDecimal applicableTotal,
                                               String scopeType, String scopeLabel,
                                               Integer applicableProductCount) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("id", v.getId());
        card.put("code", v.getCode());
        card.put("name", v.getName());
        card.put("scopeType", scopeType);          // "WEB" | "SHOP"
        card.put("scopeLabel", scopeLabel);
        card.put("discountType", v.getDiscountType() != null ? v.getDiscountType().name() : null);
        card.put("discountValue", v.getDiscountValue());
        card.put("maxDiscountAmount", v.getMaxDiscountAmount());
        card.put("minOrderValue", v.getMinOrderValue());
        card.put("remainingQuantity", v.getRemainingQuantity());
        card.put("endDate", v.getEndDate());

        String discountText;
        if (v.getDiscountType() == DiscountType.PERCENT) {
            discountText = "Giảm " + v.getDiscountValue().stripTrailingZeros().toPlainString() + "%";
            if (v.getMaxDiscountAmount() != null) {
                discountText += " (tối đa " + formatPlainMoney(v.getMaxDiscountAmount()) + ")";
            }
        } else {
            discountText = "Giảm " + formatPlainMoney(v.getDiscountValue());
        }
        card.put("discountLabel", discountText);

        boolean meetsMin = true;
        if (v.getMinOrderValue() != null && applicableTotal.compareTo(v.getMinOrderValue()) < 0) {
            meetsMin = false;
        }

        BigDecimal estimated = BigDecimal.ZERO;
        if (meetsMin) {
            estimated = computeDiscount(v, applicableTotal);
            if (estimated.signum() <= 0) meetsMin = false;
        }
        card.put("estimatedDiscount", estimated);
        card.put("meetsMinOrder", meetsMin);
        card.put("applicableProductCount", applicableProductCount);
        return card;
    }

    /** Format BigDecimal thành chuỗi có dấu phân cách hàng nghìn (vd: 50.000). */
    private String formatPlainMoney(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount.doubleValue()).replace(",", ".");
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
