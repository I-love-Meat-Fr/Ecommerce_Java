package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.service.ViolationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
 * <p>Routes:</p>
 * <ul>
 *     <li>{@code GET /moderator/violations} — Violations placeholder (Phase 3)</li>
 *     <li>{@code GET /moderator/history} — Moderation History (Phase 2A)</li>
 * </ul>
 *
 * <p>Note: {@code /moderator/dashboard} is now in ModeratorController
 * (original dashboard with stats from stash feature/admin).</p>
 */
@Controller
@RequiredArgsConstructor
public class ModeratorDashboardController {

    private final ModerationHistoryRepository historyRepository;
    private final ViolationService violationService;

    // Dashboard moved to ModeratorController — DO NOT add /moderator/dashboard here

    @GetMapping("/moderator/violations")
    public String violations(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String severity,
                             @RequestParam(required = false) String type,
                             Model model) {
        int safeSize = (size <= 0) ? 20 : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        ViolationSeverity sev = null;
        if (severity != null && !severity.isBlank() && !"ALL".equalsIgnoreCase(severity)) {
            try { sev = ViolationSeverity.valueOf(severity); } catch (Exception ignored) {}
        }
        ViolationType vt = null;
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type)) {
            try { vt = ViolationType.valueOf(type); } catch (Exception ignored) {}
        }

        Page<Violation> result = violationService.listViolations(null, sev, vt, pageable);

        model.addAttribute("violations", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("severity", severity != null ? severity : "");
        model.addAttribute("type", type != null ? type : "");

        return "moderator/violations";
    }

    @GetMapping("/moderator/history")
    public String history(@RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          Model model) {
        int safeSize = (size <= 0) ? 20 : Math.min(size, 50);
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
