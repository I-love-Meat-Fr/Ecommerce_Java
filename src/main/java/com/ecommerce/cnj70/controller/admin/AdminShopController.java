package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AdminShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private static final int DEFAULT_PAGE_SIZE = 5;

    private final AdminShopService adminShopService;
    private final UserRepository userRepository;

    @GetMapping
    public String shopList(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           @RequestParam(required = false) String q,
                           Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        Page<Shop> result = adminShopService.listShops(pageable, q);

        model.addAttribute("shops", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", (q == null) ? "" : q);
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        return "admin/shop-list";
    }

    @GetMapping("/{id}")
    public String shopDetail(@PathVariable String id,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "5") int size,
                            @RequestParam(required = false) String q,
                            Model model) {
        Shop shop = adminShopService.getShopById(id);

        User owner = null;
        if (shop.getOwnerId() != null && !shop.getOwnerId().isBlank()) {
            owner = userRepository.findById(shop.getOwnerId()).orElse(null);
        }

        model.addAttribute("shop", shop);
        model.addAttribute("owner", owner);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", (q == null) ? "" : q);
        return "admin/shop-detail";
    }

    @PostMapping("/{id}/approve")
    public String approveShop(@PathVariable String id,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size,
                              @RequestParam(required = false) String q,
                              RedirectAttributes redirectAttributes) {
        Shop shop;
        try {
            shop = adminShopService.getShopById(id);
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/shops";
        }

        if (shop.getStatus() == ShopStatus.APPROVED) {
            redirectAttributes.addFlashAttribute("flashInfo",
                    "Shop \"" + shop.getShopName() + "\" đã được duyệt trước đó");
        } else {
            try {
                adminShopService.approveShop(id);
                redirectAttributes.addFlashAttribute("flashSuccess",
                        "Đã duyệt shop \"" + shop.getShopName() + "\" thành công");
            } catch (BusinessException ex) {
                redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            }
        }
        String redirectUrl = "/admin/shops?page=" + page + "&size=" + size;
        if (q != null && !q.isBlank()) {
            redirectUrl += "&q=" + q;
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/{id}/activate")
    public String activateShop(@PathVariable String id,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "5") int size,
                                @RequestParam(required = false) String q,
                                RedirectAttributes redirectAttributes) {
        Shop shop;
        try {
            shop = adminShopService.getShopById(id);
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/shops";
        }

        try {
            adminShopService.activateShop(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã kích hoạt shop \"" + shop.getShopName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashInfo", ex.getMessage());
        }
        String redirectUrl = "/admin/shops/" + id + "?page=" + page + "&size=" + size;
        if (q != null && !q.isBlank()) {
            redirectUrl += "&q=" + q;
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateShop(@PathVariable String id,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String q,
                                 RedirectAttributes redirectAttributes) {
        Shop shop;
        try {
            shop = adminShopService.getShopById(id);
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/shops";
        }

        try {
            adminShopService.deactivateShop(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã ngừng hoạt động shop \"" + shop.getShopName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashInfo", ex.getMessage());
        }
        String redirectUrl = "/admin/shops/" + id + "?page=" + page + "&size=" + size;
        if (q != null && !q.isBlank()) {
            redirectUrl += "&q=" + q;
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/{id}/reject")
    public String rejectShop(@PathVariable String id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             @RequestParam(required = false) String q,
                             RedirectAttributes redirectAttributes) {
        Shop shop;
        try {
            shop = adminShopService.getShopById(id);
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/shops";
        }

        try {
            adminShopService.rejectShop(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã từ chối shop \"" + shop.getShopName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        String redirectUrl = "/admin/shops?page=" + page + "&size=" + size;
        if (q != null && !q.isBlank()) {
            redirectUrl += "&q=" + q;
        }
        return "redirect:" + redirectUrl;
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
