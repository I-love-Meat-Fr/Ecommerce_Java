package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.dto.review.ReviewModerationContext;
import com.ecommerce.cnj70.dto.review.ReviewModerationDecision;
import com.ecommerce.cnj70.dto.review.ReviewModerationQueueItem;
import com.ecommerce.cnj70.dto.review.ReviewModerationReq;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ModeratorReviewService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Phase 2B — Moderator Review Moderation Controller.
 *
 * <p>Server-rendered HTML pages for the Moderator Review workflow.</p>
 *
 * <h3>Routes</h3>
 * <ul>
 *     <li>{@code GET  /moderator/reviews}                  — Review Queue</li>
 *     <li>{@code GET  /moderator/reviews/{id}}             — Review Detail</li>
 *     <li>{@code POST /moderator/reviews/{id}/approve}     — Approve action</li>
 *     <li>{@code POST /moderator/reviews/{id}/reject}      — Reject action (reason required)</li>
 *     <li>{@code POST /moderator/reviews/{id}/hide}        — Hide action (reason required)</li>
 *     <li>{@code POST /moderator/reviews/{id}/unhide}      — Unhide action</li>
 *     <li>{@code POST /moderator/reviews/{id}/escalate}    — Escalate action (reason required)</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <p>{@code /moderator/**} is protected by
 * {@code SecurityConfig.hasRole("MODERATOR")}. Each action handler
 * delegates to the service which performs additional
 * object-level security (Phase 2B §32) + locked-account guard
 * (Phase 2B §46).</p>
 */
@Slf4j
@Controller
@RequestMapping("/moderator/reviews")
@RequiredArgsConstructor
public class ModeratorReviewController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ModeratorReviewService moderatorReviewService;

    // ======================== QUEUE ========================

    @GetMapping
    public String listReviews(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "10") int size,
                              @RequestParam(required = false) String q,
                              @RequestParam(required = false) ModerationStatus moderationStatus,
                              @RequestParam(required = false) Integer rating,
                              Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReviewModerationQueueItem> result = moderatorReviewService.listQueue(q, moderationStatus, rating, pageable);

        model.addAttribute("items", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("rating", rating == null ? "" : rating.toString());
        model.addAttribute("moderationStatus", moderationStatus == null ? "" : moderationStatus.name());
        model.addAttribute("statusChoices", ModerationStatus.values());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        model.addAttribute("title", "Review Queue - CNJ70 Moderator");
        model.addAttribute("activeReviewQueue", true);

        return "moderator/review-queue";
    }

    // ======================== DETAIL ========================

    @GetMapping("/{id}")
    public String reviewDetail(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) String moderationStatus,
                               @RequestParam(required = false) String rating,
                               Model model) {
        ReviewModerationContext context = moderatorReviewService.getContext(id);

        model.addAttribute("ctx", context);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("moderationStatus", moderationStatus == null ? "" : moderationStatus);
        model.addAttribute("rating", rating == null ? "" : rating);
        model.addAttribute("title", "Review Detail - CNJ70 Moderator");
        model.addAttribute("activeReviewQueue", true);

        return "moderator/review-detail";
    }

    // ======================== APPROVE ========================

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String q,
                          @RequestParam(required = false) String moderationStatus,
                          @RequestParam(required = false) String rating,
                          @AuthenticationPrincipal CustomUserDetails moderator,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            ReviewModerationDecision decision = moderatorReviewService.approve(
                    id, moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Approve review failed: reviewId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể duyệt đánh giá: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, q, moderationStatus, rating);
    }

    // ======================== REJECT ========================

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable String id,
                         @Valid @org.springframework.web.bind.annotation.ModelAttribute ReviewModerationReq body,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String moderationStatus,
                         @RequestParam(required = false) String rating,
                         @AuthenticationPrincipal CustomUserDetails moderator,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            ReviewModerationDecision decision = moderatorReviewService.reject(
                    id, body.getReason(), moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.BadRequestException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Reject review failed: reviewId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể từ chối đánh giá: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, q, moderationStatus, rating);
    }

    // ======================== HIDE ========================

    @PostMapping("/{id}/hide")
    public String hide(@PathVariable String id,
                       @Valid @org.springframework.web.bind.annotation.ModelAttribute ReviewModerationReq body,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) String moderationStatus,
                       @RequestParam(required = false) String rating,
                       @AuthenticationPrincipal CustomUserDetails moderator,
                       HttpServletRequest request,
                       RedirectAttributes redirectAttributes) {
        try {
            ReviewModerationDecision decision = moderatorReviewService.hide(
                    id, body.getReason(), moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.BadRequestException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Hide review failed: reviewId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể ẩn đánh giá: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, q, moderationStatus, rating);
    }

    // ======================== UNHIDE ========================

    @PostMapping("/{id}/unhide")
    public String unhide(@PathVariable String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String moderationStatus,
                         @RequestParam(required = false) String rating,
                         @AuthenticationPrincipal CustomUserDetails moderator,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            ReviewModerationDecision decision = moderatorReviewService.unhide(
                    id, moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Unhide review failed: reviewId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể hiện đánh giá: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, q, moderationStatus, rating);
    }

    // ======================== ESCALATE ========================

    @PostMapping("/{id}/escalate")
    public String escalate(@PathVariable String id,
                           @Valid @org.springframework.web.bind.annotation.ModelAttribute ReviewModerationReq body,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String moderationStatus,
                           @RequestParam(required = false) String rating,
                           @AuthenticationPrincipal CustomUserDetails moderator,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            ReviewModerationDecision decision = moderatorReviewService.escalate(
                    id, body.getReason(), moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.BadRequestException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Escalate review failed: reviewId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể leo thang đánh giá: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, q, moderationStatus, rating);
    }

    // ======================== HELPERS ========================

    private String detailRedirect(String id, int page, int size, String q,
                                   String moderationStatus, String rating) {
        StringBuilder url = new StringBuilder("/moderator/reviews/")
                .append(id)
                .append("?page=").append(page)
                .append("&size=").append(size);
        if (q != null && !q.isBlank()) url.append("&q=").append(q);
        if (moderationStatus != null && !moderationStatus.isBlank()) url.append("&moderationStatus=").append(moderationStatus);
        if (rating != null && !rating.isBlank()) url.append("&rating=").append(rating);
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
}

