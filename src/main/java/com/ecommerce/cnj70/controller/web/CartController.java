package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.CartService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

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
}