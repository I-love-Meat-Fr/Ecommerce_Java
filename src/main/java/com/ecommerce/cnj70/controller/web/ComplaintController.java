package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.complaint.CreateComplaintReq;
import com.ecommerce.cnj70.enums.ComplaintLevel;
import com.ecommerce.cnj70.enums.ComplaintReason;
import com.ecommerce.cnj70.enums.ComplaintStatus;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ComplaintService;
import com.ecommerce.cnj70.service.OrderService;
import com.ecommerce.cnj70.util.FileUploadUtil;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Customer-facing Complaint UI — Phase 3 §12-§14.
 *
 * <p>Workflow:</p>
 * <ol>
 *   <li>Customer vào {@code /complaints} để xem các khiếu nại của mình + status.</li>
 *   <li>Customer bấm "Tạo khiếu nại" từ order history (hoặc từ menu) → {@code /complaints/new}.</li>
 *   <li>Form chọn Order (target) + product (optional sub-target) + reason + description + evidence.</li>
 *   <li>Submit → {@code POST /complaints} → service validates + saves + audit → redirect detail.</li>
 *   <li>Customer theo dõi status qua {@code /complaints/{id}} cho đến khi RESOLVED/REJECTED.</li>
 * </ol>
 *
 * <p>Service-layer (ComplaintService) đã enforce:</p>
 * <ul>
 *   <li>Customer chỉ tạo complaint cho Order của mình (Phase 3 §11).</li>
 *   <li>Reason OTHER cần mô tả (Phase 3A §10).</li>
 *   <li>orderItemIds phải nằm trong Order.items (Phase 3A §13).</li>
 *   <li>File evidence được validate qua {@link FileUploadUtil} (size, MIME, extension).</li>
 * </ul>
 */
@Slf4j
@Controller("complaintWebController")
@RequiredArgsConstructor
public class ComplaintController {

    /** Số complaint tối đa hiển thị trên list page (đơn giản — không cần pagination chi tiết). */
    private static final int LIST_PAGE_SIZE = 100;

    /** Giới hạn số file evidence mỗi complaint. */
    private static final int MAX_EVIDENCE_FILES = 6;

    private final ComplaintService complaintService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    // ============================================================
    // LIST — Customer xem danh sách complaint của mình
    // ============================================================

