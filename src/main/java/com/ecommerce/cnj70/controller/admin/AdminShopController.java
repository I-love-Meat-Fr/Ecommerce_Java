package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AdminShopService;
import com.ecommerce.cnj70.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    /**
     * Phase 4 — dùng để raw-fetch User document với {@code _id} String
     * (cùng pattern với AdminUserServiceImpl#getUserById ở Phase 3).
     */
    private final MongoTemplate mongoTemplate;

    private String currentActorId(UserDetails user) {
        return user != null ? user.getUsername() : null;
    }

    @GetMapping
    public String shopList(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "5") int size,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "createdAt"));

        // Phase 10: parse status (null/blank/ALL = no filter)
        ShopStatus statusFilter = parseStatus(status);

        Page<Shop> result = adminShopService.listShops(pageable, q, statusFilter);

        model.addAttribute("shops", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", (q == null) ? "" : q);
        model.addAttribute("status", statusFilter == null ? "" : statusFilter.name());
        model.addAttribute("statuses", ShopStatus.values());
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
                            @RequestParam(required = false) String status,
                            Model model) {
        Shop shop = adminShopService.getShopById(id);

        User owner = null;
        if (shop.getOwnerId() != null && !shop.getOwnerId().isBlank()) {
            // Phase 4 — raw Document fetch (xem lý do trong field comment bên trên).
            org.bson.Document rawOwner = mongoTemplate.getCollection("users")
                    .find(new org.bson.Document("_id", shop.getOwnerId()))
                    .first();
            if (rawOwner != null) {
                if (!rawOwner.containsKey("_class")) {
                    rawOwner.put("_class", User.class.getName());
                }
                rawOwner.put("_id", shop.getOwnerId());
                owner = mongoTemplate.getConverter().read(User.class, rawOwner);
                if (owner.getId() == null) {
                    owner.setId(shop.getOwnerId());
                }
            }
        }

        model.addAttribute("shop", shop);
        model.addAttribute("owner", owner);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", (q == null) ? "" : q);
        model.addAttribute("status", (status == null) ? "" : status);
        return "admin/shop-detail";
    }

    @PostMapping("/{id}/approve")
    public String approveShop(@PathVariable String id,
                              @AuthenticationPrincipal UserDetails userDetails,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "5") int size,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) String status,
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
                // TASK #24: truyền actor vào audit log
                adminShopService.approveShop(id, currentActorId(userDetails), currentActorId(userDetails));
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
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/{id}/activate")
    public String activateShop(@PathVariable String id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "5") int size,
                                @RequestParam(required = false) String q,
                                @RequestParam(required = false) String status,
                                RedirectAttributes redirectAttributes) {
        Shop shop;
        try {
            shop = adminShopService.getShopById(id);
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/shops";
        }

        try {
            adminShopService.activateShop(id, currentActorId(userDetails), currentActorId(userDetails));
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã kích hoạt shop \"" + shop.getShopName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashInfo", ex.getMessage());
        }
        String redirectUrl = "/admin/shops/" + id + "?page=" + page + "&size=" + size;
        if (q != null && !q.isBlank()) {
            redirectUrl += "&q=" + q;
        }
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/{id}/deactivate")
    public String deactivateShop(@PathVariable String id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "5") int size,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String reason,
                                 RedirectAttributes redirectAttributes) {
        Shop shop;
        try {
            shop = adminShopService.getShopById(id);
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/shops";
        }

        try {
            // Signature mới (merge feature/admin + feature/vendors-module):
            //   deactivateShop(id, reason, adminUsername) - vừa lưu reason/actionBy/actionAt
            //   lên Shop, vừa ghi AuditLog SHOP_SUSPENDED với adminUsername làm actor.
            // currentActorId() trả về user.getUsername() (xem helper ở đầu class).
            String actorId = currentActorId(userDetails);
            adminShopService.deactivateShop(id, reason, actorId);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã ngừng hoạt động shop \"" + shop.getShopName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashInfo", ex.getMessage());
        }
        String redirectUrl = "/admin/shops/" + id + "?page=" + page + "&size=" + size;
        if (q != null && !q.isBlank()) {
            redirectUrl += "&q=" + q;
        }
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
        }
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/{id}/reject")
    public String rejectShop(@PathVariable String id,
                             @AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "5") int size,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String status,
                             @RequestParam(required = false) String reason,
                             RedirectAttributes redirectAttributes) {
        Shop shop;
        try {
            shop = adminShopService.getShopById(id);
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
            return "redirect:/admin/shops";
        }

        try {
            // Signature mới: rejectShop(id, reason, adminUsername).
            // Gộp cả reason + actorId/actorUsername.
            // currentActorId() trả về user.getUsername() nên truyền trực tiếp vào adminUsername.
            String actorId = currentActorId(userDetails);
            adminShopService.rejectShop(id, reason, actorId);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã từ chối shop \"" + shop.getShopName() + "\"");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        String redirectUrl = "/admin/shops?page=" + page + "&size=" + size;
        if (q != null && !q.isBlank()) {
            redirectUrl += "&q=" + q;
        }
        if (status != null && !status.isBlank()) {
            redirectUrl += "&status=" + status;
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

    private static ShopStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String norm = raw.trim().toUpperCase();
        if ("ALL".equals(norm)) return null;
        try {
            return ShopStatus.valueOf(norm);
        } catch (IllegalArgumentException ex) {
            // Unknown status value -> no filter (safe fallback)
            return null;
        }
    }
}
