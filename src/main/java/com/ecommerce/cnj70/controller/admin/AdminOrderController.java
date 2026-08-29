package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.response.AdminOrderRes;
import com.ecommerce.cnj70.service.AdminOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final int DEFAULT_PAGE_SIZE = 5;

    private final AdminOrderService adminOrderService;

    @GetMapping
    public String orderList(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           @RequestParam(required = false) String q,
                           Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> result = adminOrderService.listOrders(pageable, q);

        model.addAttribute("orders", result.getContent().stream()
                .map(adminOrderService::toAdminOrderRes)
                .toList());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("q", q == null ? "" : q);
        return "admin/order-list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable String id,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size,
                              @RequestParam(required = false) String q,
                              Model model) {
        try {
            Order order = adminOrderService.getOrderById(id);
            AdminOrderRes orderRes = adminOrderService.toAdminOrderRes(order);

            model.addAttribute("order", orderRes);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("q", q == null ? "" : q);
            return "admin/order-detail";
        } catch (Exception ex) {
            model.addAttribute("error", ex.getMessage());
            return "redirect:/admin/orders";
        }
    }
}
