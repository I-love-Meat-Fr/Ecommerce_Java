package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.VendorService;
import com.ecommerce.cnj70.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/vendor/violations")
@RequiredArgsConstructor
public class VendorViolationController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final VendorService vendorService;
    private final ViolationService violationService;

    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model) {
        String shopId = vendorService.getShopIdFromUser(user);
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Violation> violations = violationService.getViolationsByShopId(shopId, pageable);

        long activeCount = violationService.countActiveViolations(shopId);
        long criticalCount = violationService.countCriticalActiveViolations(shopId);

        model.addAttribute("violations", violations.getContent());
        model.addAttribute("page", violations.getNumber());
        model.addAttribute("size", violations.getSize());
        model.addAttribute("totalPages", violations.getTotalPages());
        model.addAttribute("totalItems", violations.getTotalElements());
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("criticalCount", criticalCount);
        return "vendor/violation-list";
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal CustomUserDetails user,
                         @PathVariable String id,
                         Model model) {
        String shopId = vendorService.getShopIdFromUser(user);
        Violation violation = violationService.getById(id);
        if (!shopId.equals(violation.getShopId())) {
            model.addAttribute("error", "Bạn không có quyền xem violation này");
            return "redirect:/vendor/violations";
        }
        model.addAttribute("violation", violation);
        return "vendor/violation-detail";
    }
}
