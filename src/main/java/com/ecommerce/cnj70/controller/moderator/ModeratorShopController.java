package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.dto.moderation.RestoreShopReq;
import com.ecommerce.cnj70.dto.moderation.ShopModerationContext;
import com.ecommerce.cnj70.dto.moderation.ShopModerationDecision;
import com.ecommerce.cnj70.dto.moderation.ShopModerationQueueItem;
import com.ecommerce.cnj70.dto.moderation.SuspendShopReq;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ModeratorShopService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 6 — Moderator Shop Moderation Controller.
 *
 * <h3>Routes</h3>
 * <ul>
 *     <li>{@code GET  /moderator/shops}                       — Shop moderation queue (search + filter + pagination)</li>
 *     <li>{@code GET  /moderator/shops/{id}}                  — Shop moderation detail with vendor context</li>
 *     <li>{@code POST /moderator/shops/{id}/approve}          — Approve pending shop registration</li>
 *     <li>{@code POST /moderator/shops/{id}/suspend}          — Suspend an active shop (reason required)</li>
 *     <li>{@code POST /moderator/shops/{id}/restore}          — Restore a suspended/restricted shop (reason required)</li>
 *     <li>{@code POST /moderator/shops/{id}/restrict}         — Restrict an approved shop (reason required)</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <ul>
 *     <li>Route-level: SecurityConfig hasRole("MODERATOR") — Phase 1</li>
 *     <li>Object-level, account-status, state-level enforced in
 *         {@link ModeratorShopServiceImpl}.</li>
 *     <li>Admin/Vendor/Customer: 403 (route-level)</li>
 * </ul>
 *
 * <h3>INTEGRATION-READY</h3>
 * <p>This controller follows the same Moderator pattern as
 * ModeratorProductController and ModeratorReviewController.
 * Backend service is real and persists decisions. The Shop
 * moderation contract can be tightened in a follow-up phase without
 * UI breakage (templates only display state names + decision messages).</p>
 */
@Slf4j
@Controller
@RequestMapping("/moderator/shops")
@RequiredArgsConstructor
public class ModeratorShopController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ModeratorShopService moderatorShopService;

    // ======================== QUEUE ========================

    @GetMapping
    public String queue(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String status,
                        Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.ASC, "createdAt"));

        ShopStatus statusFilter = parseStatus(status);
        Page<ShopModerationQueueItem> result =
                moderatorShopService.listQueue(q, statusFilter, pageable);

        model.addAttribute("items", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", statusFilter == null ? "" : statusFilter.name());
        model.addAttribute("statuses", List.of(
                ShopStatus.PENDING,
                ShopStatus.APPROVED,
                ShopStatus.RESTRICTED,
                ShopStatus.SUSPENDED));
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));

        return "moderator/shop-queue";
    }

    // ======================== DETAIL ========================

    @GetMapping("/{id}")
    public String shopDetail(@PathVariable String id,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String status,
                             Model model) {
        ShopModerationContext context = moderatorShopService.getContext(id);

        model.addAttribute("ctx", context);
        model.addAttribute("shop", context); // alias for template compatibility
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", status == null ? "" : status);
        return "moderator/shop-detail";
    }

    // ======================== APPROVE ========================

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String status,
                          @AuthenticationPrincipal CustomUserDetails moderator,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            ShopModerationDecision decision = moderatorShopService.approve(id, moderator, clientIp(request));
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (RuntimeException ex) {
            log.error("Approve shop failed: id={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildQueueRedirect(page, size, q, status);
    }

    // ======================== SUSPEND ========================

    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable String id,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String status,
                          @ModelAttribute @Valid SuspendShopReq req,
                          @AuthenticationPrincipal CustomUserDetails moderator,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            String reason = req != null ? req.getReason() : null;
            String note = req != null ? req.getNote() : null;
            ShopModerationDecision decision =
                    moderatorShopService.suspend(id, reason, note, moderator, clientIp(request));
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (RuntimeException ex) {
            log.error("Suspend shop failed: id={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildQueueRedirect(page, size, q, status);
    }

    // ======================== RESTORE ========================

    @PostMapping("/{id}/restore")
    public String restore(@PathVariable String id,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String status,
                          @ModelAttribute @Valid RestoreShopReq req,
                          @AuthenticationPrincipal CustomUserDetails moderator,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            String reason = req != null ? req.getReason() : null;
            String note = req != null ? req.getNote() : null;
            ShopModerationDecision decision =
                    moderatorShopService.restore(id, reason, note, moderator, clientIp(request));
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (RuntimeException ex) {
            log.error("Restore shop failed: id={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildQueueRedirect(page, size, q, status);
    }

    // ======================== RESTRICT ========================

    @PostMapping("/{id}/restrict")
    public String restrict(@PathVariable String id,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @ModelAttribute @Valid SuspendShopReq req,
                           @AuthenticationPrincipal CustomUserDetails moderator,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            String reason = req != null ? req.getReason() : null;
            String note = req != null ? req.getNote() : null;
            ShopModerationDecision decision =
                    moderatorShopService.restrict(id, reason, note, moderator, clientIp(request));
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (RuntimeException ex) {
            log.error("Restrict shop failed: id={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildQueueRedirect(page, size, q, status);
    }

    // ======================== HELPERS ========================

    private String buildQueueRedirect(int page, int size, String q, String status) {
        StringBuilder url = new StringBuilder("/moderator/shops?")
                .append("page=").append(page)
                .append("&size=").append(size);
        if (q != null && !q.isBlank()) {
            url.append("&q=").append(q);
        }
        if (status != null && !status.isBlank()) {
            url.append("&status=").append(status);
        }
        return "redirect:" + url;
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
            return null;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
