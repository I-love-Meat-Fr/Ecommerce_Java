package com.ecommerce.cnj70.controller.moderator;

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
 * Phase 3 — Moderator Complaint Web Controller.
 *
 * <p>Routes cho Moderator xử lý khiếu nại Cấp 1 (Customer ↔ Shop đã escalate):</p>
 * <ul>
 *     <li>{@code GET  /moderator/complaints}            — Queue Level 1</li>
 *     <li>{@code GET  /moderator/complaints/{id}}       — Chi tiết + actions</li>
 *     <li>{@code POST /moderator/complaints/{id}/act}    — Claim / Resolve / Reject / Escalate</li>
 * </ul>
 *
 * <p>Moderator chỉ thấy complaint ở Level 1 — service đã enforce.</p>
 */
@Slf4j
@Controller("moderatorComplaintController")
@RequestMapping("/moderator/complaints")
@RequiredArgsConstructor
public class ModeratorComplaintController {

    private static final int PAGE_SIZE = 20;

    private final ComplaintService complaintService;

    // ============================================================
    // QUEUE
    // ============================================================

    /**
     * GET /moderator/complaints — Queue khiếu nại Level 1.
     *
     * <p>Filter theo status: OPEN (escalated, chờ claim), MODERATOR_REVIEW (đã claim),
     * RESOLVED, REJECTED.</p>
     */
    @GetMapping
    public String queue(@AuthenticationPrincipal CustomUserDetails user,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "20") int size,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) String level,
                        Model model) {
        if (user == null) return "redirect:/auth/login";

        int safeSize = (size <= 0) ? PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Page<Complaint> pageObj = complaintService.listModeratorQueue(
                user,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Complaint> complaints = pageObj.getContent();

        // Filter status (chỉ filter trên page hiện tại — moderation queue ít items)
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

        // Stats
        long escalatedCount = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.ESCALATED).count();
        long inReviewCount = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.MODERATOR_REVIEW).count();
        long escalatedL2Count = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.ESCALATED_L2).count();
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
        model.addAttribute("levelFilter", level != null ? level : "");
        model.addAttribute("escalatedCount", escalatedCount);
        model.addAttribute("inReviewCount", inReviewCount);
        model.addAttribute("escalatedL2Count", escalatedL2Count);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("statusChoices", STATUS_CHOICES);
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        model.addAttribute("levelLabels", LEVEL_LABELS_VI);
        return "moderator/complaint-list";
    }

    // ============================================================
    // DETAIL
    // ============================================================

    /**
     * GET /moderator/complaints/{id} — Chi tiết với form action.
     */
    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal CustomUserDetails user,
                         @PathVariable String id,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "20") int size,
                         @RequestParam(required = false) String status,
                         @RequestParam(required = false) String level,
                         Model model) {
        if (user == null) return "redirect:/auth/login";

        try {
            Complaint complaint = complaintService.getById(id, user);

            // Enforce: Moderator chỉ xem Level 1 / Level 2
            if (complaint.getLevel() == ComplaintLevel.LEVEL_0) {
                model.addAttribute("error", "Moderator không được xem khiếu nại Level 0 (giữa Customer ↔ Shop).");
                return "moderator/complaint-detail";
            }

            List<TimelineEvent> timeline = buildTimeline(complaint);
            List<ConversationMessage> conversation = buildConversation(complaint);
            HandlerInfo handler = determineHandler(complaint);

            boolean isClaimed = complaint.getAssignedModeratorId() != null;
            boolean canClaim = complaint.getStatus() == ComplaintStatus.ESCALATED
                    || complaint.getStatus() == ComplaintStatus.MODERATOR_REVIEW;
            boolean canResolve = !complaint.getStatus().isTerminal()
                    && complaint.getLevel() == ComplaintLevel.LEVEL_1;
            boolean canReject = !complaint.getStatus().isTerminal()
                    && complaint.getLevel() == ComplaintLevel.LEVEL_1;
            boolean canEscalateL2 = !complaint.getStatus().isTerminal()
                    && complaint.getLevel() == ComplaintLevel.LEVEL_1;

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
            model.addAttribute("canEscalateL2", canEscalateL2);
            model.addAttribute("reasonLabel", reasonLabel);
            model.addAttribute("statusLabel", STATUS_LABELS_VI.getOrDefault(complaint.getStatus(),
                    complaint.getStatus() != null ? complaint.getStatus().name() : ""));
            model.addAttribute("levelLabel", LEVEL_LABELS_VI.getOrDefault(complaint.getLevel(),
                    complaint.getLevel() != null ? complaint.getLevel().name() : ""));
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("statusFilter", status != null ? status : "");
            model.addAttribute("levelFilter", level != null ? level : "");
            model.addAttribute("statusLabels", STATUS_LABELS_VI);
            model.addAttribute("reasonLabels", REASON_LABELS_VI);
            return "moderator/complaint-detail";
        } catch (Exception e) {
            log.warn("Moderator load complaint detail failed user={} id={}: {}", user.getId(), id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("complaintId", id);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("statusFilter", status != null ? status : "");
            model.addAttribute("levelFilter", level != null ? level : "");
            return "moderator/complaint-detail";
        }
    }

    // ============================================================
    // ACTIONS — Claim / Resolve / Reject / Escalate L2
    // ============================================================

    /**
     * POST /moderator/complaints/{id}/act — Moderator xử lý (CLAIM | RESOLVE | REJECT | ESCALATE).
     */
    @PostMapping("/{id}/act")
    public String act(@AuthenticationPrincipal CustomUserDetails user,
                      @PathVariable String id,
                      @RequestParam("action") String action,
                      @RequestParam(value = "reason", required = false) String reason,
                      @RequestParam(value = "note", required = false) String note,
                      @RequestParam(defaultValue = "0") int page,
                      @RequestParam(defaultValue = "20") int size,
                      @RequestParam(required = false) String status,
                      @RequestParam(required = false) String level,
                      RedirectAttributes ra) {
        if (user == null) return "redirect:/auth/login";
        try {
            ResolveComplaintReq req = new ResolveComplaintReq();
            req.setAction(action.toUpperCase());
            // RESOLVE / REJECT / ESCALATE đều cần reason
            String resolvedReason = StringUtils.hasText(reason) ? reason
                    : ("CLAIM".equalsIgnoreCase(action) ? "Moderator claim case" : "Moderator xử lý case");
            req.setReason(resolvedReason);
            req.setNote(note);
            complaintService.moderatorAct(id, req, user);
            log.info("Moderator {} {} complaint {}", user.getId(), action, id);

            String actionLabel = switch (action.toUpperCase()) {
                case "CLAIM" -> "Đã nhận xử lý khiếu nại.";
                case "RESOLVE" -> "Đã giải quyết khiếu nại.";
                case "REJECT" -> "Đã từ chối khiếu nại.";
                case "ESCALATE" -> "Đã chuyển khiếu nại lên Admin.";
                default -> "Đã xử lý.";
            };
            ra.addFlashAttribute("flashSuccess", actionLabel);

            return "redirect:/moderator/complaints/" + id + "?page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status)
                    + "&level=" + (level == null ? "" : level);
        } catch (Exception e) {
            log.warn("Moderator act failed user={} complaint={} action={}: {}",
                    user.getId(), id, action, e.getMessage());
            return "redirect:/moderator/complaints/" + id + "?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status)
                    + "&level=" + (level == null ? "" : level);
        }
    }

    // ============================================================
    // HELPERS — Build view DTOs
    // ============================================================

    private List<TimelineEvent> buildTimeline(Complaint c) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("OPEN", "Khiếu nại được tạo",
                "Customer đã mở khiếu nại.", c.getCreatedAt(), "fas fa-flag", "completed"));

        if (c.getVendorRespondedAt() != null) {
            events.add(new TimelineEvent("VENDOR_RESPONDED", "Shop đã phản hồi",
                    "Shop đã gửi phản hồi.", c.getVendorRespondedAt(), "fas fa-comment-dots", "completed"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_1 || c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.ESCALATED
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW) {
            events.add(new TimelineEvent("ESCALATED", "Đã chuyển lên Moderator",
                    "Bạn đang xem xét case này ở Cấp 1.",
                    c.getModeratorAssignedAt() != null ? c.getModeratorAssignedAt() : c.getUpdatedAt(),
                    "fas fa-level-up-alt", "completed"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_2 || c.getStatus() == ComplaintStatus.ESCALATED_L2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
            events.add(new TimelineEvent("ADMIN_REVIEW", "Đã chuyển lên Admin",
                    "Moderator đã leo thang lên Admin.",
                    c.getAdminAssignedAt() != null ? c.getAdminAssignedAt() : c.getUpdatedAt(),
                    "fas fa-shield-alt", "completed"));
        }

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.RESOLVED) {
            String body = c.getDecisionReason() != null
                    ? truncate(c.getDecisionReason(), 200)
                    : (c.getDecision() != null ? truncate(c.getDecision(), 200)
                    : "Case đã được giải quyết.");
            events.add(new TimelineEvent("RESOLVED", "Đã giải quyết", body,
                    c.getResolvedAt(), "fas fa-check-circle", "completed"));
        }

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.REJECTED) {
            String body = c.getDecisionReason() != null
                    ? truncate(c.getDecisionReason(), 200)
                    : "Case đã bị từ chối.";
            events.add(new TimelineEvent("REJECTED", "Bị từ chối", body,
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
                    c.getCreatedAt(),
                    "fas fa-flag",
                    "Customer"));
        }

        if (StringUtils.hasText(c.getVendorResponse())) {
            String vendorName = c.getVendorRespondedByEmail() != null
                    ? c.getVendorRespondedByEmail() : "Shop";
            msgs.add(new ConversationMessage(
                    "VENDOR",
                    vendorName,
                    c.getVendorResponse(),
                    c.getVendorRespondedAt() != null ? c.getVendorRespondedAt() : c.getUpdatedAt(),
                    "fas fa-store",
                    "Shop"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_1 || c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                || (c.getResolvedByRole() != null && "MODERATOR".equals(c.getResolvedByRole()))) {
            String moderatorName = c.getAssignedModeratorEmail() != null
                    ? c.getAssignedModeratorEmail() : "Moderator";
            String body;
            if (c.getStatus() == ComplaintStatus.MODERATOR_REVIEW) {
                body = "Moderator đang xem xét khiếu nại.";
            } else if ("MODERATOR".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.RESOLVED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Moderator đã giải quyết.";
            } else if ("MODERATOR".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.REJECTED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Moderator đã từ chối.";
            } else {
                body = "Moderator đã claim case.";
            }
            msgs.add(new ConversationMessage(
                    "MODERATOR",
                    moderatorName,
                    body,
                    c.getModeratorAssignedAt() != null ? c.getModeratorAssignedAt() : c.getUpdatedAt(),
                    "fas fa-user-shield",
                    "Moderator"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_2 || c.getStatus() == ComplaintStatus.ESCALATED_L2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW
                || (c.getResolvedByRole() != null && "ADMIN".equals(c.getResolvedByRole()))) {
            String adminName = c.getAssignedAdminEmail() != null
                    ? c.getAssignedAdminEmail() : "Admin";
            String body;
            if (c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
                body = "Admin đang xem xét case.";
            } else if ("ADMIN".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.RESOLVED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Admin đã giải quyết.";
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
                    "fas fa-shield-halved",
                    "Admin"));
        }

        msgs.sort(Comparator.comparing(ConversationMessage::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return msgs;
    }

    private HandlerInfo determineHandler(Complaint c) {
        if (c.getStatus() == null) return new HandlerInfo("MODERATOR", "Moderator", "fas fa-user-shield");
        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            return new HandlerInfo("RESOLVED", "Đã giải quyết", "fas fa-check-circle");
        }
        if (c.getStatus() == ComplaintStatus.REJECTED || c.getStatus() == ComplaintStatus.CLOSED) {
            return new HandlerInfo("CLOSED", "Đã đóng", "fas fa-times-circle");
        }
        if (c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW
                || c.getStatus() == ComplaintStatus.ESCALATED_L2) {
            String name = c.getAssignedAdminEmail() != null ? c.getAssignedAdminEmail() : "Admin đang xem xét";
            return new HandlerInfo("ADMIN", name, "fas fa-shield-halved");
        }
        String name = c.getAssignedModeratorEmail() != null
                ? c.getAssignedModeratorEmail() : "Bạn (chờ claim)";
        if (c.getStatus() == ComplaintStatus.MODERATOR_REVIEW) {
            return new HandlerInfo("MODERATOR", name + " (đã claim)", "fas fa-user-shield");
        }
        return new HandlerInfo("MODERATOR", name, "fas fa-user-shield");
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
        STATUS_CHOICES.add(Map.entry("ESCALATED", "Chờ claim"));
        STATUS_CHOICES.add(Map.entry("MODERATOR_REVIEW", "Đang xem xét"));
        STATUS_CHOICES.add(Map.entry("ESCALATED_L2", "Đã escalate Admin"));
        STATUS_CHOICES.add(Map.entry("ADMIN_REVIEW", "Admin đang xem"));
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
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED, "Chờ Moderator claim");
        STATUS_LABELS_VI.put(ComplaintStatus.MODERATOR_REVIEW, "Moderator đang xem xét");
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED_L2, "Đã chuyển Admin");
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
