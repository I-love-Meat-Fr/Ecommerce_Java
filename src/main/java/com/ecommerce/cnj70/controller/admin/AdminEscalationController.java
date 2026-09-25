package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ViolationAction;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReportCaseRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminEscalationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
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
 * Phase 3A — Admin Escalation Controller.
 *
 * <h3>Routes</h3>
 * <ul>
 *     <li>{@code GET  /admin/escalations}                  — Admin Escalation Queue</li>
 *     <li>{@code GET  /admin/escalations/{id}}             — Admin Escalation Detail</li>
 *     <li>{@code POST /admin/escalations/{id}/claim}       — Claim PENDING → IN_REVIEW</li>
 *     <li>{@code POST /admin/escalations/{id}/enforce}     — Apply ViolationAction (Suspend/Restrict/Ban/Warn)</li>
 *     <li>{@code POST /admin/escalations/{id}/dismiss}     — Dismiss with mandatory reason</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <ul>
 *     <li>Route-level: SecurityConfig.hasRole("ADMIN") — Phase 1</li>
 *     <li>Object-level, account-status, state-level enforced in
 *         {@link AdminEscalationService}.</li>
 *     <li>Moderator/Vendor/Customer: 403 (route-level)</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/admin/escalations")
@RequiredArgsConstructor
public class AdminEscalationController {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminEscalationService service;
    private final ReportCaseRepository reportCaseRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;

