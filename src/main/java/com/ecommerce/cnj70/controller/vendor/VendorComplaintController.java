package com.ecommerce.cnj70.controller.vendor;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.dto.complaint.VendorRespondReq;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3 — Vendor Complaint Web Controller.
 *
 * <p>Routes (HTML page rendering) cho Vendor (chủ shop):</p>
 * <ul>
 *     <li>{@code GET  /vendor/complaints}            — Danh sách khiếu nại của shop</li>
 *     <li>{@code GET  /vendor/complaints/{id}}       — Chi tiết + form phản hồi</li>
 *     <li>{@code POST /vendor/complaints/{id}/respond} — Vendor phản hồi Level 0</li>
 *     <li>{@code POST /vendor/complaints/{id}/escalate} — Vendor yêu cầu Moderator</li>
 *     <li>{@code POST /vendor/complaints/{id}/resolve}  — Vendor đồng ý giải quyết L0</li>
 * </ul>
 *
 * <p>Phân biệt với REST API ở {@code controller.complaint.ComplaintApiController}
 * — controller này render Thymeleaf views cho dashboard web.</p>
 *
 * <p>Security: route-level role enforcement bởi SecurityConfig — chỉ VENDOR mới
 * vào được. Ownership & state guards ở {@link ComplaintService}.</p>
 */
@Slf4j
@Controller("vendorComplaintController")
@RequestMapping("/vendor/complaints")
@RequiredArgsConstructor
public class VendorComplaintController {

    private static final int PAGE_SIZE = 20;

    private final ComplaintService complaintService;

    // ============================================================
    // LIST — Vendor queue
    // ============================================================

