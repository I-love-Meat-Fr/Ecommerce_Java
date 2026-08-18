package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        model.addAttribute("cart", cart);
        return "web/cart";
    }
    
    @PostMapping("/cart/add")
    public String addToCart(@AuthenticationPrincipal CustomUserDetails user,
                           @RequestParam String productId,
                           @RequestParam(defaultValue = "1") int quantity) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        cartService.addToCart(user.getId(), productId, quantity);
        return "redirect:/cart";
    }
    
    @PostMapping("/cart/update")
    public String updateCart(@AuthenticationPrincipal CustomUserDetails user,
                            @RequestParam String productId,
                            @RequestParam int quantity) {
        if (user == null) {
            return "redirect:/auth/login";
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