    // ======================== QUEUE ========================

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) Escalation.Status status,
                       @RequestParam(required = false) EscalationSeverity severity,
                       @RequestParam(required = false) ReportCaseResourceType resourceType,
                       Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Escalation> result = service.listEscalations(status, severity, resourceType, pageable);

        model.addAttribute("escalations", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("statusFilter", status == null ? "" : status.name());
        model.addAttribute("severityFilter", severity == null ? "" : severity.name());
        model.addAttribute("resourceTypeFilter", resourceType == null ? "" : resourceType.name());
        model.addAttribute("statusChoices", Escalation.Status.values());
        model.addAttribute("severityChoices", EscalationSeverity.values());
        model.addAttribute("resourceTypeChoices", ReportCaseResourceType.values());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        model.addAttribute("title", "Escalations - CNJ70 Admin");
        model.addAttribute("activeEscalations", true);

        return "admin/escalation-queue";
    }

    // ======================== DETAIL ========================

    @GetMapping("/{id}")
    public String detail(@PathVariable String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String severity,
                         @RequestParam(required = false) String resourceType,
                         Model model) {
        Escalation esc = service.getDetail(id);

        // Pull live context for resource, vendor, shop, source case
        String resourceName = null;
        String liveResourceName = null;
        if (esc.getResourceId() != null) {
            if (esc.getResourceType() == ReportCaseResourceType.PRODUCT) {
                Product p = productRepository.findById(esc.getResourceId()).orElse(null);
                if (p != null) {
                    resourceName = p.getName();
                    liveResourceName = p.getName();
                }
            } else if (esc.getResourceType() == ReportCaseResourceType.REVIEW) {
                Review r = reviewRepository.findById(esc.getResourceId()).orElse(null);
                if (r != null) {
                    liveResourceName = "Review by " + (r.getUserName() != null ? r.getUserName() : r.getUserId());
                    resourceName = liveResourceName;
                }
            }
        }
        if (resourceName == null) resourceName = esc.getResourceId();

        String vendorName = null, vendorEmail = null, vendorStatus = null;
        if (esc.getVendorId() != null) {
            User vendor = userRepository.findById(esc.getVendorId()).orElse(null);
            if (vendor != null) {
                vendorName = vendor.getFullName();
                vendorEmail = vendor.getEmail();
                vendorStatus = vendor.getStatus() != null ? vendor.getStatus().name() : null;
            }
        }

        String shopName = null, shopStatus = null;
        if (esc.getShopId() != null) {
            Shop shop = shopRepository.findById(esc.getShopId()).orElse(null);
            if (shop != null) {
                shopName = shop.getShopName();
                shopStatus = shop.getStatus() != null ? shop.getStatus().name() : null;
            }
        }

        // Source ReportCase context (Moderator decision + reason)
        ReportCase sourceCase = null;
        if (esc.getReportCaseId() != null) {
            sourceCase = reportCaseRepository.findById(esc.getReportCaseId()).orElse(null);
        }

        // Vendor history snapshot (Phase 3A §20)
        long vendorTotalEscalations = 0L;
        long vendorResolvedEscalations = 0L;
        if (esc.getVendorId() != null) {
            // Approximation via ReportCaseRepository counts (the source of escalations).
            List<ReportCase> allCasesForVendor = reportCaseRepository
                    .findByVendorIdOrderByCreatedAtDesc(esc.getVendorId());
            vendorTotalEscalations = allCasesForVendor.size();
            vendorResolvedEscalations = allCasesForVendor.stream()
                    .filter(c -> c.getStatus() != null
                            && (c.getStatus().name().equals("APPROVED")
                            || c.getStatus().name().equals("REJECTED")
                            || c.getStatus().name().equals("ESCALATED")))
                    .count();
        }

        model.addAttribute("esc", esc);
        model.addAttribute("resourceName", resourceName);
        model.addAttribute("liveResourceName", liveResourceName);
        model.addAttribute("vendorName", vendorName);
        model.addAttribute("vendorEmail", vendorEmail);
        model.addAttribute("vendorStatus", vendorStatus);
        model.addAttribute("shopName", shopName);
        model.addAttribute("shopStatus", shopStatus);
        model.addAttribute("sourceCase", sourceCase);
        model.addAttribute("vendorTotalEscalations", vendorTotalEscalations);
        model.addAttribute("vendorResolvedEscalations", vendorResolvedEscalations);
        model.addAttribute("violationActions",
                new ViolationAction[] {
                        ViolationAction.WARNING,
                        ViolationAction.PRODUCT_HIDDEN,
                        ViolationAction.PRODUCT_REJECTED,
                        ViolationAction.SHOP_RESTRICTED,
                        ViolationAction.SHOP_SUSPENDED,
                        ViolationAction.VENDOR_BANNED
                });
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("severity", severity == null ? "" : severity);
        model.addAttribute("resourceType", resourceType == null ? "" : resourceType);
        model.addAttribute("title", "Escalation Detail - CNJ70 Admin");
        model.addAttribute("activeEscalations", true);

        return "admin/escalation-detail";
    }

    // ======================== CLAIM ========================

    @PostMapping("/{id}/claim")
    public String claim(@PathVariable String id,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String severity,
                        @RequestParam(required = false) String resourceType,
                        @AuthenticationPrincipal CustomUserDetails admin,
                        HttpServletRequest request,
                        RedirectAttributes redirectAttributes) {
        try {
            Escalation updated = service.claim(id, admin, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã nhận xử lý escalation #" + updated.getId());
        } catch (RuntimeException ex) {
            log.error("Claim escalation failed: id={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể nhận escalation: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, status, severity, resourceType);
    }

    // ======================== ENFORCE ========================

    @PostMapping("/{id}/enforce")
    public String enforce(@PathVariable String id,
                          @RequestParam @NotNull ViolationAction action,
                          @RequestParam String reason,
                          @RequestParam(required = false) String note,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String severity,
                          @RequestParam(required = false) String resourceType,
                          @AuthenticationPrincipal CustomUserDetails admin,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            Escalation updated = service.enforce(id, action, reason, note, admin, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã thực thi " + action.name() + " trên escalation #" + updated.getId());
        } catch (RuntimeException ex) {
            log.error("Enforce escalation failed: id={} action={}", id, action, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể enforce: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, status, severity, resourceType);
    }

    // ======================== DISMISS ========================

    @PostMapping("/{id}/dismiss")
    public String dismiss(@PathVariable String id,
                          @RequestParam String reason,
                          @RequestParam(required = false) String note,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String status,
                          @RequestParam(required = false) String severity,
                          @RequestParam(required = false) String resourceType,
                          @AuthenticationPrincipal CustomUserDetails admin,
                          HttpServletRequest request,
                          RedirectAttributes redirectAttributes) {
        try {
            Escalation updated = service.dismiss(id, reason, note, admin, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã đóng escalation #" + updated.getId() + " (dismiss)");
        } catch (RuntimeException ex) {
            log.error("Dismiss escalation failed: id={}", id, ex);
            redirectAttributes.addFlashAttribute("flashError",
                    "Không thể dismiss: " + ex.getMessage());
        }
        return detailRedirect(id, page, size, status, severity, resourceType);
    }

    // ======================== HELPERS ========================

    private String detailRedirect(String id, int page, int size, String status,
                                   String severity, String resourceType) {
        StringBuilder url = new StringBuilder("/admin/escalations/")
                .append(id)
                .append("?page=").append(page)
                .append("&size=").append(size);
        if (status != null && !status.isBlank()) url.append("&status=").append(status);
        if (severity != null && !severity.isBlank()) url.append("&severity=").append(severity);
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
