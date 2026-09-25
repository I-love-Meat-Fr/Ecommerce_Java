package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.dto.reportcase.ReportCaseActionReq;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseDecision;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseDetail;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseQueueItem;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportReason;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ModeratorReportCaseService;
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
 * Phase 2C — Moderator ReportCase Controller.
 *
 * <h3>Routes</h3>
 * <ul>
 *     <li>{@code GET  /moderator/cases}                 — ReportCase Queue</li>
 *     <li>{@code GET  /moderator/cases/{id}}            — ReportCase Detail</li>
 *     <li>{@code POST /moderator/cases/{id}/claim}      — Claim OPEN → IN_REVIEW</li>
 *     <li>{@code POST /moderator/cases/{id}/approve}    — Approve (reason optional as note)</li>
 *     <li>{@code POST /moderator/cases/{id}/reject}     — Reject (reason MANDATORY)</li>
 *     <li>{@code POST /moderator/cases/{id}/escalate}   — Escalate (reason + severity MANDATORY)</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <p>Route-level: SecurityConfig.hasRole("MODERATOR") (Phase 1).
 * Object-level, account-status, state-level, and reason-validation
 * are enforced in {@link ModeratorReportCaseService}.</p>
 */
@Slf4j
@Controller
@RequestMapping("/moderator/cases")
@RequiredArgsConstructor
public class ModeratorReportCaseController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ModeratorReportCaseService service;

    // ======================== QUEUE ========================

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       @RequestParam(required = false) ReportCaseStatus status,
                       @RequestParam(required = false) ReportReason reason,
                       @RequestParam(required = false) ReportCaseResourceType resourceType,
                       Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ReportCaseQueueItem> result = service.listQueue(status, reason, resourceType, pageable);

        model.addAttribute("items", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("statusFilter", status == null ? "" : status.name());
        model.addAttribute("reasonFilter", reason == null ? "" : reason.name());
        model.addAttribute("resourceTypeFilter", resourceType == null ? "" : resourceType.name());
        model.addAttribute("statusChoices", ReportCaseStatus.values());
        model.addAttribute("reasonChoices", ReportReason.values());
        model.addAttribute("resourceTypeChoices", ReportCaseResourceType.values());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        model.addAttribute("title", "Report Cases - CNJ70 Moderator");
        model.addAttribute("activeCaseQueue", true);

        return "moderator/case-queue";
    }

    // ======================== DETAIL ========================

    @GetMapping("/{id}")
    public String detail(@PathVariable String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String resourceType,
                         Model model) {
        ReportCaseDetail detail = service.getDetail(id);

        model.addAttribute("ctx", detail);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("reason", reason == null ? "" : reason);
        model.addAttribute("resourceType", resourceType == null ? "" : resourceType);
        model.addAttribute("escalationSeverityChoices",
                com.ecommerce.cnj70.enums.EscalationSeverity.values());
        model.addAttribute("title", "Report Case Detail - CNJ70 Moderator");
        model.addAttribute("activeCaseQueue", true);

        return "moderator/case-detail";
    }

    // ======================== CLAIM ========================

    @PostMapping("/{id}/claim")
    public String claim(@PathVariable String id,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String reason,
                        @RequestParam(required = false) String resourceType,
                        @AuthenticationPrincipal CustomUserDetails moderator,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        try {
            ReportCaseDecision decision = service.claim(id, moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Claim case failed: caseId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể nhận case: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, status, reason, resourceType);
    }

    // ======================== APPROVE ========================

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable String id,
                          @Valid @org.springframework.web.bind.annotation.ModelAttribute ReportCaseActionReq body,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "10") int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String reason,
                          @RequestParam(required = false) String resourceType,
                          @AuthenticationPrincipal CustomUserDetails moderator,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            ReportCaseDecision decision = service.approve(
                    id, body.getReason(), body.getNote(), moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.BadRequestException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Approve case failed: caseId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể duyệt case: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, status, reason, resourceType);
    }

    // ======================== REJECT ========================

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable String id,
                         @Valid @org.springframework.web.bind.annotation.ModelAttribute ReportCaseActionReq body,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "10") int size,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String reason,
                         @RequestParam(required = false) String resourceType,
                         @AuthenticationPrincipal CustomUserDetails moderator,
                         HttpServletRequest request,
                         RedirectAttributes redirectAttributes) {
        try {
            ReportCaseDecision decision = service.reject(
                    id, body.getReason(), body.getNote(), moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.BadRequestException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Reject case failed: caseId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể từ chối case: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, status, reason, resourceType);
    }

    // ======================== ESCALATE ========================

    @PostMapping("/{id}/escalate")
    public String escalate(@PathVariable String id,
                           @Valid @org.springframework.web.bind.annotation.ModelAttribute ReportCaseActionReq body,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String reason,
                           @RequestParam(required = false) String resourceType,
                           @AuthenticationPrincipal CustomUserDetails moderator,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            ReportCaseDecision decision = service.escalate(
                    id, body.getReason(), body.getSeverity(), moderator, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess", decision.getMessage());
        } catch (com.ecommerce.cnj70.exception.BadRequestException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.ConflictException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (com.ecommerce.cnj70.exception.UnauthorizedException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("Escalate case failed: caseId={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể leo thang case: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, status, reason, resourceType);
    }

    // ======================== HELPERS ========================

    private String detailRedirect(String id, int page, int size, String status,
                                   String reason, String resourceType) {
        StringBuilder url = new StringBuilder("/moderator/cases/")
                .append(id)
                .append("?page=").append(page)
                .append("&size=").append(size);
        if (status != null && !status.isBlank()) url.append("&status=").append(status);
        if (reason != null && !reason.isBlank()) url.append("&reason=").append(reason);
        if (resourceType != null && !resourceType.isBlank()) url.append("&resourceType=").append(resourceType);
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

