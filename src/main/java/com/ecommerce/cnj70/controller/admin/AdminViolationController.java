package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/admin/violations")
@RequiredArgsConstructor
public class AdminViolationController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ViolationService violationService;
    private final ShopRepository shopRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String shopId,
                       @RequestParam(required = false) String severity,
                       @RequestParam(required = false) String type,
                       Model model) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        ViolationSeverity sev = parseSeverity(severity);
        ViolationType ty = parseType(type);

        Page<Violation> violations = violationService.listViolations(emptyToNull(shopId), sev, ty, pageable);

        model.addAttribute("violations", violations.getContent());
        model.addAttribute("page", violations.getNumber());
        model.addAttribute("size", violations.getSize());
        model.addAttribute("totalPages", violations.getTotalPages());
        model.addAttribute("totalItems", violations.getTotalElements());
        model.addAttribute("shopId", shopId);
        model.addAttribute("severity", severity);
        model.addAttribute("type", type);
        model.addAttribute("severities", ViolationSeverity.values());
        model.addAttribute("types", ViolationType.values());
        return "admin/violation-list";
    }

    @GetMapping("/create")
    public String createForm(@RequestParam(required = false) String shopId, Model model) {
        List<Shop> shops = new ArrayList<>();
        shopRepository.findAll().forEach(shops::add);
        model.addAttribute("shops", shops);
        model.addAttribute("severities", ViolationSeverity.values());
        model.addAttribute("types", ViolationType.values());
        model.addAttribute("selectedShopId", shopId);
        return "admin/violation-create";
    }

    @PostMapping("/create")
    public String create(@RequestParam String shopId,
                         @RequestParam String type,
                         @RequestParam String severity,
                         @RequestParam String reason,
                         @RequestParam(required = false) String adminNote,
                         @RequestParam(required = false) String productId,
                         @RequestParam(required = false) String productName,
                         @AuthenticationPrincipal CustomUserDetails admin,
                         RedirectAttributes redirectAttributes) {
        try {
            Violation violation = Violation.builder()
                    .shopId(shopId)
                    .productId(emptyToNull(productId))
                    .productName(emptyToNull(productName))
                    .type(ViolationType.valueOf(type))
                    .severity(ViolationSeverity.valueOf(severity))
                    .reason(reason)
                    .adminNote(emptyToNull(adminNote))
                    .createdBy(admin != null ? admin.getUsername() : "system")
                    .build();
            violationService.createViolation(violation);
            redirectAttributes.addFlashAttribute("flashSuccess", "Đã tạo violation thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashError", "Lỗi: " + e.getMessage());
            return "redirect:/admin/violations/create?shopId=" + shopId;
        }
        return "redirect:/admin/violations";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          @AuthenticationPrincipal CustomUserDetails admin,
                          RedirectAttributes redirectAttributes) {
        try {
            violationService.resolveViolation(id,
                    admin != null ? admin.getUsername() : "system",
                    note);
            redirectAttributes.addFlashAttribute("flashSuccess", "Đã xử lý violation");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashError", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/violations";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String severity,
                         @RequestParam(required = false) String resourceType,
                         Model model) {
        Violation v = null;
        try {
            v = violationService.getById(id);
        } catch (Exception ex) {
            log.warn("Admin violation detail lookup failed for id={}: {}", id, ex.getMessage());
        }
        if (v == null) {
            model.addAttribute("error", "Không tìm thấy violation với ID: " + id);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("status", status);
            model.addAttribute("severity", severity);
            model.addAttribute("resourceType", resourceType);
            return "admin/violation-detail";
        }
        Map<String, Object> view = new HashMap<>();
        view.put("id", v.getId());
        view.put("severity", v.getSeverity() != null ? v.getSeverity().name() : null);
        view.put("status", v.isActive() ? "OPEN" : "RESOLVED");
        view.put("resourceType", "SHOP");
        view.put("resourceId", v.getShopId());
        view.put("vendorId", null);
        view.put("vendorName", null);
        view.put("shopId", v.getShopId());
        view.put("shopName", null);
        if (v.getShopId() != null) {
            shopRepository.findById(v.getShopId()).ifPresent(shop -> {
                view.put("shopName", shop.getShopName());
                view.put("vendorId", shop.getOwnerId());
                view.put("vendorName", shop.getOwnerId());
            });
        }
        view.put("policyCode", v.getType() != null ? v.getType().name() : null);
        view.put("violationCode", v.getType() != null ? v.getType().name() : null);
        view.put("action", v.getSeverity() != null ? v.getSeverity().name() : null);
        view.put("source", v.getCreatedBy());
        view.put("description", v.getReason());
        view.put("resolutionNote", v.getResolutionNote());
        view.put("evidence", new ArrayList<>());
        view.put("createdAt", v.getCreatedAt());
        view.put("resolvedAt", v.getResolvedAt());
        view.put("moderatorId", null);
        view.put("moderatorEmail", null);
        view.put("escalationId", null);

        model.addAttribute("v", view);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("status", status);
        model.addAttribute("severity", severity);
        model.addAttribute("resourceType", resourceType);
        return "admin/violation-detail";
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static ViolationSeverity parseSeverity(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return ViolationSeverity.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static ViolationType parseType(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return ViolationType.valueOf(s);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
