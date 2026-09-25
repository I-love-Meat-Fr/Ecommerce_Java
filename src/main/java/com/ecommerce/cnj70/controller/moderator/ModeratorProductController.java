package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.dto.moderation.EscalateProductReq;
import com.ecommerce.cnj70.dto.moderation.ModerationDecision;
import com.ecommerce.cnj70.dto.moderation.ProductModerationContext;
import com.ecommerce.cnj70.dto.moderation.ProductModerationQueueItem;
import com.ecommerce.cnj70.dto.moderation.RejectProductReq;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ModeratorProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * Phase 2A — Moderator Product Moderation Controller.
 *
 * <p>Routes:</p>
 * <ul>
 *     <li>{@code GET  /moderator/queue} — Product moderation queue (search + filter + pagination)</li>
 *     <li>{@code GET  /moderator/products/{id}} — Product detail with Auto Moderation result + Shop/Vendor context</li>
 *     <li>{@code POST /moderator/products/{id}/approve} — Approve product</li>
 *     <li>{@code POST /moderator/products/{id}/reject} — Reject product (reason required)</li>
 *     <li>{@code POST /moderator/products/{id}/escalate} — Escalate to Admin (reason required)</li>
 * </ul>
 *
 * <p>Security:</p>
 * <ul>
 *     <li>Route-level: {@code /moderator/**} requires {@code ROLE_MODERATOR} (Phase 1)</li>
 *     <li>Object-level: enforced inside {@code ModeratorProductServiceImpl}</li>
 *     <li>Account-status: locked/unverified moderators rejected at service boundary</li>
 * </ul>
 *
 * <p>Phase 2A §34 / §35 / §36 / §37 — backend-enforced, NOT UI-only.</p>
 */
@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorProductController {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final ModeratorProductService moderatorProductService;

    // ======================== QUEUE ========================

    @GetMapping("/queue")
    public String queue(@RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String q,
                        @RequestParam(required = false) String status,
                        Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        ModerationStatus moderationFilter = parseStatus(status);
        Page<ProductModerationQueueItem> result =
                moderatorProductService.listQueue(q, moderationFilter, pageable);

        model.addAttribute("items", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("status", moderationFilter == null ? "" : moderationFilter.name());
        model.addAttribute("statuses", List.of(
                ModerationStatus.PENDING_AUTO,
                ModerationStatus.PENDING_MANUAL,
                ModerationStatus.AUTO_PASSED,
                ModerationStatus.AUTO_REJECTED));
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));

        return "moderator/product-queue";
    }

    // ======================== DETAIL ========================

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable String id,
                                Model model) {
        ProductModerationContext context = moderatorProductService.getContext(id);
        model.addAttribute("ctx", context);
        model.addAttribute("product", context); // alias for template compatibility
        return "moderator/product-detail";
    }

    // ======================== APPROVE ========================

    @PostMapping("/products/{id}/approve")
    public String approve(@PathVariable String id,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String status,
                          @AuthenticationPrincipal CustomUserDetails moderator,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            ModerationDecision decision = moderatorProductService.approve(id, moderator, clientIp(request));
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildQueueRedirect(page, size, q, status);
    }

    // ======================== REJECT ========================

    @PostMapping("/products/{id}/reject")
    public String reject(@PathVariable String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String status,
                         @ModelAttribute @Valid RejectProductReq req,
                         @AuthenticationPrincipal CustomUserDetails moderator,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            String reason = req != null ? req.getReason() : null;
            ModerationDecision decision =
                    moderatorProductService.reject(id, reason, moderator, clientIp(request));
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildQueueRedirect(page, size, q, status);
    }

    // ======================== ESCALATE ========================

    @PostMapping("/products/{id}/escalate")
    public String escalate(@PathVariable String id,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String status,
                           @ModelAttribute @Valid EscalateProductReq req,
                           @AuthenticationPrincipal CustomUserDetails moderator,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            String reason = req != null ? req.getReason() : null;
            ModerationDecision decision =
                    moderatorProductService.escalate(id, reason, moderator, clientIp(request));
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildQueueRedirect(page, size, q, status);
    }

    // ======================== HELPERS ========================

    private static String buildQueueRedirect(int page, int size, String q, String status) {
        StringBuilder url = new StringBuilder("/moderator/queue?")
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

    private static ModerationStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String norm = raw.trim().toUpperCase();
        if ("ALL".equals(norm)) return null;
        try {
            return ModerationStatus.valueOf(norm);
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
