package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Phase 1/2A/2B/2C — Moderator Dashboard Controller.
 *
 * <p>Routes:</p>
 * <ul>
 *     <li>{@code GET /moderator/dashboard} — Dashboard overview (Phase 1)</li>
 *     <li>{@code GET /moderator/violations} — Violations placeholder (Phase 3)</li>
 *     <li>{@code GET /moderator/history} — Moderation History (Phase 2A — wired to ModerationHistoryRepository; Phase 2C extends to show ReportCase actions)</li>
 * </ul>
 *
 * <p>Routes owned by sibling controllers:</p>
 * <ul>
 *     <li>{@code /moderator/queue}       — Product Queue (Phase 2A: ModeratorProductController)</li>
 *     <li>{@code /moderator/products/**} — Product Detail &amp; Actions (Phase 2A)</li>
 *     <li>{@code /moderator/reviews/**}  — Review Queue / Detail / Actions (Phase 2B: ModeratorReviewController)</li>
 *     <li>{@code /moderator/cases/**}    — ReportCase Queue / Detail / Actions (Phase 2C: ModeratorReportCaseController)</li>
 * </ul>
 *
 * <p>Security: {@code /moderator/**} is protected by
 * {@code SecurityConfig.hasRole("MODERATOR")} (Phase 1).</p>
 */
/**
 * Phase 1/2A/2B/2C — Moderator Dashboard Controller.
 *
 * <p>
 * Routes:
 * </p>
 * <ul>
 * <li>{@code GET /moderator/violations} — Violations placeholder (Phase 3)</li>
 * <li>{@code GET /moderator/history} — Moderation History (Phase 2A)</li>
 * </ul>
 *
 * <p>
 * Note: {@code /moderator/dashboard} is now in ModeratorController
 * (original dashboard with stats from stash feature/admin).
 * </p>
 */
@Controller
@RequiredArgsConstructor
public class ModeratorDashboardController {

    private final ModerationHistoryRepository historyRepository;
    private final ViolationService violationService;
    private final ShopRepository shopRepository;

    // Dashboard moved to ModeratorController — DO NOT add /moderator/dashboard here

    @GetMapping("/moderator/violations")
    public String violations(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            Model model) {
        int safeSize = (size <= 0) ? 10 : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        ViolationSeverity sev = null;
        if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity)) {
            try {
                sev = ViolationSeverity.valueOf(severity);
            } catch (Exception ignored) {
            }
        }
        ViolationType vt = null;
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            try {
                vt = ViolationType.valueOf(type);
            } catch (Exception ignored) {
            }
        }
        String shopIdFilter = (shopId != null && !shopId.isBlank()) ? shopId.trim() : null;

        // Phase 2 BUG-V1 Fix: truyền shopId xuống service để filter theo shop.
        Page<Violation> result = violationService.listViolations(shopIdFilter, sev, vt, pageable);

        model.addAttribute("violations", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("shopId", shopId != null ? shopId : "");
        model.addAttribute("severity", severity != null ? severity : "");
        model.addAttribute("type", type != null ? type : "");
        model.addAttribute("severities", Arrays.asList(ViolationSeverity.values()));
        model.addAttribute("types", Arrays.asList(ViolationType.values()));

        return "moderator/violations";
    }

    @GetMapping("/moderator/violations/{id}")
    public String violationDetail(@PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String shopId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String type,
            Model model) {
        Violation v = null;
        try {
            v = violationService.getById(id);
        } catch (Exception ex) {
            model.addAttribute("error", "Không tìm thấy violation với ID: " + id);
        }
        if (v == null) {
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("shopId", shopId);
            model.addAttribute("severity", severity);
            model.addAttribute("type", type);
            return "moderator/violation-detail";
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
        view.put("action", v.getSeverity() != null ? v.getSeverity().name() : null);
        view.put("source", v.getCreatedBy());
        view.put("description", v.getReason());
        view.put("resolutionNote", v.getResolutionNote());
        view.put("evidence", new ArrayList<>());
        view.put("createdAt", v.getCreatedAt());
        view.put("resolvedAt", v.getResolvedAt());
        model.addAttribute("v", view);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("shopId", shopId);
        model.addAttribute("severity", severity);
        model.addAttribute("type", type);
        return "moderator/violation-detail";
    }

    @GetMapping("/moderator/history")
    public String history(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        int safeSize = (size <= 0) ? 10 : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<ModerationHistory> result = historyRepository.findAllByOrderByCreatedAtDesc(pageable);

        model.addAttribute("history", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());

        return "moderator/history";
    }
}