    /**
     * GET /complaints — Customer dashboard.
     * Hiển thị tất cả complaint của user (mới nhất trước).
     * Nếu có query param {@code status} → filter theo trạng thái.
     */
    @GetMapping("/complaints")
    public String listComplaints(@AuthenticationPrincipal CustomUserDetails user,
                                 @RequestParam(required = false) String status,
                                 Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        // Service.listMyComplaints đã enforce ownership + role check (CUSTOMER)
        Page<Complaint> page = complaintService.listMyComplaints(
                user,
                PageRequest.of(0, LIST_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<Complaint> all = page.getContent();
        List<Complaint> filtered = all;
        if (StringUtils.hasText(status)) {
            try {
                ComplaintStatus statusFilter = ComplaintStatus.valueOf(status);
                filtered = all.stream()
                        .filter(c -> c.getStatus() == statusFilter)
                        .toList();
            } catch (IllegalArgumentException ignored) {
                // status không hợp lệ → bỏ qua filter, hiển thị tất cả
            }
        }

        // Thống kê theo status (cho sidebar filter buttons)
        long openCount = all.stream().filter(c -> c.getStatus() == ComplaintStatus.OPEN).count();
        long vendorRespondedCount = all.stream()
                .filter(c -> c.getStatus() == ComplaintStatus.VENDOR_RESPONDED).count();
        long escalatedCount = all.stream().filter(c ->
                c.getStatus() == ComplaintStatus.ESCALATED
                        || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                        || c.getStatus() == ComplaintStatus.ESCALATED_L2
                        || c.getStatus() == ComplaintStatus.ADMIN_REVIEW).count();
        long resolvedCount = all.stream().filter(c -> c.getStatus() == ComplaintStatus.RESOLVED).count();
        long rejectedCount = all.stream().filter(c -> c.getStatus() == ComplaintStatus.REJECTED).count();
        long closedCount = all.stream().filter(c -> c.getStatus() == ComplaintStatus.CLOSED).count();

        model.addAttribute("complaints", filtered);
        model.addAttribute("activeStatus", status != null ? status : "ALL");
        model.addAttribute("totalCount", all.size());
        model.addAttribute("openCount", openCount);
        model.addAttribute("vendorRespondedCount", vendorRespondedCount);
        model.addAttribute("escalatedCount", escalatedCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("closedCount", closedCount);
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        return "web/complaint-list";
    }

    // ============================================================
    // CREATE FORM — Customer tạo complaint mới
    // ============================================================

    /**
     * GET /complaints/new — Hiển thị form tạo complaint.
     *
     * <p>Query params:</p>
     * <ul>
     *   <li>{@code orderId} (optional) — pre-fill Order target khi user click từ order history.</li>
     * </ul>
     *
     * <p>Nếu có {@code orderId}, load Order details để hiển thị product picker.</p>
     */
    @GetMapping("/complaints/new")
    public String createComplaintForm(@AuthenticationPrincipal CustomUserDetails user,
                                      @RequestParam(required = false) String orderId,
                                      Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        // Lấy tất cả orders của customer (mới nhất trước)
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // Filter chỉ giữ những order cho phép tạo complaint
        // (Phase 3 §27-§28: loại CANCELLED — không có gì để khiếu nại)
        List<Order> eligibleOrders = orders.stream()
                .filter(this::isOrderEligibleForComplaint)
                .toList();

        // Order được pre-fill (nếu có) — phải thuộc customer + eligible
        Order selectedOrder = null;
        if (StringUtils.hasText(orderId)) {
            Optional<Order> opt = orderRepository.findById(orderId);
            if (opt.isPresent()
                    && user.getId().equals(opt.get().getUserId())
                    && isOrderEligibleForComplaint(opt.get())) {
                selectedOrder = opt.get();
            }
        }

        model.addAttribute("orders", eligibleOrders);
        model.addAttribute("selectedOrder", selectedOrder);
        model.addAttribute("reasons", ComplaintReason.values());
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        return "web/complaint-create";
    }

    /**
     * POST /complaints — Submit form tạo complaint.
     *
     * <p>Multipart form data:</p>
     * <ul>
     *   <li>{@code orderId} — Order cần khiếu nại (bắt buộc)</li>
     *   <li>{@code productIds} — Danh sách productId cụ thể (optional, multi-select)</li>
     *   <li>{@code reason} — ComplaintReason enum</li>
     *   <li>{@code description} — Mô tả chi tiết (bắt buộc nếu reason=OTHER)</li>
     *   <li>{@code evidence} — Multi-file upload (jpg/png/pdf, ≤ 5MB mỗi file)</li>
     * </ul>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Validate input (orderId, reason).</li>
     *   <li>Upload từng file evidence qua FileUploadUtil → trả về URL.</li>
     *   <li>Gọi complaintService.createComplaint.</li>
     *   <li>Redirect sang /complaints/{id} để xem status.</li>
     * </ol>
     */
    @PostMapping("/complaints")
    public String submitComplaint(@AuthenticationPrincipal CustomUserDetails user,
                                  @RequestParam("orderId") String orderId,
                                  @RequestParam(value = "productIds", required = false) List<String> productIds,
                                  @RequestParam("reason") String reasonStr,
                                  @RequestParam(value = "description", required = false) String description,
                                  @RequestParam(value = "evidence", required = false) MultipartFile[] evidenceFiles,
                                  Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        try {
            // 1) Validate reason
            ComplaintReason reason;
            try {
                reason = ComplaintReason.valueOf(reasonStr);
            } catch (IllegalArgumentException | NullPointerException ex) {
                throw new IllegalArgumentException("Lý do khiếu nại không hợp lệ: " + reasonStr);
            }

            // 2) Build request DTO
            CreateComplaintReq req = new CreateComplaintReq();
            req.setOrderId(orderId);
            req.setReason(reason);
            req.setDescription(description);
            if (productIds != null && !productIds.isEmpty()) {
                req.setOrderItemIds(new ArrayList<>(productIds));
            }

            // 3) Upload evidence files (jpg/png/pdf, ≤5MB) trước khi tạo complaint
            if (evidenceFiles != null && evidenceFiles.length > 0) {
                if (evidenceFiles.length > MAX_EVIDENCE_FILES) {
                    throw new IllegalArgumentException(
                            "Chỉ được phép upload tối đa " + MAX_EVIDENCE_FILES
                                    + " file bằng chứng. Bạn đã chọn " + evidenceFiles.length + " file.");
                }
                List<String> evidenceUrls = new ArrayList<>();
                for (MultipartFile file : evidenceFiles) {
                    if (file == null || file.isEmpty()) continue;
                    String url = FileUploadUtil.saveFile(file);
                    evidenceUrls.add(url);
                }
                req.setEvidence(evidenceUrls);
            }

            // 4) Gọi service (đã enforce ownership + validate orderItemIds)
            Complaint saved = complaintService.createComplaint(req, user);
            log.info("Customer {} created complaint {} for order {}", user.getId(), saved.getId(), orderId);

            // 5) Redirect sang detail để xem status
            return "redirect:/complaints/" + saved.getId() + "?created=1";

        } catch (Exception e) {
            log.warn("Create complaint failed for user={} orderId={}: {}", user.getId(), orderId, e.getMessage());
            // Re-render form với error
            List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                    .stream()
                    .filter(this::isOrderEligibleForComplaint)
                    .toList();
            Order selectedOrder = null;
            try {
                if (StringUtils.hasText(orderId)) {
                    Optional<Order> opt = orderRepository.findById(orderId);
                    if (opt.isPresent()
                            && user.getId().equals(opt.get().getUserId())
                            && isOrderEligibleForComplaint(opt.get())) {
                        selectedOrder = opt.get();
                    }
                }
            } catch (Exception ignored) {
                // ignore — chỉ cần render lại form
            }

            model.addAttribute("orders", orders);
            model.addAttribute("selectedOrder", selectedOrder);
            model.addAttribute("reasons", ComplaintReason.values());
            model.addAttribute("reasonLabels", REASON_LABELS_VI);
            model.addAttribute("error", e.getMessage());
            model.addAttribute("formOrderId", orderId);
            model.addAttribute("formProductIds", productIds);
            model.addAttribute("formReason", reasonStr);
            model.addAttribute("formDescription", description);
            return "web/complaint-create";
        }
    }

    // ============================================================
    // DETAIL — Customer xem chi tiết complaint + status timeline
    // ============================================================

    /**
     * GET /complaints/{id} — Chi tiết complaint của customer.
     *
     * <p>Hiển thị:</p>
     * <ul>
     *   <li>Status hiện tại + level (0/1/2) + current handler</li>
     *   <li>Vendor response (nếu đã Level 0 responded)</li>
     *   <li>Decision + decision reason (nếu đã resolved/rejected)</li>
     *   <li>Evidence images/PDFs</li>
     *   <li>Conversation thread (mô tả ↔ vendor response ↔ moderator note ↔ admin decision)</li>
     *   <li>Timeline lịch sử (open → vendor respond → escalate → resolve)</li>
     *   <li>SLA deadline với progress bar (nếu còn trong level)</li>
     *   <li>Action buttons (escalate / resolve) — chỉ khi hợp lệ</li>
     * </ul>
     *
     * <p>Service đã enforce: customer chỉ xem được complaint của mình.</p>
     */
    @GetMapping("/complaints/{id}")
    public String complaintDetail(@AuthenticationPrincipal CustomUserDetails user,
                                  @PathVariable String id,
                                  @RequestParam(required = false) String created,
                                  @RequestParam(required = false) String flagged,
                                  Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        try {
            // Service.getById đã enforce: customer chỉ xem của mình; nếu không sẽ throw UnauthorizedException
            Complaint complaint = complaintService.getById(id, user);

            // Load thêm Order để hiển thị thông tin sản phẩm.
            // Dùng getOrderByIdForCustomer (ownership-checked) thay vì getOrderById thuần
            // để chặn IDOR: nếu customer cố tình truy cập complaintId của người khác,
            // complaintService.getById đã chặn trước đó — nhưng defense-in-depth vẫn
            // enforce ownership khi load Order để tránh bất kỳ flow nào leak.
            Order order = null;
            try {
                order = orderService.getOrderByIdForCustomer(complaint.getOrderId(), user.getId());
            } catch (Exception ignored) {
                // Order có thể đã bị xóa — bỏ qua
            }

            // Build timeline + conversation thread + current handler
            List<TimelineEvent> timeline = buildTimeline(complaint);
            List<ConversationMessage> conversation = buildConversation(complaint);
            HandlerInfo handler = determineHandler(complaint);

            // Customer actions: hiển thị buttons nào?
            boolean canEscalate = canCustomerEscalate(complaint);
            boolean canResolve = canCustomerResolve(complaint);
            boolean isOpenLevel = complaint.getStatus() != null && complaint.getStatus().isOpenLevel();
            boolean isLevel0 = complaint.getLevel() == ComplaintLevel.LEVEL_0;

            // SLA deadline (Level 0 — vendor response)
            long minutesToDeadline = -1;
            boolean deadlineExpired = false;
            Integer deadlinePercent = null;
            if (complaint.getVendorResponseDeadline() != null
                    && (isOpenLevel || complaint.getStatus() == ComplaintStatus.OPEN)) {
                long nowMs = System.currentTimeMillis();
                long due = complaint.getVendorResponseDeadline().atZone(java.time.ZoneId.systemDefault())
                        .toInstant().toEpochMilli();
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

            // Resolve reason label
            String reasonLabel = REASON_LABELS_VI.getOrDefault(complaint.getReason(),
                    complaint.getReason() != null ? complaint.getReason().name() : "");

            model.addAttribute("complaint", complaint);
            model.addAttribute("order", order);
            model.addAttribute("timeline", timeline);
            model.addAttribute("conversation", conversation);
            model.addAttribute("handler", handler);
            model.addAttribute("canEscalate", canEscalate);
            model.addAttribute("canResolve", canResolve);
            model.addAttribute("isOpenLevel", isOpenLevel);
            model.addAttribute("isLevel0", isLevel0);
            model.addAttribute("minutesToDeadline", minutesToDeadline);
            model.addAttribute("deadlineExpired", deadlineExpired);
            model.addAttribute("deadlinePercent", deadlinePercent);
            model.addAttribute("reasonLabel", reasonLabel);
            model.addAttribute("statusLabel", STATUS_LABELS_VI.getOrDefault(complaint.getStatus(),
                    complaint.getStatus() != null ? complaint.getStatus().name() : ""));
            model.addAttribute("levelLabel", LEVEL_LABELS_VI.getOrDefault(complaint.getLevel(),
                    complaint.getLevel() != null ? complaint.getLevel().name() : ""));
            model.addAttribute("statusLabels", STATUS_LABELS_VI);
            model.addAttribute("reasonLabels", REASON_LABELS_VI);
            model.addAttribute("showCreatedToast", "1".equals(created));
            model.addAttribute("showEscalatedToast", "1".equals(flagged));
            return "web/complaint-detail";
        } catch (Exception e) {
            log.warn("Load complaint detail failed for user={} id={}: {}", user.getId(), id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("complaintId", id);
            return "web/complaint-detail";
        }
    }

    // ============================================================
    // CUSTOMER ACTIONS — escalate / resolve (Level 0 only)
    // ============================================================

    /**
     * POST /complaints/{id}/escalate — Customer yêu cầu Moderator xử lý.
     *
     * <p>Điều kiện (đã được enforce trong service):</p>
     * <ul>
     *   <li>Complaint ở Level 0 (Customer ↔ Shop).</li>
     *   <li>Status chưa terminal.</li>
     *   <li>Phải là Customer của complaint.</li>
     * </ul>
     *
     * <p>Sau khi escalate thành công → redirect lại detail với flag {@code flagged=1} để show toast.</p>
     */
    @PostMapping("/complaints/{id}/escalate")
    public String escalateComplaint(@AuthenticationPrincipal CustomUserDetails user,
                                    @PathVariable String id,
                                    @RequestParam(value = "reason", required = false) String reason,
                                    Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            complaintService.escalateFromLevel0(
                    id,
                    StringUtils.hasText(reason) ? reason : "Customer yêu cầu Moderator xem xét",
                    user);
            log.info("Customer {} escalated complaint {}", user.getId(), id);
            return "redirect:/complaints/" + id + "?flagged=1";
        } catch (Exception e) {
            log.warn("Escalate failed user={} complaint={}: {}", user.getId(), id, e.getMessage());
            // Reload detail với error
            return "redirect:/complaints/" + id + "?error=" + java.net.URLEncoder.encode(
                    e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    /**
     * POST /complaints/{id}/resolve — Customer đồng ý giải quyết Level 0.
     *
     * <p>Điều kiện: complaint ở Level 0, vendor đã respond (status=VENDOR_RESPONDED).</p>
     */
    @PostMapping("/complaints/{id}/resolve")
    public String resolveComplaint(@AuthenticationPrincipal CustomUserDetails user,
                                  @PathVariable String id,
                                  @RequestParam(value = "note", required = false) String note,
                                  Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        try {
            complaintService.resolveLevel0(
                    id,
                    StringUtils.hasText(note) ? note : "Customer đồng ý giải quyết",
                    user);
            log.info("Customer {} resolved complaint {}", user.getId(), id);
            return "redirect:/complaints/" + id + "?flagged=1";
        } catch (Exception e) {
            log.warn("Resolve failed user={} complaint={}: {}", user.getId(), id, e.getMessage());
            return "redirect:/complaints/" + id + "?error=" + java.net.URLEncoder.encode(
                    e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Phase 3 §27-§28 — chỉ chấp nhận complaint cho Order chưa bị CANCELLED.
     * Với order CANCELLED → không có giao dịch thành công để khiếu nại.
     */
    private boolean isOrderEligibleForComplaint(Order order) {
        if (order == null || order.getStatus() == null) return false;
        return order.getStatus() != OrderStatus.CANCELLED;
    }

    /**
     * Build timeline cho trang detail. Trả về list các sự kiện theo thứ tự thời gian.
     */
    private List<TimelineEvent> buildTimeline(Complaint c) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent(
                "OPEN",
                "Khiếu nại đã được gửi",
                "Bạn đã gửi khiếu nại đến shop. Shop có thời hạn 48 giờ để phản hồi.",
                c.getCreatedAt(),
                "fas fa-flag",
                "completed"));

        if (c.getVendorRespondedAt() != null) {
            events.add(new TimelineEvent(
                    "VENDOR_RESPONDED",
                    "Shop đã phản hồi",
                    c.getVendorResponse() != null
                            ? truncate(c.getVendorResponse(), 200)
                            : "Shop đã gửi phản hồi cho khiếu nại của bạn.",
                    c.getVendorRespondedAt(),
                    "fas fa-comment-dots",
                    "completed"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_1 || c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.ESCALATED
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW) {
            events.add(new TimelineEvent(
                    "ESCALATED",
                    "Đã chuyển lên cấp Moderator",
                    "Khiếu nại được Moderator xem xét vì không đạt thỏa thuận với shop.",
                    c.getUpdatedAt(),
                    "fas fa-level-up-alt",
                    "completed"));
        }

        if (c.getLevel() == ComplaintLevel.LEVEL_2 || c.getStatus() == ComplaintStatus.ESCALATED_L2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
            events.add(new TimelineEvent(
                    "ADMIN_REVIEW",
                    "Đã chuyển lên cấp Admin",
                    "Khiếu nại được Admin xem xét vì Moderator không đạt thỏa thuận.",
                    c.getUpdatedAt(),
                    "fas fa-shield-alt",
                    "completed"));
        }

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.RESOLVED) {
            String role = c.getResolvedByRole() != null ? c.getResolvedByRole() : "—";
            events.add(new TimelineEvent(
                    "RESOLVED",
                    "Khiếu nại đã được giải quyết",
                    c.getDecisionReason() != null
                            ? truncate(c.getDecisionReason(), 200)
                            : (c.getDecision() != null ? truncate(c.getDecision(), 200)
                            : "Khiếu nại đã được xử lý thành công bởi " + role + "."),
                    c.getResolvedAt(),
                    "fas fa-check-circle",
                    "completed"));
        }

        if (c.getResolvedAt() != null && c.getStatus() == ComplaintStatus.REJECTED) {
            events.add(new TimelineEvent(
                    "REJECTED",
                    "Khiếu nại bị từ chối",
                    c.getDecisionReason() != null
                            ? truncate(c.getDecisionReason(), 200)
                            : "Khiếu nại đã bị từ chối bởi " + c.getResolvedByRole(),
                    c.getResolvedAt(),
                    "fas fa-times-circle",
                    "completed"));
        }

        // Sort by timestamp asc
        events.sort(Comparator.comparing(
                TimelineEvent::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    /**
     * Build conversation thread — sắp xếp messages theo thời gian.
     *
     * <p>Mỗi message có: actor role (CUSTOMER/VENDOR/MODERATOR/ADMIN/SYSTEM), actor name,
     * content, timestamp. Thread bắt đầu bằng mô tả ban đầu của Customer.</p>
     */
    private List<ConversationMessage> buildConversation(Complaint c) {
        List<ConversationMessage> msgs = new ArrayList<>();

        // 1. Initial complaint message (Customer)
        if (StringUtils.hasText(c.getDescription())) {
            msgs.add(new ConversationMessage(
                    "CUSTOMER",
                    c.getCustomerName() != null ? c.getCustomerName() : "Bạn",
                    c.getDescription(),
                    c.getCreatedAt(),
                    "fas fa-flag",
                    "Bạn đã gửi khiếu nại"));
        }

        // 2. Vendor response (nếu có)
        if (StringUtils.hasText(c.getVendorResponse())) {
            String vendorName = c.getShopName() != null ? c.getShopName() : "Shop";
            msgs.add(new ConversationMessage(
                    "VENDOR",
                    vendorName,
                    c.getVendorResponse(),
                    c.getVendorRespondedAt() != null ? c.getVendorRespondedAt() : c.getUpdatedAt(),
                    "fas fa-store",
                    "Shop đã phản hồi"));
        }

        // 3. Moderator action (Level 1)
        if (c.getLevel() == ComplaintLevel.LEVEL_1 || c.getLevel() == ComplaintLevel.LEVEL_2
                || c.getStatus() == ComplaintStatus.ESCALATED
                || c.getStatus() == ComplaintStatus.MODERATOR_REVIEW
                || (c.getResolvedByRole() != null && "MODERATOR".equals(c.getResolvedByRole()))) {
            String moderatorName = c.getAssignedModeratorEmail() != null
                    ? c.getAssignedModeratorEmail() : "Moderator";
            String body;
            if (c.getStatus() == ComplaintStatus.MODERATOR_REVIEW) {
                body = "Moderator đang xem xét khiếu nại của bạn. Bạn sẽ được thông báo khi có cập nhật.";
            } else if (c.getStatus() == ComplaintStatus.RESOLVED && "MODERATOR".equals(c.getResolvedByRole())) {
                body = buildModeratorDecisionBody(c);
            } else if (c.getStatus() == ComplaintStatus.REJECTED && "MODERATOR".equals(c.getResolvedByRole())) {
                body = buildModeratorRejectionBody(c);
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

        // 4. Admin action (Level 2) — chỉ khi level=2 hoặc resolved bởi ADMIN
        if (c.getLevel() == ComplaintLevel.LEVEL_2 || c.getStatus() == ComplaintStatus.ESCALATED_L2
                || c.getStatus() == ComplaintStatus.ADMIN_REVIEW
                || (c.getResolvedByRole() != null && "ADMIN".equals(c.getResolvedByRole()))) {
            String adminName = c.getAssignedAdminEmail() != null
                    ? c.getAssignedAdminEmail() : "Admin";
            String body;
            if (c.getStatus() == ComplaintStatus.ADMIN_REVIEW) {
                body = "Admin đang xem xét khiếu nại của bạn — đây là cấp xử lý cuối cùng.";
            } else if (c.getResolvedByRole() != null && "ADMIN".equals(c.getResolvedByRole())
                    && c.getStatus() == ComplaintStatus.RESOLVED) {
                body = buildAdminDecisionBody(c);
            } else if (c.getResolvedByRole() != null && "ADMIN".equals(c.getResolvedByRole())
                    && c.getStatus() == ComplaintStatus.REJECTED) {
                body = buildAdminRejectionBody(c);
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

        // Sort by timestamp asc
        msgs.sort(Comparator.comparing(
                ConversationMessage::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return msgs;
    }

    private String buildModeratorDecisionBody(Complaint c) {
        if (StringUtils.hasText(c.getDecisionReason())) return c.getDecisionReason();
        if (StringUtils.hasText(c.getDecision())) return c.getDecision();
        return "Moderator đã giải quyết khiếu nại theo hướng có lợi cho bạn.";
    }

    private String buildModeratorRejectionBody(Complaint c) {
        if (StringUtils.hasText(c.getDecisionReason())) return c.getDecisionReason();
        return "Moderator đã xem xét và từ chối khiếu nại sau khi cân nhắc toàn bộ bằng chứng.";
    }

    private String buildAdminDecisionBody(Complaint c) {
        if (StringUtils.hasText(c.getDecisionReason())) return c.getDecisionReason();
        if (StringUtils.hasText(c.getDecision())) return c.getDecision();
        return "Admin đã ra phán quyết cuối cùng theo hướng có lợi cho bạn.";
    }

    private String buildAdminRejectionBody(Complaint c) {
        if (StringUtils.hasText(c.getDecisionReason())) return c.getDecisionReason();
        return "Admin đã giữ nguyên quyết định của Moderator. Khiếu nại kết thúc ở cấp Cấp 2.";
    }

    /**
     * Xác định "hiện tại ai đang xử lý" complaint → render badge nổi bật.
     */
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
        // Default — Level 0: handled by Vendor
        String name = c.getShopName() != null ? c.getShopName() : "Shop";
        if (c.getStatus() == ComplaintStatus.VENDOR_RESPONDED) {
            return new HandlerInfo("VENDOR", name + " (đã phản hồi)", "fas fa-comment-dots");
        }
        return new HandlerInfo("VENDOR", name + " (chờ phản hồi)", "fas fa-store");
    }

    /**
     * Customer có thể escalate (gửi lên Moderator) khi:
     * <ul>
     *   <li>Complaint ở Level 0</li>
     *   <li>Status chưa terminal (chưa RESOLVED/REJECTED/CLOSED)</li>
     *   <li>Shop đã phản hồi (VENDOR_RESPONDED) — escalation sau khi shop có cơ hội trả lời</li>
     * </ul>
     */
    private boolean canCustomerEscalate(Complaint c) {
        if (c == null) return false;
        if (c.getLevel() != ComplaintLevel.LEVEL_0) return false;
        if (c.getStatus() == null || c.getStatus().isTerminal()) return false;
        return c.getStatus() == ComplaintStatus.VENDOR_RESPONDED;
    }

    /**
     * Customer có thể "đồng ý giải quyết" Level 0 khi:
     * <ul>
     *   <li>Level 0</li>
     *   <li>Status = VENDOR_RESPONDED (shop đã phản hồi)</li>
     * </ul>
     */
    private boolean canCustomerResolve(Complaint c) {
        if (c == null) return false;
        if (c.getLevel() != ComplaintLevel.LEVEL_0) return false;
        return c.getStatus() == ComplaintStatus.VENDOR_RESPONDED;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    // ============================================================
    // CONSTANTS — Vietnamese labels
    // ============================================================

    private static final Map<ComplaintReason, String> REASON_LABELS_VI = new HashMap<>();
    private static final Map<ComplaintStatus, String> STATUS_LABELS_VI = new HashMap<>();
    private static final Map<ComplaintLevel, String> LEVEL_LABELS_VI = new HashMap<>();

    static {
        REASON_LABELS_VI.put(ComplaintReason.NON_DELIVERY, "Không giao hàng");
        REASON_LABELS_VI.put(ComplaintReason.WRONG_PRODUCT, "Sai sản phẩm");
        REASON_LABELS_VI.put(ComplaintReason.MISSING_ITEM, "Thiếu sản phẩm");
        REASON_LABELS_VI.put(ComplaintReason.DAMAGED_PRODUCT, "Sản phẩm hư hỏng");
        REASON_LABELS_VI.put(ComplaintReason.NOT_AS_DESCRIBED, "Không đúng mô tả");
        REASON_LABELS_VI.put(ComplaintReason.WARRANTY_ISSUE, "Vấn đề bảo hành");
        REASON_LABELS_VI.put(ComplaintReason.OTHER, "Khác");

        STATUS_LABELS_VI.put(ComplaintStatus.OPEN, "Đang chờ shop phản hồi");
        STATUS_LABELS_VI.put(ComplaintStatus.VENDOR_RESPONDED, "Shop đã phản hồi");
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED, "Đã chuyển lên Moderator");
        STATUS_LABELS_VI.put(ComplaintStatus.MODERATOR_REVIEW, "Moderator đang xem xét");
        STATUS_LABELS_VI.put(ComplaintStatus.ESCALATED_L2, "Đã chuyển lên Admin");
        STATUS_LABELS_VI.put(ComplaintStatus.ADMIN_REVIEW, "Admin đang xem xét");
        STATUS_LABELS_VI.put(ComplaintStatus.RESOLVED, "Đã giải quyết");
        STATUS_LABELS_VI.put(ComplaintStatus.CLOSED, "Đã đóng");
        STATUS_LABELS_VI.put(ComplaintStatus.REJECTED, "Bị từ chối");

        LEVEL_LABELS_VI.put(ComplaintLevel.LEVEL_0, "Cấp 0 — Customer ↔ Shop");
        LEVEL_LABELS_VI.put(ComplaintLevel.LEVEL_1, "Cấp 1 — Moderator");
        LEVEL_LABELS_VI.put(ComplaintLevel.LEVEL_2, "Cấp 2 — Admin");
    }

    /**
     * Timeline event DTO for complaint detail page.
     */
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

    /**
     * Conversation message — mỗi phát biểu trong thread giữa Customer ↔ Vendor ↔ Moderator ↔ Admin.
     */
    public static class ConversationMessage {
        private final String actorRole; // CUSTOMER / VENDOR / MODERATOR / ADMIN
        private final String actorName;
        private final String content;
        private final LocalDateTime timestamp;
        private final String icon;
        private final String actorLabel; // "Bạn", "Shop", "Moderator", "Admin"

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

    /**
     * Thông tin về "ai đang xử lý" complaint hiện tại — Customer xem thấy cái này
     * để biết case của mình đang ở đâu trong pipeline.
     */
    public static class HandlerInfo {
        private final String role;       // VENDOR / MODERATOR / ADMIN / RESOLVED / CLOSED
        private final String displayName;
        private final String iconClass;  // FontAwesome icon class

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
