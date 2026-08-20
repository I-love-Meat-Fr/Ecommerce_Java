package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.response.VendorOrderRes;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/vendor/orders")
@RequiredArgsConstructor
public class VendorOrderController {
    
    private final VendorService vendorService;
    private final OrderRepository orderRepository;
    
    @GetMapping
    public String orderList(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        String shopId = vendorService.getShopIdFromUser(user);
        
        List<Order> allOrders = orderRepository.findAll();
        List<Order> vendorOrders = allOrders.stream()
                .filter(order -> order.getItems() != null && 
                        order.getItems().stream()
                                .anyMatch(item -> shopId.equals(item.getShopId())))
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() == null || o2.getCreatedAt() == null) return 0;
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                })
                .toList();
        
        List<VendorOrderRes> orderResponses = vendorOrders.stream()
                .map(order -> mapToVendorOrderRes(order, shopId))
                .toList();
        
        model.addAttribute("orders", orderResponses);
        return "vendor/order-list";
    }
    
    @GetMapping("/{id}")
    public String orderDetail(@AuthenticationPrincipal CustomUserDetails user,
                            @PathVariable String id,
                            Model model) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            
            String shopId = vendorService.getShopIdFromUser(user);
            
            VendorOrderRes vendorOrderRes = mapToVendorOrderRes(order, shopId);
            
            if (vendorOrderRes.getItems().isEmpty()) {
                model.addAttribute("error", "Bạn không có quyền xem đơn hàng này");
                return "redirect:/vendor/orders";
            }
            
            model.addAttribute("order", vendorOrderRes);
            model.addAttribute("statuses", OrderStatus.values());
            return "vendor/order-detail";
        } catch (BadRequestException e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/vendor/orders";
        }
    }
    
    @PostMapping("/{id}/status")
    public String updateOrderStatus(@AuthenticationPrincipal CustomUserDetails user,
                                   @PathVariable String id,
                                   @RequestParam OrderStatus status,
                                   RedirectAttributes redirectAttributes) {
        try {
            Order order = orderRepository.findById(id)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy đơn hàng"));
            
            String shopId = vendorService.getShopIdFromUser(user);
            
            boolean hasItemsFromShop = order.getItems() != null && 
                    order.getItems().stream()
                            .anyMatch(item -> shopId.equals(item.getShopId()));
            
            if (!hasItemsFromShop) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền cập nhật đơn hàng này");
                return "redirect:/vendor/orders";
            }
            
            order.setStatus(status);
            
            if (status == OrderStatus.DELIVERED) {
                order.setDeliveredAt(java.time.LocalDateTime.now());
            }
            
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái đơn hàng thành công!");
        } catch (BadRequestException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vendor/orders/" + id;
    }
    
    private VendorOrderRes mapToVendorOrderRes(Order order, String shopId) {
        List<Order.OrderItem> shopItems = order.getItems().stream()
                .filter(item -> shopId.equals(item.getShopId()))
                .toList();
        
        List<VendorOrderRes.OrderItemRes> itemResponses = shopItems.stream()
                .map(item -> VendorOrderRes.OrderItemRes.builder()
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .imageUrl(item.getImageUrl())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();
        
        BigDecimal shopSubtotal = shopItems.stream()
                .map(Order.OrderItem::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return VendorOrderRes.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .userName(order.getUserName())
                .userEmail(order.getUserEmail())
                .userPhone(order.getUserPhone())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .subtotal(shopSubtotal)
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paid(order.isPaid())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .deliveredAt(order.getDeliveredAt())
                .build();
    }
}
