package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/vendor/orders")
@RequiredArgsConstructor
public class VendorOrderController {
    
    private final OrderService orderService;
    
    @GetMapping
    public String orderList(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        List<Order> orders = orderService.getOrdersByShopId(user.getShopId());
        model.addAttribute("orders", orders);
        return "vendor/order-list";
    }
    
    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable String id, @RequestParam OrderStatus status) {
        orderService.updateOrderStatus(id, status);
        return "redirect:/vendor/orders";
    }
}
