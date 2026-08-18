package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.dto.response.OrderHistoryRes;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class OrderController {
    
    private final OrderService orderService;
    private final UserRepository userRepository;
    
    @GetMapping("/checkout")
    public String checkoutPage(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("checkoutReq", new CheckoutReq());
        return "web/checkout";
    }
    
    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal CustomUserDetails user,
                          @ModelAttribute @Valid CheckoutReq request,
                          Model model) {
        if (user == null) {
            return "redirect:/auth/login";
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
