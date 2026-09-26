package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.dto.complaint.ResolveComplaintReq;
import com.ecommerce.cnj70.enums.ComplaintLevel;
import com.ecommerce.cnj70.enums.ComplaintStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3 — Admin Complaint Web Controller (Cấp 2 — cuối cùng).
 *
 * <p>Routes:</p>
 * <ul>
 *     <li>{@code GET  /admin/complaints}            — Queue Level 2</li>
 *     <li>{@code GET  /admin/complaints/{id}}       — Chi tiết + actions</li>
 *     <li>{@code POST /admin/complaints/{id}/act}    — Claim / Resolve / Reject</li>
 * </ul>
 *
 * <p>Admin xử lý khiếu nại cuối cùng — Level 2 được escalate từ Moderator
 * hoặc do Moderator quá hạn (Scheduler).</p>
 */
@Slf4j
@Controller("adminComplaintController")
@RequestMapping("/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private static final int PAGE_SIZE = 20;

    private final ComplaintService complaintService;

    // ============================================================
    // QUEUE
    // ============================================================

    /**
     * GET /admin/complaints — Queue Level 2.
     */
    @GetMapping
    public String queue(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) String status,
                        Model model) {
        if (user == null) return "redirect:/auth/login";

        int safeSize = (size <= 0) ? PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Page<Complaint> pageObj = complaintService.listAdminLevel2Queue(
                user,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Complaint> complaints = pageObj.getContent();

        List<Complaint> filtered = complaints;
        ComplaintStatus statusFilter = null;
        if (StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status)) {
            try {
                statusFilter = ComplaintStatus.valueOf(status);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (statusFilter != null) {
            final ComplaintStatus finalStatus = statusFilter;
            filtered = complaints.stream().filter(c -> c.getStatus() == finalStatus).toList();
        }

        long escalatedL2Count = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.ESCALATED_L2).count();
        long inReviewCount = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.ADMIN_REVIEW).count();
        long resolvedCount = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        long rejectedCount = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.REJECTED).count();

        model.addAttribute("complaints", filtered);
        model.addAttribute("page", pageObj.getNumber());
        model.addAttribute("size", pageObj.getSize());
        model.addAttribute("totalPages", pageObj.getTotalPages());
        model.addAttribute("totalItems", pageObj.getTotalElements());
        model.addAttribute("hasNext", pageObj.hasNext());
        model.addAttribute("hasPrev", pageObj.hasPrevious());
        model.addAttribute("statusFilter", status != null ? status : "");
        model.addAttribute("escalatedL2Count", escalatedL2Count);
        model.addAttribute("inReviewCount", inReviewCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("statusChoices", STATUS_CHOICES);
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        model.addAttribute("levelLabels", LEVEL_LABELS_VI);
        return "admin/complaint-list";
    }

    // ============================================================
    // DETAIL
    // ============================================================

    /**
     * GET /admin/complaints/{id} — Chi tiết với form action Level 2.
     */
    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal CustomUserDetails user,
                         @PathVariable String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         @RequestParam(required = false) String status,
                         Model model) {
        if (user == null) return "redirect:/auth/login";

        try {
            Complaint complaint = complaintService.getById(id, user);

            // Enforce: Admin chỉ xem Level 2 ở queue chính; Level 0/1 chỉ audit
            if (complaint.getLevel() != ComplaintLevel.LEVEL_2) {
                model.addAttribute("error",
                        "Admin xem Level 0/1 trong escalation queue. Vào đây chỉ xử lý Level 2 (Admin cuối).");
                model.addAttribute("complaintId", id);
                model.addAttribute("page", page);
                model.addAttribute("size", size);
                model.addAttribute("statusFilter", status != null ? status : "");
                return "admin/complaint-detail";
            }

            List<TimelineEvent> timeline = buildTimeline(complaint);
            List<ConversationMessage> conversation = buildConversation(complaint);
            HandlerInfo handler = determineHandler(complaint);

            boolean isClaimed = complaint.getAssignedAdminId() != null;
            boolean canClaim = complaint.getStatus() == ComplaintStatus.ESCALATED_L2
                    || complaint.getStatus() == ComplaintStatus.ADMIN_REVIEW;
            boolean canResolve = !complaint.getStatus().isTerminal()
                    && complaint.getLevel() == ComplaintLevel.LEVEL_2;
            boolean canReject = !complaint.getStatus().isTerminal()
                    && complaint.getLevel() == ComplaintLevel.LEVEL_2;

            String reasonLabel = REASON_LABELS_VI.getOrDefault(complaint.getReason(),
                    complaint.getReason() != null ? complaint.getReason().name() : "");

            model.addAttribute("complaint", complaint);
            model.addAttribute("timeline", timeline);
            model.addAttribute("conversation", conversation);
            model.addAttribute("handler", handler);
            model.addAttribute("isClaimed", isClaimed);
            model.addAttribute("canClaim", canClaim);
            model.addAttribute("canResolve", canResolve);
            model.addAttribute("canReject", canReject);
            model.addAttribute("reasonLabel", reasonLabel);
            model.addAttribute("statusLabel", STATUS_LABELS_VI.getOrDefault(complaint.getStatus(),
                    complaint.getStatus() != null ? complaint.getStatus().name() : ""));
            model.addAttribute("levelLabel", LEVEL_LABELS_VI.getOrDefault(complaint.getLevel(),
                    complaint.getLevel() != null ? complaint.getLevel().name() : ""));
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("statusFilter", status != null ? status : "");
            model.addAttribute("statusLabels", STATUS_LABELS_VI);
            model.addAttribute("reasonLabels", REASON_LABELS_VI);
            return "admin/complaint-detail";
        } catch (Exception e) {
            log.warn("Admin load complaint detail failed user={} id={}: {}", user.getId(), id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("complaintId", id);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("statusFilter", status != null ? status : "");
            return "admin/complaint-detail";
        }
    }

    // ============================================================
    // ACTIONS — Claim / Resolve / Reject
    // ============================================================

    @PostMapping("/{id}/act")
    public String act(@AuthenticationPrincipal CustomUserDetails user,
                      @PathVariable String id,
                      @RequestParam("action") String action,
                      @RequestParam(value = "reason", required = false) String reason,
                      @RequestParam(value = "note", required = false) String note,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size,
                      @RequestParam(required = false) String status,
                      RedirectAttributes ra) {
        if (user == null) return "redirect:/auth/login";
        try {
            ResolveComplaintReq req = new ResolveComplaintReq();
            req.setAction(action.toUpperCase());
            String resolvedReason = StringUtils.hasText(reason) ? reason
                    : ("CLAIM".equalsIgnoreCase(action) ? "Admin claim case"
                            : "Admin xử lý case cấp cuối");
            req.setReason(resolvedReason);
            req.setNote(note);
            complaintService.adminAct(id, req, user);
            log.info("Admin {} {} complaint {}", user.getId(), action, id);

            String actionLabel = switch (action.toUpperCase()) {
                case "CLAIM" -> "Đã nhận xử lý khiếu nại.";
                case "RESOLVE" -> "Đã ra phán quyết cuối cùng — RESOLVED.";
                case "REJECT" -> "Đã từ chối khiếu nại (cấp cuối).";
                default -> "Đã xử lý.";
            };
            ra.addFlashAttribute("flashSuccess", actionLabel);

            return "redirect:/admin/complaints/" + id + "?page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        } catch (Exception e) {
            log.warn("Admin act failed user={} complaint={} action={}: {}",
                    user.getId(), id, action, e.getMessage());
            return "redirect:/admin/complaints/" + id + "?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private List<TimelineEvent> buildTimeline(Complaint c) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("OPEN", "Khiếu nại được tạo",
                "Customer mở khiếu nại.", c.getCreatedAt(), "fas fa-flag", "completed"));

        if (c.getVendorRespondedAt() != null) {
            events.add(new TimelineEvent("VENDOR_RESPONDED", "Shop phản hồi",
                    "Shop đã phản hồi.", c.getVendorRespondedAt(), "fas fa-store", "completed"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_1
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                || c.getStatus() == ComplaintStatus.ESCALATED) {
            events.add(new TimelineEvent("MODERATOR_REVIEW", "Moderator xem xét",
                    "Moderator đã tham gia.", c.getModeratorAssignedAt() != null
                            ? c.getModeratorAssignedAt() : c.getUpdatedAt(),
                    "fas fa-user-shield", "completed"));
        }

        events.add(new TimelineEvent("ADMIN_REVIEW", "Admin xem xét (Cấp 2)",
                "Moderator đã escalate lên Admin — bạn đang xử lý cấp cuối.",
                c.getAdminAssignedAt() != null ? c.getAdminAssignedAt() : c.getUpdatedAt(),
                "fas fa-shield-alt", "completed"));

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.RESOLVED) {
            String body = c.getDecisionReason() != null
                    ? truncate(c.getDecisionReason(), 200)
                    : "Admin đã giải quyết cấp cuối.";
            events.add(new TimelineEvent("RESOLVED", "Đã giải quyết", body,
                    c.getResolvedAt(), "fas fa-check-circle", "completed"));
        }

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.REJECTED) {
            String body = c.getDecisionReason() != null
                    ? truncate(c.getDecisionReason(), 200)
                    : "Admin đã từ chối.";
            events.add(new TimelineEvent("REJECTED", "Bị từ chối (cấp cuối)", body,
                    c.getResolvedAt(), "fas fa-times-circle", "completed"));
        }

        events.sort(Comparator.comparing(TimelineEvent::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    private List<ConversationMessage> buildConversation(Complaint c) {
        List<ConversationMessage> msgs = new ArrayList<>();

        if (StringUtils.hasText(c.getDescription())) {
            msgs.add(new ConversationMessage(
                    "CUSTOMER",
                    c.getCustomerName() != null ? c.getCustomerName() : "Customer",
                    c.getDescription(),
                    c.getCreatedAt(), "fas fa-flag", "Customer"));
        }

        if (StringUtils.hasText(c.getVendorResponse())) {
            String vendorName = c.getVendorRespondedByEmail() != null
                    ? c.getVendorRespondedByEmail() : "Shop";
            msgs.add(new ConversationMessage(
                    "VENDOR",
                    vendorName,
                    c.getVendorResponse(),
                    c.getVendorRespondedAt() != null ? c.getVendorRespondedAt() : c.getUpdatedAt(),
                    "fas fa-store", "Shop"));
        }

        if (c.getAssignedModeratorEmail() != null
                || c.getResolvedByRole() != null && "MODERATOR".equals(c.getResolvedByRole())) {
            msgs.add(new ConversationMessage(
                    "MODERATOR",
                    c.getAssignedModeratorEmail() != null ? c.getAssignedModeratorEmail() : "Moderator",
                    c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                            ? "Moderator đã xem xét và escalate lên Admin."
                            : (c.getDecisionReason() != null ? c.getDecisionReason()
                                    : "Moderator đã escalate lên Admin."),
                    c.getModeratorAssignedAt() != null ? c.getModeratorAssignedAt() : c.getUpdatedAt(),
                    "fas fa-user-shield", "Moderator"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_2 || c.getStatus() == ComplaintStatus.ADMIN_REVIEW
                || (c.getResolvedByRole() != null && "ADMIN".equals(c.getResolvedByRole()))) {
            String adminName = c.getAssignedAdminEmail() != null
                    ? c.getAssignedAdminEmail() : "Admin";
            String body;
            if (c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
                body = "Admin đang xem xét — cấp xử lý cuối cùng.";
            } else if ("ADMIN".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.RESOLVED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Admin đã ra phán quyết cuối.";
            } else if ("ADMIN".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.REJECTED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Admin đã từ chối.";
            } else {
                body = "Admin đang xem xét.";
            }
            msgs.add(new ConversationMessage(
                    "ADMIN",
                    adminName,
                    body,
                    c.getAdminAssignedAt() != null ? c.getAdminAssignedAt() : c.getUpdatedAt(),
                    "fas fa-shield-halved", "Admin"));
        }

        msgs.sort(Comparator.comparing(ConversationMessage::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return msgs;
    }

    private HandlerInfo determineHandler(Complaint c) {
        if (c.getStatus() == null) return new HandlerInfo("ADMIN", "Admin", "fas fa-shield-halved");
        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            return new HandlerInfo("RESOLVED", "Đã giải quyết", "fas fa-check-circle");
        }
        if (c.getStatus() == ComplaintStatus.REJECTED || c.getStatus() == ComplaintStatus.CLOSED) {
            return new HandlerInfo("CLOSED", "Đã đóng", "fas fa-times-circle");
        }
        String name = c.getAssignedAdminEmail() != null
                ? c.getAssignedAdminEmail() : "Bạn (chờ claim)";
        if (c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
            return new HandlerInfo("ADMIN", name + " (đã claim)", "fas fa-shield-halved");
        }
        return new HandlerInfo("ADMIN", name, "fas fa-shield-halved");
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    // ============================================================
    // LABELS
    // ============================================================

    private static final Map<com.ecommerce.cnj70.enums.ComplaintReason, String> REASON_LABELS_VI = new HashMap<>();
    private static final Map<ComplaintStatus, String> STATUS_LABELS_VI = new HashMap<>();
    private static final Map<ComplaintLevel, String> LEVEL_LABELS_VI = new HashMap<>();

    static final List<Map.Entry<String, String>> STATUS_CHOICES = new ArrayList<>();
    static {
        STATUS_CHOICES.add(Map.entry("ALL", "Tất cả"));
        STATUS_CHOICES.add(Map.entry("ESCALATED_L2", "Chờ Admin claim"));
        STATUS_CHOICES.add(Map.entry("ADMIN_REVIEW", "Đang xem xét"));
        STATUS_CHOICES.add(Map.entry("RESOLVED", "Đã giải quyết"));
        STATUS_CHOICES.add(Map.entry("REJECTED", "Bị từ chối"));
    }

    static {
        REASON_LABELS_VI.put(com.ecommerce.cnj70.enums.ComplaintReason.NON_DELIVERY, "Không giao hàng");
        REASON_LABELS_VI.put(com.ecommerce.cnj70.enums.ComplaintReason.WRONG_PRODUCT, "Sai sản phẩm");
        REASON_LABELS_VI.put(com.ecommerce.cnj70.enums.ComplaintReason.MISSING_ITEM, "Thiếu sản phẩm");
        REASON_LABELS_VI.put(com.ecommerce.cnj70.enums.ComplaintReason.DAMAGED_PRODUCT, "Sản phẩm hư hỏng");
        REASON_LABELS_VI.put(com.ecommerce.cnj70.enums.ComplaintReason.NOT_AS_DESCRIBED, "Không đúng mô tả");
        REASON_LABELS_VI.put(com.ecommerce.cnj70.enums.ComplaintReason.WARRANTY_ISSUE, "Vấn đề bảo hành");
        REASON_LABELS_VI.put(com.ecommerce.cnj70.enums.ComplaintReason.OTHER, "Khác");

        STATUS_LABELS_VI.put(ComplaintStatus.OPEN, "Đang chờ shop");
        STATUS_LABELS_VI.put(ComplaintStatus.VENDOR_RESPONDED, "Shop đã phản hồi");
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED, "Chờ Moderator");
        STATUS_LABELS_VI.put(ComplaintStatus.MODERATOR_REVIEW, "Moderator đang xem");
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED_L2, "Chờ Admin claim");
        STATUS_LABELS_VI.put(ComplaintStatus.ADMIN_REVIEW, "Admin đang xem xét");
        STATUS_LABELS_VI.put(ComplaintStatus.RESOLVED, "Đã giải quyết");
        STATUS_LABELS_VI.put(ComplaintStatus.CLOSED, "Đã đóng");
        STATUS_LABELS_VI.put(ComplaintStatus.REJECTED, "Bị từ chối");

        LEVEL_LABELS_VI.put(ComplaintLevel.LEVEL_0, "Cấp 0");
        LEVEL_LABELS_VI.put(ComplaintLevel.LEVEL_1, "Cấp 1 — Moderator");
        LEVEL_LABELS_VI.put(ComplaintLevel.LEVEL_2, "Cấp 2 — Admin");
    }

    // ============================================================
    // View DTOs
    // ============================================================

    public static class TimelineEvent {
        private final String key;
        private final String title;
        private final String description;
        private final LocalDateTime timestamp;
        private final String icon;
        private final String state;

        public TimelineEvent(String key, String title, String description,
                            LocalDateTime timestamp, String icon, String state) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.timestamp = timestamp;
            this.icon = icon;
            this.state = state;
        }

        public String getKey() { return key; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getIcon() { return icon; }
        public String getState() { return state; }
    }

    public static class ConversationMessage {
        private final String actorRole;
        private final String actorName;
        private final String content;
        private final LocalDateTime timestamp;
        private final String icon;
        private final String actorLabel;

        public ConversationMessage(String actorRole, String actorName, String content,
                                   LocalDateTime timestamp, String icon, String actorLabel) {
            this.actorRole = actorRole;
            this.actorName = actorName;
            this.content = content;
            this.timestamp = timestamp;
            this.icon = icon;
            this.actorLabel = actorLabel;
        }

        public String getActorRole() { return actorRole; }
        public String getActorName() { return actorName; }
        public String getContent() { return content; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getIcon() { return icon; }
        public String getActorLabel() { return actorLabel; }
    }

    public static class HandlerInfo {
        private final String role;
        private final String displayName;
        private final String iconClass;

        public HandlerInfo(String role, String displayName, String iconClass) {
            this.role = role;
            this.displayName = displayName;
            this.iconClass = iconClass;
        }

        public String getRole() { return role; }
        public String getDisplayName() { return displayName; }
        public String getIconClass() { return iconClass; }
    }
}
