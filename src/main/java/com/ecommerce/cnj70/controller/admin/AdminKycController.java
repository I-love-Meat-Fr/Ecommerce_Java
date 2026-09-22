package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminKycService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/kyc")
@RequiredArgsConstructor
public class AdminKycController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AdminKycService adminKycService;

    /**
     * Danh sách hồ sơ KYC chờ admin duyệt (mặc định PENDING_ADMIN)
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "PENDING_ADMIN") String status,
                       Model model) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        KycStatus statusEnum = parseStatus(status);
        Page<KycProfile> profiles = adminKycService.listByStatus(statusEnum, q, pageable);

        // Build thông tin user cho từng profile
        java.util.List<Map<String, Object>> rows = profiles.getContent().stream()
                .map(p -> {
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    row.put("profile", p);
                    row.put("user", adminKycService.getUserForProfile(p));
                    return row;
                })
                .toList();

        model.addAttribute("rows", rows);
        model.addAttribute("profiles", profiles);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("pageSize", safeSize);
        model.addAttribute("totalPages", profiles.getTotalPages());
        model.addAttribute("totalItems", profiles.getTotalElements());
        model.addAttribute("searchQuery", q);
        model.addAttribute("currentStatus", statusEnum != null ? statusEnum.name() : "ALL");
        model.addAttribute("stats", adminKycService.getStats());

        return "admin/kyc-list";
    }

    /**
     * Chi tiết 1 hồ sơ KYC
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        Map<String, Object> data = adminKycService.getDetail(id);
        model.addAttribute("profile", data.get("profile"));
        model.addAttribute("user", data.get("user"));
        return "admin/kyc-detail";
    }

    /**
     * Admin duyệt
     */
    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                         @RequestParam(required = false) String note,
                         @AuthenticationPrincipal CustomUserDetails admin,
                         RedirectAttributes redirectAttributes) {
        try {
            adminKycService.approve(id, admin, note);
            redirectAttributes.addFlashAttribute("success", "Đã duyệt hồ sơ KYC. Vendor có thể tạo cửa hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/kyc/" + id;
    }

    /**
     * Admin từ chối
     */
    @PostMapping("/{id}/reject")
    public String reject(@PathVariable String id,
                         @RequestParam(required = false) String note,
                         @AuthenticationPrincipal CustomUserDetails admin,
                         RedirectAttributes redirectAttributes) {
        try {
            if (note == null || note.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng nhập lý do từ chối");
                return "redirect:/admin/kyc/" + id;
            }
            adminKycService.reject(id, admin, note);
            redirectAttributes.addFlashAttribute("success", "Đã từ chối hồ sơ KYC.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/kyc";
    }

    /**
     * Đình chỉ hồ sơ đã duyệt
     */
    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable String id,
                          @RequestParam(required = false) String note,
                          @AuthenticationPrincipal CustomUserDetails admin,
                          RedirectAttributes redirectAttributes) {
        try {
            adminKycService.suspend(id, admin, note);
            redirectAttributes.addFlashAttribute("success", "Đã đình chỉ hồ sơ KYC.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/kyc";
    }

    private KycStatus parseStatus(String s) {
        if (s == null || s.isBlank() || "ALL".equalsIgnoreCase(s)) return null;
        try {
            return KycStatus.valueOf(s);
        } catch (Exception e) {
            return KycStatus.PENDING_ADMIN;
        }
    }
}
