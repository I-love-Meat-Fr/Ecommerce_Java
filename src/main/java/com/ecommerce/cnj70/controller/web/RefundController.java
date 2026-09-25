package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.RefundRequest;
import com.ecommerce.cnj70.document.ReturnRequest;
import com.ecommerce.cnj70.enums.RefundStatus;
import com.ecommerce.cnj70.enums.ReturnStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.RefundService;
import com.ecommerce.cnj70.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Customer-facing Refund/Return UI — Phase 3A §23-§34.
 *
 * <p>Workflow:</p>
 * <ol>
 *   <li>Customer vào {@code /refunds} để xem các yêu cầu hoàn tiền/trả hàng của mình.</li>
 *   <li>Customer xem chi tiết từng yêu cầu tại {@code /refunds/{id}} (cho refund) hoặc {@code /returns/{id}} (cho return).</li>
 *   <li>Customer thấy trạng thái và kết quả xử lý (Done: Customer thấy request status và kết quả).</li>
 * </ol>
 *
 * <p>Rule: Customer chỉ thấy refund/return của mình (ownership-enforced bởi service layer).</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class RefundController {

    private static final int LIST_PAGE_SIZE = 50;

    private final RefundService refundService;
    private final ReturnService returnService;

    // ============================================================
    // LIST — Customer xem danh sách refund + return của mình
    // ============================================================

    /**
     * GET /refunds — Customer refund/return dashboard.
     * Hiển thị tất cả refund request và return request của user (mới nhất trước).
     * Nếu có query param {@code type} → filter theo loại (REFUND / RETURN).
     * Nếu có query param {@code status} → filter theo trạng thái.
     */
    @GetMapping("/refunds")
    public String listRefunds(@AuthenticationPrincipal CustomUserDetails user,
                              @RequestParam(required = false) String type,
                              @RequestParam(required = false) String status,
                              Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        // Lấy refunds
        Page<RefundRequest> refundPage = refundService.listMyRefunds(
                user,
                PageRequest.of(0, LIST_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<RefundRequest> refunds = refundPage.getContent();

        // Lấy returns
        Page<ReturnRequest> returnPage = returnService.listMyReturns(
                user,
                PageRequest.of(0, LIST_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<ReturnRequest> returns = returnPage.getContent();

        // Thống kê
        long refundCount = refunds.size();
        long returnCount = returns.size();
        long pendingCount = refunds.stream()
                .filter(r -> r.getStatus() == RefundStatus.REQUESTED || r.getStatus() == RefundStatus.PROCESSING)
                .count();
        long successCount = refunds.stream()
                .filter(r -> r.getStatus() == RefundStatus.SUCCEEDED)
                .count();
        long failedCount = refunds.stream()
                .filter(r -> r.getStatus() == RefundStatus.FAILED)
                .count();

        // Filter theo type — dùng typed local variable cho rõ ràng.
        List<RefundRequest> visibleRefunds = refunds;
        List<ReturnRequest> visibleReturns = returns;
        if ("REFUND".equalsIgnoreCase(type)) {
            visibleReturns = List.of();
        } else if ("RETURN".equalsIgnoreCase(type)) {
            visibleRefunds = List.of();
        }

        // Filter theo status (chỉ áp dụng cho refund list)
        if (status != null && !status.isBlank()) {
            try {
                RefundStatus refundStatus = RefundStatus.valueOf(status.toUpperCase());
                final RefundStatus finalStatus = refundStatus;
                visibleRefunds = visibleRefunds.stream()
                        .filter(r -> r.getStatus() == finalStatus)
                        .toList();
            } catch (IllegalArgumentException ignored) {
                // Không hợp lệ → bỏ qua
            }
        }

        model.addAttribute("refunds", visibleRefunds);
        model.addAttribute("returns", visibleReturns);
        model.addAttribute("activeType", type != null ? type.toUpperCase() : "ALL");
        model.addAttribute("activeStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("totalRefunds", refundCount);
        model.addAttribute("totalReturns", returnCount);
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("successCount", successCount);
        model.addAttribute("failedCount", failedCount);
        model.addAttribute("refundStatusLabels", REFUND_STATUS_LABELS_VI);
        model.addAttribute("returnStatusLabels", RETURN_STATUS_LABELS_VI);
        return "web/refund-list";
    }

    // ============================================================
    // REFUND DETAIL — Customer xem chi tiết refund request
    // ============================================================

    /**
     * GET /refunds/{id} — Chi tiết refund request của customer.
     *
     * <p>Hiển thị:</p>
     * <ul>
     *   <li>Trạng thái refund (REQUESTED → PROCESSING → SUCCEEDED / FAILED)</li>
     *   <li>Số tiền yêu cầu và số tiền thực nhận (nếu đã settle)</li>
     *   <li>Provider transaction ID (nếu có)</li>
     *   <li>Thông tin order liên quan</li>
     *   <li>Timeline trạng thái</li>
     *   <li>Kết quả xử lý (SUCCEEDED/FAILED message)</li>
     * </ul>
     */
    @GetMapping("/refunds/{id}")
    public String refundDetail(@AuthenticationPrincipal CustomUserDetails user,
                               @PathVariable String id,
                               @RequestParam(required = false) String error,
                               Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        try {
            RefundRequest refund = refundService.getById(id, user);

            // Build timeline
            List<RefundTimelineEvent> timeline = buildRefundTimeline(refund);

            model.addAttribute("refund", refund);
            model.addAttribute("timeline", timeline);
            model.addAttribute("refundStatusLabels", REFUND_STATUS_LABELS_VI);
            model.addAttribute("statusLabel", REFUND_STATUS_LABELS_VI.getOrDefault(refund.getStatus(),
                    refund.getStatus() != null ? refund.getStatus().name() : ""));
            return "web/refund-detail";
        } catch (Exception e) {
            log.warn("Load refund detail failed for user={} id={}: {}", user.getId(), id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("refundId", id);
            return "web/refund-detail";
        }
    }

    // ============================================================
    // RETURN DETAIL — Customer xem chi tiết return request
    // ============================================================

    /**
     * GET /returns/{id} — Chi tiết return request của customer.
     *
     * <p>Hiển thị:</p>
     * <ul>
     *   <li>Trạng thái return (REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED)</li>
     *   <li>Lý do return</li>
     *   <li>Số tiền hoàn tiền dự kiến (declaredAmount)</li>
     *   <li>Refund ID liên quan (nếu đã trigger)</li>
     *   <li>Timeline trạng thái</li>
     * </ul>
     */
    @GetMapping("/returns/{id}")
    public String returnDetail(@AuthenticationPrincipal CustomUserDetails user,
                               @PathVariable String id,
                               @RequestParam(required = false) String error,
                               Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        try {
            ReturnRequest returnReq = returnService.getById(id, user);

            // Build timeline
            List<ReturnTimelineEvent> timeline = buildReturnTimeline(returnReq);

            model.addAttribute("returnReq", returnReq);
            model.addAttribute("timeline", timeline);
            model.addAttribute("returnStatusLabels", RETURN_STATUS_LABELS_VI);
            model.addAttribute("statusLabel", RETURN_STATUS_LABELS_VI.getOrDefault(returnReq.getStatus(),
                    returnReq.getStatus() != null ? returnReq.getStatus().name() : ""));
            return "web/return-detail";
        } catch (Exception e) {
            log.warn("Load return detail failed for user={} id={}: {}", user.getId(), id, e.getMessage());
            model.addAttribute("error", e.getMessage());
            model.addAttribute("returnId", id);
            return "web/return-detail";
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private List<RefundTimelineEvent> buildRefundTimeline(RefundRequest r) {
        java.util.ArrayList<RefundTimelineEvent> events = new java.util.ArrayList<>();

        // REQUESTED
        events.add(new RefundTimelineEvent(
                "REQUESTED",
                "Yêu cầu hoàn tiền đã gửi",
                "Yêu cầu hoàn tiền của bạn đã được tiếp nhận và đang chờ xử lý.",
                r.getCreatedAt(),
                "fas fa-paper-plane",
                r.getStatus() != RefundStatus.REQUESTED));

        // PROCESSING
        if (r.getStatus() == RefundStatus.PROCESSING
                || r.getStatus() == RefundStatus.SUCCEEDED
                || r.getStatus() == RefundStatus.FAILED) {
            events.add(new RefundTimelineEvent(
                    "PROCESSING",
                    "Đang xử lý thanh toán",
                    "Đơn vị thanh toán đang xử lý yêu cầu hoàn tiền của bạn.",
                    r.getUpdatedAt(),
                    "fas fa-sync-alt",
                    r.getStatus() == RefundStatus.SUCCEEDED || r.getStatus() == RefundStatus.FAILED));
        }

        // SUCCEEDED
        if (r.getStatus() == RefundStatus.SUCCEEDED && r.getSettledAt() != null) {
            String amountStr = r.getSettledAmount() != null
                    ? r.getSettledAmount().toString() + " đ"
                    : r.getDeclaredAmount() + " đ";
            events.add(new RefundTimelineEvent(
                    "SUCCEEDED",
                    "Hoàn tiền thành công",
                    "Tiền đã được hoàn vào tài khoản của bạn. Số tiền: " + amountStr,
                    r.getSettledAt(),
                    "fas fa-check-circle",
                    true));
        }

        // FAILED
        if (r.getStatus() == RefundStatus.FAILED) {
            String reason = r.getProviderMessage() != null
                    ? r.getProviderMessage()
                    : "Đơn vị thanh toán từ chối xử lý yêu cầu.";
            events.add(new RefundTimelineEvent(
                    "FAILED",
                    "Hoàn tiền thất bại",
                    reason,
                    r.getUpdatedAt(),
                    "fas fa-times-circle",
                    true));
        }

        // CANCELLED
        if (r.getStatus() == RefundStatus.CANCELLED) {
            events.add(new RefundTimelineEvent(
                    "CANCELLED",
                    "Yêu cầu đã bị hủy",
                    "Yêu cầu hoàn tiền đã bị hủy bỏ.",
                    r.getUpdatedAt(),
                    "fas fa-ban",
                    true));
        }

        return events;
    }

    private List<ReturnTimelineEvent> buildReturnTimeline(ReturnRequest r) {
        java.util.ArrayList<ReturnTimelineEvent> events = new java.util.ArrayList<>();

        // REQUESTED
        events.add(new ReturnTimelineEvent(
                "REQUESTED",
                "Yêu cầu trả hàng đã gửi",
                "Yêu cầu trả hàng của bạn đã được tiếp nhận và đang chờ xử lý.",
                r.getCreatedAt(),
                "fas fa-undo",
                r.getStatus() != ReturnStatus.REQUESTED));

        // APPROVED
        if (r.getStatus() == ReturnStatus.APPROVED
                || r.getStatus() == ReturnStatus.IN_TRANSIT
                || r.getStatus() == ReturnStatus.RECEIVED
                || r.getStatus() == ReturnStatus.COMPLETED) {
            events.add(new ReturnTimelineEvent(
                    "APPROVED",
                    "Yêu cầu được chấp nhận",
                    "Shop đã chấp nhận yêu cầu trả hàng. Vui lòng gửi hàng về cho shop.",
                    r.getUpdatedAt(),
                    "fas fa-check",
                    r.getStatus() != ReturnStatus.APPROVED));
        }

        // IN_TRANSIT
        if (r.getStatus() == ReturnStatus.IN_TRANSIT
                || r.getStatus() == ReturnStatus.RECEIVED
                || r.getStatus() == ReturnStatus.COMPLETED) {
            events.add(new ReturnTimelineEvent(
                    "IN_TRANSIT",
                    "Hàng đang được gửi về",
                    "Hàng của bạn đang được vận chuyển về shop.",
                    r.getUpdatedAt(),
                    "fas fa-truck",
                    r.getStatus() != ReturnStatus.IN_TRANSIT));
        }

        // RECEIVED
        if (r.getStatus() == ReturnStatus.RECEIVED || r.getStatus() == ReturnStatus.COMPLETED) {
            events.add(new ReturnTimelineEvent(
                    "RECEIVED",
                    "Shop đã nhận hàng",
                    "Shop đã xác nhận nhận lại hàng. Tiền hoàn sẽ được xử lý.",
                    r.getUpdatedAt(),
                    "fas fa-box-open",
                    r.getStatus() != ReturnStatus.RECEIVED));
        }

        // COMPLETED
        if (r.getStatus() == ReturnStatus.COMPLETED) {
            String amountStr = r.getDeclaredAmount() != null
                    ? r.getDeclaredAmount().toString() + " đ"
                    : "đang chờ xử lý";
            events.add(new ReturnTimelineEvent(
                    "COMPLETED",
                    "Trả hàng hoàn tất",
                    "Quy trình trả hàng đã hoàn tất. Refund: " + amountStr,
                    r.getUpdatedAt(),
                    "fas fa-flag-checkered",
                    true));
        }

        // REJECTED
        if (r.getStatus() == ReturnStatus.REJECTED) {
            events.add(new ReturnTimelineEvent(
                    "REJECTED",
                    "Yêu cầu bị từ chối",
                    "Yêu cầu trả hàng đã bị từ chối. Vui lòng liên hệ shop để biết thêm chi tiết.",
                    r.getUpdatedAt(),
                    "fas fa-times-circle",
                    true));
        }

        // CANCELLED
        if (r.getStatus() == ReturnStatus.CANCELLED) {
            events.add(new ReturnTimelineEvent(
                    "CANCELLED",
                    "Yêu cầu đã bị hủy",
                    "Yêu cầu trả hàng đã bị hủy bỏ.",
                    r.getUpdatedAt(),
                    "fas fa-ban",
                    true));
        }

        return events;
    }

    // ============================================================
    // CONSTANTS — Vietnamese labels
    // ============================================================

    private static final Map<RefundStatus, String> REFUND_STATUS_LABELS_VI = new HashMap<>();
    private static final Map<ReturnStatus, String> RETURN_STATUS_LABELS_VI = new HashMap<>();

    static {
        // RefundStatus labels
        REFUND_STATUS_LABELS_VI.put(RefundStatus.REQUESTED, "Đang chờ xử lý");
        REFUND_STATUS_LABELS_VI.put(RefundStatus.PROCESSING, "Đang xử lý thanh toán");
        REFUND_STATUS_LABELS_VI.put(RefundStatus.SUCCEEDED, "Hoàn tiền thành công");
        REFUND_STATUS_LABELS_VI.put(RefundStatus.FAILED, "Hoàn tiền thất bại");
        REFUND_STATUS_LABELS_VI.put(RefundStatus.CANCELLED, "Đã hủy");

        // ReturnStatus labels
        RETURN_STATUS_LABELS_VI.put(ReturnStatus.REQUESTED, "Đang chờ xử lý");
        RETURN_STATUS_LABELS_VI.put(ReturnStatus.APPROVED, "Đã chấp nhận");
        RETURN_STATUS_LABELS_VI.put(ReturnStatus.IN_TRANSIT, "Hàng đang gửi về");
        RETURN_STATUS_LABELS_VI.put(ReturnStatus.RECEIVED, "Shop đã nhận hàng");
        RETURN_STATUS_LABELS_VI.put(ReturnStatus.REJECTED, "Bị từ chối");
        RETURN_STATUS_LABELS_VI.put(ReturnStatus.COMPLETED, "Hoàn tất");
        RETURN_STATUS_LABELS_VI.put(ReturnStatus.CANCELLED, "Đã hủy");
    }

    // ============================================================
    // INNER CLASSES — Timeline DTOs
    // ============================================================

    public static class RefundTimelineEvent {
        private final String key;
        private final String title;
        private final String description;
        private final java.time.LocalDateTime timestamp;
        private final String icon;
        private final boolean completed;

        public RefundTimelineEvent(String key, String title, String description,
                                  java.time.LocalDateTime timestamp, String icon, boolean completed) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.timestamp = timestamp;
            this.icon = icon;
            this.completed = completed;
        }

        public String getKey() { return key; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public String getIcon() { return icon; }
        public boolean isCompleted() { return completed; }
    }

    public static class ReturnTimelineEvent {
        private final String key;
        private final String title;
        private final String description;
        private final java.time.LocalDateTime timestamp;
        private final String icon;
        private final boolean completed;

        public ReturnTimelineEvent(String key, String title, String description,
                                  java.time.LocalDateTime timestamp, String icon, boolean completed) {
            this.key = key;
            this.title = title;
            this.description = description;
            this.timestamp = timestamp;
            this.icon = icon;
            this.completed = completed;
        }

        public String getKey() { return key; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public java.time.LocalDateTime getTimestamp() { return timestamp; }
        public String getIcon() { return icon; }
        public boolean isCompleted() { return completed; }
    }
}
