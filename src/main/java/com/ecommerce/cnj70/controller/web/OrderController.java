package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.dto.response.OrderHistoryRes;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final CartService cartService;
    
    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        User dbUser = userRepository.findById(user.getId()).orElse(null);
        Cart cart = cartService.getCartByUserId(user.getId());
        BigDecimal cartTotal = cartService.calculateTotal(cart);
        int totalQuantity = cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum();
        
        Map<String, List<Cart.CartItem>> itemsByShop = cart.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> item.getShopId() != null ? item.getShopId() : "default",
                        LinkedHashMap::new,
                        Collectors.toList()));
        
        // Pre-fill form with user data
        CheckoutReq checkoutReq = CheckoutReq.builder()
                .phone(dbUser != null ? dbUser.getPhone() : null)
                .shippingAddress(dbUser != null ? dbUser.getAddress() : null)
                .build();
        
        model.addAttribute("checkoutReq", checkoutReq);
        model.addAttribute("cart", cart);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("totalQuantity", totalQuantity);
        model.addAttribute("itemsByShop", itemsByShop);
        model.addAttribute("user", dbUser);
        
        return "web/checkout";
    }
    
    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal CustomUserDetails user,
                          @ModelAttribute @Valid CheckoutReq request,
                          BindingResult bindingResult,
                          Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        // Reload cart and user data for re-render
        User dbUser = userRepository.findById(user.getId()).orElse(null);
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
        model.addAttribute("user", dbUser);
        
        // If validation errors, return to checkout with errors
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Vui lòng điền đầy đủ thông tin bắt buộc.");
            return "web/checkout";
        }
        
        // If cart is empty, redirect back
        if (cart.getItems().isEmpty()) {
            model.addAttribute("error", "Giỏ hàng của bạn đang trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
            return "web/checkout";
        }
        
        try {
            Order order = orderService.createOrder(user.getId(), request);
            return "redirect:/orders/" + order.getId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "web/checkout";
        }
    }
    
    @GetMapping("/orders")
    public String orderHistoryPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        List<Order> orders = orderService.getOrdersByUserId(user.getId());
        List<OrderHistoryRes> orderHistory = orders.stream()
                .map(order -> OrderHistoryRes.builder()
                        .orderId(order.getId())
                        .orderDate(order.getCreatedAt())
                        .status(order.getStatus())
                        .totalAmount(order.getTotalAmount())
                        .itemCount(order.getItems().size())
                        .shopName(order.getShopName())
                        .productImages(order.getItems().stream()
                                .map(Order.OrderItem::getImageUrl)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
        
        model.addAttribute("orders", orderHistory);
        return "web/order-history";
    }
}
