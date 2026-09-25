package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.response.AdminOrderRes;
import com.ecommerce.cnj70.enums.OrderStatus;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

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
                           @RequestParam(required = false) String status,
                           Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        OrderStatus statusFilter = parseStatus(status);

        Page<Order> result = adminOrderService.listOrders(pageable, q, statusFilter);

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
        model.addAttribute("status", statusFilter == null ? "" : statusFilter.name());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        return "admin/order-list";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable String id,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String status,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        try {
            Order order = adminOrderService.getOrderById(id);
            AdminOrderRes orderRes = adminOrderService.toAdminOrderRes(order);

            model.addAttribute("order", orderRes);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("q", q == null ? "" : q);
            model.addAttribute("status", status == null ? "" : status);
            return "admin/order-detail";
        } catch (Exception ex) {
            // Phase 2 fix (BUG-14.1): dùng RedirectAttributes.addFlashAttribute thay vì
            // Model.addAttribute khi redirect — Model bị Spring discard khi return "redirect:".
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            String redirectUrl = "/admin/orders?page=" + page + "&size=" + size;
            if (q != null && !q.isBlank()) {
                redirectUrl += "&q=" + q;
            }
            if (status != null && !status.isBlank()) {
                redirectUrl += "&status=" + status;
            }
            return "redirect:" + redirectUrl;
        }
    }

    private static OrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String norm = raw.trim().toUpperCase();
        if ("ALL".equals(norm)) return null;
        try {
            return OrderStatus.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static List<Integer> computePageRange(int current, int totalPages) {
        List<Integer> out = new ArrayList<>();
        if (totalPages <= 0) return out;
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }
}