    /**
     * GET /vendor/complaints — Danh sách khiếu nại của shop.
     *
     * <p>Query params:</p>
     * <ul>
     *     <li>{@code status} — Filter theo trạng thái (optional)</li>
     *     <li>{@code page}   — Số trang (0-indexed)</li>
     *     <li>{@code size}   — Số phần tử mỗi trang</li>
     * </ul>
     */
    @GetMapping
    public String list(@AuthenticationPrincipal CustomUserDetails user,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size,
                       @RequestParam(required = false) String status,
                       Model model) {
        if (user == null) return "redirect:/auth/login";

        int safeSize = (size <= 0) ? PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Page<Complaint> pageObj = complaintService.listShopComplaints(
                user,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Complaint> complaints = pageObj.getContent();

        // Filter theo status nếu có
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

        // Stats cho sidebar filter buttons
        long openCount = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.OPEN).count();
        long respondedCount = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.VENDOR_RESPONDED).count();
        long escalatedCount = complaints.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.ESCALATED
                        || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                        || c.getStatus() == ComplaintStatus.ESCALATED_L2
                        || c.getStatus() == ComplaintStatus.ADMIN_REVIEW).count();
        long resolvedCount = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        long rejectedCount = complaints.stream().filter(c -> c.getStatus() == ComplaintStatus.REJECTED).count();

        model.addAttribute("complaints", filtered);
        model.addAttribute("page", pageObj.getNumber());
        model.addAttribute("size", pageObj.getSize());
        model.addAttribute("totalPages", pageObj.getTotalPages());
        model.addAttribute("totalItems", pageObj.getTotalElements());
        model.addAttribute("hasNext", pageObj.hasNext());
        model.addAttribute("hasPrev", pageObj.hasPrevious());
        model.addAttribute("statusFilter", status != null ? status : "");
        model.addAttribute("openCount", openCount);
        model.addAttribute("respondedCount", respondedCount);
        model.addAttribute("escalatedCount", escalatedCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("statusChoices", STATUS_LABELS_VI);
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        model.addAttribute("levelLabels", LEVEL_LABELS_VI);
        return "vendor/complaint-list";
    }

    // ============================================================
    // DETAIL — Vendor xem chi tiết + actions
    // ============================================================

    /**
     * GET /vendor/complaints/{id} — Chi tiết khiếu nại của shop.
     *
     * <p>Hiển thị: status, customer info, description, evidence, timeline,
     * form phản hồi (nếu đang Level 0 OPEN) và các action escalate / resolve.</p>
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

            // Build timeline + conversation + handler
            List<TimelineEvent> timeline = buildTimeline(complaint);
            List<ConversationMessage> conversation = buildConversation(complaint);
            HandlerInfo handler = determineHandler(complaint);

            // Vendor actions: cho phép khi nào?
            boolean canRespond = canVendorRespond(complaint);
            boolean canEscalate = canVendorEscalate(complaint);
            boolean canResolve = canVendorResolve(complaint);
            // Phase 3A §47 — flag phân biệt lần đầu vs bổ sung
            boolean isInitialResponse = complaint.getStatus() == ComplaintStatus.OPEN;
            int replyCount = complaint.getVendorReplyCount();

            // SLA deadline (Level 0)
            long minutesToDeadline = -1;
            boolean deadlineExpired = false;
            Integer deadlinePercent = null;
            if (complaint.getVendorResponseDeadline() != null
                    && complaint.getLevel() == ComplaintLevel.LEVEL_0
                    && (complaint.getStatus() == ComplaintStatus.OPEN
                            || complaint.getStatus() == ComplaintStatus.VENDOR_RESPONDED)) {
                long nowMs = System.currentTimeMillis();
                long due = complaint.getVendorResponseDeadline()
                        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                long createdMs = complaint.getCreatedAt() != null
                        ? complaint.getCreatedAt().atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        : nowMs - (48L * 3600 * 1000);
                long totalWindow = Math.max(due - createdMs, 1);
                long elapsed = Math.max(nowMs - createdMs, 0);
                deadlinePercent = (int) Math.min(100, Math.max(0, (elapsed * 100) / totalWindow));
                minutesToDeadline = (due - nowMs) / (60_000L);
                deadlineExpired = minutesToDeadline < 0;
            }

            String reasonLabel = REASON_LABELS_VI.getOrDefault(complaint.getReason(),
                    complaint.getReason() != null ? complaint.getReason().name() : "");

            model.addAttribute("complaint", complaint);
            model.addAttribute("timeline", timeline);
            model.addAttribute("conversation", conversation);
            model.addAttribute("handler", handler);
            model.addAttribute("canRespond", canRespond);
            model.addAttribute("canEscalate", canEscalate);
            model.addAttribute("canResolve", canResolve);
            model.addAttribute("isInitialResponse", isInitialResponse);
            model.addAttribute("replyCount", replyCount);
            model.addAttribute("minutesToDeadline", minutesToDeadline);
            model.addAttribute("deadlineExpired", deadlineExpired);
            model.addAttribute("deadlinePercent", deadlinePercent);
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
            return "vendor/complaint-detail";
        } catch (Exception e) {
            log.warn("Vendor load complaint detail failed user={} id={}: {}", user.getId(), id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("complaintId", id);
            model.addAttribute("page", page);
            model.addAttribute("size", size);
            model.addAttribute("statusFilter", status != null ? status : "");
            return "vendor/complaint-detail";
        }
    }

    // ============================================================
    // ACTIONS — Vendor phản hồi / escalate / resolve
    // ============================================================

    /**
     * POST /vendor/complaints/{id}/respond — Vendor gửi phản hồi Level 0.
     */
    @PostMapping("/{id}/respond")
    public String respond(@AuthenticationPrincipal CustomUserDetails user,
                          @PathVariable String id,
                          @RequestParam("response") String response,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String status,
                          RedirectAttributes ra) {
        if (user == null) return "redirect:/auth/login";
        try {
            VendorRespondReq req = new VendorRespondReq();
            req.setResponse(response);
            complaintService.vendorRespond(id, req, user);
            log.info("Vendor {} responded to complaint {}", user.getId(), id);
            ra.addFlashAttribute("flashSuccess", "Đã gửi phản hồi cho khách hàng.");
            return "redirect:/vendor/complaints/" + id + "?page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        } catch (Exception e) {
            log.warn("Vendor respond failed user={} complaint={}: {}", user.getId(), id, e.getMessage());
            return "redirect:/vendor/complaints/" + id + "?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        }
    }

    /**
     * POST /vendor/complaints/{id}/escalate — Vendor yêu cầu Moderator.
     */
    @PostMapping("/{id}/escalate")
    public String escalate(@AuthenticationPrincipal CustomUserDetails user,
                           @PathVariable String id,
                           @RequestParam(value = "reason", required = false) String reason,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "20") int size,
                           @RequestParam(required = false) String status,
                           RedirectAttributes ra) {
        if (user == null) return "redirect:/auth/login";
        try {
            complaintService.escalateFromLevel0(
                    id,
                    StringUtils.hasText(reason) ? reason : "Shop yêu cầu Moderator xem xét",
                    user);
            log.info("Vendor {} escalated complaint {}", user.getId(), id);
            ra.addFlashAttribute("flashSuccess", "Đã chuyển khiếu nại lên Moderator.");
            return "redirect:/vendor/complaints/" + id + "?page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        } catch (Exception e) {
            log.warn("Vendor escalate failed user={} complaint={}: {}", user.getId(), id, e.getMessage());
            return "redirect:/vendor/complaints/" + id + "?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        }
    }

    /**
     * POST /vendor/complaints/{id}/resolve — Vendor đồng ý giải quyết L0.
     */
    @PostMapping("/{id}/resolve")
    public String resolve(@AuthenticationPrincipal CustomUserDetails user,
                          @PathVariable String id,
                          @RequestParam(value = "note", required = false) String note,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @RequestParam(required = false) String status,
                          RedirectAttributes ra) {
        if (user == null) return "redirect:/auth/login";
        try {
            complaintService.resolveLevel0(
                    id,
                    StringUtils.hasText(note) ? note : "Shop đồng ý giải quyết",
                    user);
            log.info("Vendor {} resolved complaint {}", user.getId(), id);
            ra.addFlashAttribute("flashSuccess", "Đã đóng khiếu nại.");
            return "redirect:/vendor/complaints/" + id + "?page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        } catch (Exception e) {
            log.warn("Vendor resolve failed user={} complaint={}: {}", user.getId(), id, e.getMessage());
            return "redirect:/vendor/complaints/" + id + "?error="
                    + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8)
                    + "&page=" + page + "&size=" + size
                    + "&status=" + (status == null ? "" : status);
        }
    }

    // ============================================================
    // GUARDS
    // ============================================================

    private boolean canVendorRespond(Complaint c) {
        if (c == null) return false;
        if (c.getLevel() != ComplaintLevel.LEVEL_0) return false;
        if (c.getStatus() == null || c.getStatus().isTerminal()) return false;
        // Phase 3A §47 — Vendor có thể gửi nhiều lần phản hồi (append-mode).
        // Form hiển thị khi OPEN (lần đầu) hoặc VENDOR_RESPONDED (bổ sung/sửa).
        return c.getStatus() == ComplaintStatus.OPEN
                || c.getStatus() == ComplaintStatus.VENDOR_RESPONDED;
    }

    private boolean canVendorEscalate(Complaint c) {
        if (c == null) return false;
        if (c.getLevel() != ComplaintLevel.LEVEL_0) return false;
        if (c.getStatus() == null || c.getStatus().isTerminal()) return false;
        // Vendor có thể escalate khi đã respond (VENDOR_RESPONDED) — đã cố gắng giải quyết
        return c.getStatus() == ComplaintStatus.VENDOR_RESPONDED;
    }

    private boolean canVendorResolve(Complaint c) {
        if (c == null) return false;
        if (c.getLevel() != ComplaintLevel.LEVEL_0) return false;
        if (c.getStatus() == null || c.getStatus().isTerminal()) return false;
        // Vendor chỉ có thể resolve nếu đã respond
        return c.getStatus() == ComplaintStatus.VENDOR_RESPONDED;
    }

    // ============================================================
    // HELPERS — Build view DTOs
    // ============================================================

    private List<TimelineEvent> buildTimeline(Complaint c) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent("OPEN", "Khách hàng gửi khiếu nại",
                "Customer đã mở khiếu nại đối với shop của bạn.",
                c.getCreatedAt(), "fas fa-flag", "completed"));

        if (c.getVendorRespondedAt() != null) {
            events.add(new TimelineEvent("VENDOR_RESPONDED", "Bạn đã phản hồi",
                    "Shop đã gửi phản hồi cho khiếu nại này.",
                    c.getVendorRespondedAt(), "fas fa-comment-dots", "completed"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_1 || c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.ESCALATED
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW) {
            events.add(new TimelineEvent("ESCALATED", "Đã chuyển lên Moderator",
                    "Khiếu nại được Moderator xem xét — Shop không xử lý trực tiếp nữa.",
                    c.getUpdatedAt(), "fas fa-level-up-alt", "completed"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_2 || c.getStatus() == ComplaintStatus.ESCALATED_L2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
            events.add(new TimelineEvent("ADMIN_REVIEW", "Đã chuyển lên Admin",
                    "Moderator đã leo thang lên Admin — cấp xử lý cuối.",
                    c.getUpdatedAt(), "fas fa-shield-alt", "completed"));
        }

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.RESOLVED) {
            String role = c.getResolvedByRole() != null ? c.getResolvedByRole() : "—";
            String body = c.getDecisionReason() != null
                    ? truncate(c.getDecisionReason(), 200)
                    : (c.getDecision() != null ? truncate(c.getDecision(), 200)
                    : "Khiếu nại đã được giải quyết bởi " + role + ".");
            events.add(new TimelineEvent("RESOLVED", "Khiếu nại đã giải quyết", body,
                    c.getResolvedAt(), "fas fa-check-circle", "completed"));
        }

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.REJECTED) {
            String body = c.getDecisionReason() != null
                    ? truncate(c.getDecisionReason(), 200)
                    : "Khiếu nại đã bị từ chối bởi " + c.getResolvedByRole();
            events.add(new TimelineEvent("REJECTED", "Khiếu nại bị từ chối", body,
                    c.getResolvedAt(), "fas fa-times-circle", "completed"));
        }

        events.sort(Comparator.comparing(
                TimelineEvent::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    private List<ConversationMessage> buildConversation(Complaint c) {
        List<ConversationMessage> msgs = new ArrayList<>();

        // 1. Customer's initial complaint
        if (StringUtils.hasText(c.getDescription())) {
            msgs.add(new ConversationMessage(
                    "CUSTOMER",
                    c.getCustomerName() != null ? c.getCustomerName() : "Customer",
                    c.getDescription(),
                    c.getCreatedAt(),
                    "fas fa-flag",
                    "Khách hàng"));
        }

        // 2. Vendor response (if any)
        if (StringUtils.hasText(c.getVendorResponse())) {
            String vendorName = c.getVendorRespondedByEmail() != null
                    ? c.getVendorRespondedByEmail() : "Shop";
            msgs.add(new ConversationMessage(
                    "VENDOR",
                    vendorName,
                    c.getVendorResponse(),
                    c.getVendorRespondedAt() != null ? c.getVendorRespondedAt() : c.getUpdatedAt(),
                    "fas fa-store",
                    "Bạn"));
        }

        // 3. Moderator (Level 1)
        if (c.getLevel() == ComplaintLevel.LEVEL_1 || c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.ESCALATED
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                || (c.getResolvedByRole() != null && "MODERATOR".equals(c.getResolvedByRole()))) {
            String moderatorName = c.getAssignedModeratorEmail() != null
                    ? c.getAssignedModeratorEmail() : "Moderator";
            String body;
            if (c.getStatus() == ComplaintStatus.MODERATOR_REVIEW) {
                body = "Moderator đang xem xét khiếu nại — Shop vui lòng chờ kết quả.";
            } else if ("MODERATOR".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.RESOLVED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Moderator đã giải quyết khiếu nại.";
            } else if ("MODERATOR".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.REJECTED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Moderator đã từ chối khiếu nại sau khi xem xét.";
            } else {
                body = "Moderator đã nhận khiếu nại và đang xem xét.";
            }
            msgs.add(new ConversationMessage(
                    "MODERATOR",
                    moderatorName,
                    body,
                    c.getModeratorAssignedAt() != null ? c.getModeratorAssignedAt() : c.getUpdatedAt(),
                    "fas fa-user-shield",
                    "Moderator"));
        }

        // 4. Admin (Level 2)
        if (c.getLevel() == ComplaintLevel.LEVEL_2 || c.getStatus() == ComplaintStatus.ESCALATED_L2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW
                || (c.getResolvedByRole() != null && "ADMIN".equals(c.getResolvedByRole()))) {
            String adminName = c.getAssignedAdminEmail() != null
                    ? c.getAssignedAdminEmail() : "Admin";
            String body;
            if (c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
                body = "Admin đang xem xét — đây là cấp xử lý cuối cùng.";
            } else if ("ADMIN".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.RESOLVED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Admin đã ra phán quyết cuối cùng.";
            } else if ("ADMIN".equals(c.getResolvedByRole()) && c.getStatus() == ComplaintStatus.REJECTED) {
                body = c.getDecisionReason() != null ? c.getDecisionReason()
                        : "Admin đã giữ nguyên — khiếu nại kết thúc.";
            } else {
                body = "Admin đã nhận khiếu nại và đang xem xét.";
            }
            msgs.add(new ConversationMessage(
                    "ADMIN",
                    adminName,
                    body,
                    c.getAdminAssignedAt() != null ? c.getAdminAssignedAt() : c.getUpdatedAt(),
                    "fas fa-shield-halved",
                    "Admin"));
        }

        msgs.sort(Comparator.comparing(
                ConversationMessage::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return msgs;
    }

    private HandlerInfo determineHandler(Complaint c) {
        if (c.getStatus() == null) return new HandlerInfo("VENDOR", "Shop", "fas fa-store");
        if (c.getStatus() == ComplaintStatus.RESOLVED) {
            return new HandlerInfo("RESOLVED", "Đã giải quyết", "fas fa-check-circle");
        }
        if (c.getStatus() == ComplaintStatus.REJECTED || c.getStatus() == ComplaintStatus.CLOSED) {
            return new HandlerInfo("CLOSED", "Đã đóng", "fas fa-times-circle");
        }
        if (c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW
                || c.getStatus() == ComplaintStatus.ESCALATED_L2) {
            String name = c.getAssignedAdminEmail() != null ? c.getAssignedAdminEmail() : "Đang chờ Admin";
            return new HandlerInfo("ADMIN", name, "fas fa-shield-halved");
        }
        if (c.getLevel() == ComplaintLevel.LEVEL_1
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                || c.getStatus() == ComplaintStatus.ESCALATED) {
            String name = c.getAssignedModeratorEmail() != null
                    ? c.getAssignedModeratorEmail() : "Đang chờ Moderator";
            return new HandlerInfo("MODERATOR", name, "fas fa-user-shield");
        }
        if (c.getStatus() == ComplaintStatus.VENDOR_RESPONDED) {
            return new HandlerInfo("VENDOR", "Bạn (đã phản hồi)", "fas fa-comment-dots");
        }
        return new HandlerInfo("VENDOR", "Bạn (chờ phản hồi)", "fas fa-store");
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

    /** Status choices cho filter dropdown */
    static final List<Map.Entry<String, String>> STATUS_CHOICES = new ArrayList<>();
    static {
        STATUS_CHOICES.add(Map.entry("ALL", "Tất cả"));
        STATUS_CHOICES.add(Map.entry("OPEN", "Đang chờ shop phản hồi"));
        STATUS_CHOICES.add(Map.entry("VENDOR_RESPONDED", "Shop đã phản hồi"));
        STATUS_CHOICES.add(Map.entry("ESCALATED", "Đã chuyển Moderator"));
        STATUS_CHOICES.add(Map.entry("MODERATOR_REVIEW", "Moderator đang xem xét"));
        STATUS_CHOICES.add(Map.entry("ESCALATED_L2", "Đã chuyển Admin"));
        STATUS_CHOICES.add(Map.entry("ADMIN_REVIEW", "Admin đang xem xét"));
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
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED, "Đã chuyển Moderator");
        STATUS_LABELS_VI.put(ComplaintStatus.MODERATOR_REVIEW, "Moderator đang xem xét");
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED_L2, "Đã chuyển Admin");
        STATUS_LABELS_VI.put(ComplaintStatus.ADMIN_REVIEW, "Admin đang xem xét");
        STATUS_LABELS_VI.put(ComplaintStatus.RESOLVED, "Đã giải quyết");
        STATUS_LABELS_VI.put(ComplaintStatus.CLOSED, "Đã đóng");
        STATUS_LABELS_VI.put(ComplaintStatus.REJECTED, "Bị từ chối");

        LEVEL_LABELS_VI.put(ComplaintLevel.LEVEL_0, "Cấp 0 — Shop ↔ Customer");
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
        private final java.time.LocalDateTime timestamp;
        private final String icon;
        private final String state;

        public TimelineEvent(String key, String title, String description,
                            java.time.LocalDateTime timestamp, String icon, String state) {
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
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public String getIcon() { return icon; }
        public String getState() { return state; }
    }

    public static class ConversationMessage {
        private final String actorRole;
        private final String actorName;
        private final String content;
        private final java.time.LocalDateTime timestamp;
        private final String icon;
        private final String actorLabel;

        public ConversationMessage(String actorRole, String actorName, String content,
                                   java.time.LocalDateTime timestamp, String icon, String actorLabel) {
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
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
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
