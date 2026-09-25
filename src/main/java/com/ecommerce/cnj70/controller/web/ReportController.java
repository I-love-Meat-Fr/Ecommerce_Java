package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.dto.request.ReportCaseCreateReq;
import com.ecommerce.cnj70.dto.response.ReportCaseRes;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportReason;
import com.ecommerce.cnj70.enums.ReportTargetType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ReportCaseRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.ReportCaseService;
import com.ecommerce.cnj70.service.ReviewService;
import com.ecommerce.cnj70.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Review UI — Customer-facing Report flow.
 *
 * <p>Cho phép Customer gửi report Review hoặc Product kèm bằng chứng.
 * Sau khi gửi, ReportCase được tạo (idempotent — trùng PENDING sẽ trả về case cũ),
 * Customer nhận được mã case + trạng thái theo {@link ReportCaseStatus}.</p>
 *
 * <p>Workflow:</p>
 * <ol>
 *   <li>Customer click "Báo cáo" trên Review/Product → {@code /reports/new?targetType=...&targetId=...}.</li>
 *   <li>Chọn lý do + mô tả + upload bằng chứng (ảnh/PDF, ≤ 5MB).</li>
 *   <li>Submit → {@code POST /reports} → service tạo ReportCase (PENDING).</li>
 *   <li>Redirect sang {@code /reports/{id}?created=1} để xem mã case + trạng thái.</li>
 *   <li>Theo dõi qua {@code /reports} (list) hoặc {@code /reports/{id}} (detail).</li>
 * </ol>
 *
 * <p>Quy tắc:</p>
 * <ul>
 *   <li>Service đã validate target tồn tại (Product/Review) — controller chỉ cần load
 *       snapshot để hiển thị trước form.</li>
 *   <li>File evidence dùng {@link FileUploadUtil} — validate MIME, size, extension.</li>
 *   <li>Idempotent: báo cáo trùng target khi đang PENDING sẽ trả về case cũ.</li>
 *   <li>Status hiển thị theo {@link ReportCaseStatus} enum: PENDING / OPEN / IN_REVIEW /
 *       RESOLVED / APPROVED / REJECTED / ESCALATED.</li>
 * </ul>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/reports")
public class ReportController {

    /** Số report tối đa trên list page. */
    private static final int LIST_PAGE_SIZE = 100;

    /** Số file evidence tối đa cho mỗi report (giống Complaint). */
    private static final int MAX_EVIDENCE_FILES = 6;

    private final ReportCaseService reportCaseService;
    private final ReportCaseRepository reportCaseRepository;
    private final ProductService productService;
    private final ReviewService reviewService;

    // ============================================================
    // LIST — Customer xem danh sách report của mình
    // ============================================================

    /**
     * GET /reports — Customer dashboard.
     * Hiển thị tất cả ReportCase mà Customer đã gửi (mới nhất trước).
     * Filter theo {@code status} (PENDING / RESOLVED / ESCALATED / v.v.).
     */
    @GetMapping
    public String listReports(@AuthenticationPrincipal CustomUserDetails user,
                              @RequestParam(required = false) String status,
                              @RequestParam(required = false) String targetType,
                              Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        // Lấy tất cả report của customer (sắp xếp theo createdAt DESC từ repo).
        List<ReportCase> all = reportCaseRepository.findByReporterIdOrderByCreatedAtDesc(user.getId());

        // Filter theo status (optional)
        List<ReportCase> filtered = all;
        if (StringUtils.hasText(status)) {
            try {
                ReportCaseStatus statusFilter = ReportCaseStatus.valueOf(status);
                filtered = all.stream()
                        .filter(c -> c.getStatus() == statusFilter)
                        .toList();
            } catch (IllegalArgumentException ignored) {
                // status không hợp lệ → bỏ filter
            }
        }

        // Filter theo targetType (optional)
        if (StringUtils.hasText(targetType)) {
            try {
                ReportTargetType typeFilter = ReportTargetType.valueOf(targetType);
                filtered = filtered.stream()
                        .filter(c -> c.getTargetType() == typeFilter)
                        .toList();
            } catch (IllegalArgumentException ignored) {
                // ignore
            }
        }

        // Thống kê theo status
        long pendingCount = all.stream().filter(c -> c.getStatus() == ReportCaseStatus.PENDING).count();
        long inReviewCount = all.stream().filter(c ->
                c.getStatus() == ReportCaseStatus.OPEN
                        || c.getStatus() == ReportCaseStatus.IN_REVIEW).count();
        long resolvedCount = all.stream().filter(c -> c.getStatus() == ReportCaseStatus.RESOLVED
                || c.getStatus() == ReportCaseStatus.APPROVED).count();
        long rejectedCount = all.stream().filter(c -> c.getStatus() == ReportCaseStatus.REJECTED).count();
        long escalatedCount = all.stream().filter(c -> c.getStatus() == ReportCaseStatus.ESCALATED).count();

        model.addAttribute("reports", filtered);
        model.addAttribute("totalCount", all.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("inReviewCount", inReviewCount);
        model.addAttribute("resolvedCount", resolvedCount);
        model.addAttribute("rejectedCount", rejectedCount);
        model.addAttribute("escalatedCount", escalatedCount);
        model.addAttribute("activeStatus", StringUtils.hasText(status) ? status : "ALL");
        model.addAttribute("activeTargetType", StringUtils.hasText(targetType) ? targetType : "ALL");
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("targetTypeLabels", TARGET_TYPE_LABELS_VI);
        return "web/report-list";
    }

    // ============================================================
    // CREATE FORM — Customer tạo report mới
    // ============================================================

    /**
     * GET /reports/new — Form tạo Report.
     *
     * <p>Query params bắt buộc:</p>
     * <ul>
     *   <li>{@code targetType} — {@code REVIEW} hoặc {@code PRODUCT}</li>
     *   <li>{@code targetId} — Mongo _id của Review/Product</li>
     * </ul>
     *
     * <p>Nếu không truyền query params → hiển thị form chọn target.</p>
     */
    @GetMapping("/new")
    public String createReportForm(@AuthenticationPrincipal CustomUserDetails user,
                                   @RequestParam(required = false) String targetType,
                                   @RequestParam(required = false) String targetId,
                                   Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        // Snapshot target để hiển thị trên form
        Object targetSnapshot = null;
        ReportTargetType targetTypeEnum = null;
        ReportCaseResourceType resourceType = null;

        if (StringUtils.hasText(targetType) && StringUtils.hasText(targetId)) {
            try {
                targetTypeEnum = ReportTargetType.valueOf(targetType);
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Loại đối tượng không hợp lệ: " + targetType);
            }

            switch (targetTypeEnum) {
                case REVIEW -> {
                    Review review = reviewService.getReviewById(targetId);
                    targetSnapshot = review;
                    resourceType = ReportCaseResourceType.REVIEW;
                }
                case PRODUCT -> {
                    Product product = productService.getProductById(targetId);
                    targetSnapshot = product;
                    resourceType = ReportCaseResourceType.PRODUCT;
                }
                default -> throw new BadRequestException(
                        "Chỉ hỗ trợ report REVIEW hoặc PRODUCT từ Customer.");
            }
        }

        model.addAttribute("targetSnapshot", targetSnapshot);
        model.addAttribute("targetType", targetTypeEnum);
        model.addAttribute("targetId", targetId);
        model.addAttribute("resourceType", resourceType);
        model.addAttribute("reasons", ReportReason.values());
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("targetTypeLabels", TARGET_TYPE_LABELS_VI);
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        return "web/report-create";
    }

    /**
     * POST /reports — Submit form tạo Report.
     *
     * <p>Multipart form data:</p>
     * <ul>
     *   <li>{@code targetType} (bắt buộc) — REVIEW / PRODUCT</li>
     *   <li>{@code targetId} (bắt buộc) — id của Review/Product</li>
     *   <li>{@code reason} (bắt buộc) — ReportReason enum</li>
     *   <li>{@code description} — mô tả chi tiết</li>
     *   <li>{@code evidence} — multi-file (jpg/png/pdf, ≤ 5MB)</li>
     * </ul>
     *
     * <p>Flow:</p>
     * <ol>
     *   <li>Validate input.</li>
     *   <li>Upload evidence qua {@link FileUploadUtil} → trả URL.</li>
     *   <li>Gọi {@link ReportCaseService#createCase}.</li>
     *   <li>Redirect sang {@code /reports/{id}?created=1}.</li>
     * </ol>
     */
    @PostMapping
    public String submitReport(@AuthenticationPrincipal CustomUserDetails user,
                               @RequestParam("targetType") String targetTypeStr,
                               @RequestParam("targetId") String targetId,
                               @RequestParam("reason") String reasonStr,
                               @RequestParam(value = "description", required = false) String description,
                               @RequestParam(value = "evidence", required = false) MultipartFile[] evidenceFiles,
                               Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        ReportTargetType targetType;
        ReportReason reason;
        try {
            targetType = ReportTargetType.valueOf(targetTypeStr);
        } catch (IllegalArgumentException ex) {
            return renderCreateFormWithError(user, targetTypeStr, targetId, reasonStr, description,
                    "Loại đối tượng không hợp lệ: " + targetTypeStr, model);
        }
        if (targetType != ReportTargetType.REVIEW && targetType != ReportTargetType.PRODUCT) {
            return renderCreateFormWithError(user, targetTypeStr, targetId, reasonStr, description,
                    "Chỉ hỗ trợ report REVIEW hoặc PRODUCT.", model);
        }

        try {
            reason = ReportReason.valueOf(reasonStr);
        } catch (IllegalArgumentException ex) {
            return renderCreateFormWithError(user, targetTypeStr, targetId, reasonStr, description,
                    "Lý do báo cáo không hợp lệ: " + reasonStr, model);
        }

        // Upload evidence files (≤6 file, ≤5MB/file)
        List<String> evidenceUrls = new ArrayList<>();
        if (evidenceFiles != null && evidenceFiles.length > 0) {
            if (evidenceFiles.length > MAX_EVIDENCE_FILES) {
                return renderCreateFormWithError(user, targetTypeStr, targetId, reasonStr, description,
                        "Chỉ được upload tối đa " + MAX_EVIDENCE_FILES
                                + " file bằng chứng. Bạn đã chọn " + evidenceFiles.length + " file.",
                        model);
            }
            for (MultipartFile file : evidenceFiles) {
                if (file == null || file.isEmpty()) continue;
                try {
                    String url = FileUploadUtil.saveFile(file);
                    evidenceUrls.add(url);
                } catch (Exception ex) {
                    log.warn("Upload evidence failed: {}", ex.getMessage());
                    return renderCreateFormWithError(user, targetTypeStr, targetId, reasonStr, description,
                            "Upload bằng chứng thất bại: " + ex.getMessage(), model);
                }
            }
        }

        // Build request DTO
        ReportCaseCreateReq req = ReportCaseCreateReq.builder()
                .targetType(targetType)
                .targetId(targetId)
                .reason(reason.name())
                .description(description)
                .evidence(evidenceUrls)
                .source("USER")
                .build();

        try {
            ReportCaseRes saved = reportCaseService.createCase(
                    req,
                    user.getId(),
                    user.getUsername() != null ? user.getUsername() : user.getFullName());
            log.info("Customer {} created report case {} for {} {}", user.getId(), saved.getId(), targetType, targetId);
            return "redirect:/reports/" + saved.getId() + "?created=1";
        } catch (Exception e) {
            log.warn("Create report failed for user={} target={} {}: {}",
                    user.getId(), targetType, targetId, e.getMessage());
            return renderCreateFormWithError(user, targetTypeStr, targetId, reasonStr, description,
                    e.getMessage(), model);
        }
    }

    /**
     * Re-render create form với error message và giữ lại input đã nhập.
     */
    private String renderCreateFormWithError(CustomUserDetails user,
                                             String targetTypeStr,
                                             String targetId,
                                             String reasonStr,
                                             String description,
                                             String errorMessage,
                                             Model model) {
        Object targetSnapshot = null;
        ReportTargetType targetTypeEnum = null;
        try {
            if (StringUtils.hasText(targetTypeStr)) {
                targetTypeEnum = ReportTargetType.valueOf(targetTypeStr);
                if (targetTypeEnum == ReportTargetType.REVIEW) {
                    targetSnapshot = reviewService.getReviewById(targetId);
                } else if (targetTypeEnum == ReportTargetType.PRODUCT) {
                    targetSnapshot = productService.getProductById(targetId);
                }
            }
        } catch (Exception ignored) {
            // ignore — chỉ để giữ snapshot nếu load được
        }
        model.addAttribute("targetSnapshot", targetSnapshot);
        model.addAttribute("targetType", targetTypeEnum);
        model.addAttribute("targetId", targetId);
        model.addAttribute("reasons", ReportReason.values());
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("targetTypeLabels", TARGET_TYPE_LABELS_VI);
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        model.addAttribute("error", errorMessage);
        model.addAttribute("formReason", reasonStr);
        model.addAttribute("formDescription", description);
        return "web/report-create";
    }

    // ============================================================
    // DETAIL — Customer xem chi tiết report + status
    // ============================================================

    /**
     * GET /reports/{id} — Chi tiết ReportCase của customer.
     *
     * <p>Hiển thị:</p>
     * <ul>
     *   <li>Status hiện tại + target type/id + target snapshot</li>
     *   <li>Lý do + mô tả + bằng chứng (hình/PDF)</li>
     *   <li>Quyết định của Moderator (nếu có)</li>
     *   <li>Timeline các sự kiện (created → assigned → resolved)</li>
     *   <li>Toast "Đã tạo báo cáo" khi mới redirect từ form</li>
     * </ul>
     *
     * <p><b>Security</b>: chỉ customer là reporter của case mới xem được —
     * service layer / controller enforce ownership để tránh IDOR.</p>
     */
    @GetMapping("/{id}")
    public String reportDetail(@AuthenticationPrincipal CustomUserDetails user,
                               @PathVariable String id,
                               @RequestParam(required = false) String created,
                               Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }

        ReportCase reportCase = reportCaseRepository.findById(id)
                .orElse(null);
        if (reportCase == null) {
            model.addAttribute("error", "Không tìm thấy báo cáo #" + id);
            return "web/report-detail";
        }

        // ===== Ownership check (defense-in-depth) =====
        // Chỉ customer là reporter mới được xem. Nếu report do AUTO (reporterId=null)
        // hoặc customer khác → từ chối truy cập.
        if (reportCase.getReporterId() == null
                || !user.getId().equals(reportCase.getReporterId())) {
            log.warn("IDOR blocked: user={} tried to access report={} owned by {}",
                    user.getId(), id, reportCase.getReporterId());
            model.addAttribute("error", "Bạn không có quyền xem báo cáo này.");
            return "web/report-detail";
        }

        // Load lại target snapshot tươi (nếu còn tồn tại) — dùng để hiển thị preview
        Object targetSnapshot = null;
        try {
            switch (reportCase.getTargetType()) {
                case REVIEW -> targetSnapshot = reviewService.getReviewById(reportCase.getTargetId());
                case PRODUCT -> targetSnapshot = productService.getProductById(reportCase.getTargetId());
                default -> { /* ignore */ }
            }
        } catch (ResourceNotFoundException ignored) {
            // target có thể đã bị xóa — vẫn hiển thị targetSnapshot từ ReportCase
        }

        // Build timeline
        List<TimelineEvent> timeline = buildTimeline(reportCase);

        model.addAttribute("report", reportCase);
        model.addAttribute("targetSnapshot", targetSnapshot);
        model.addAttribute("timeline", timeline);
        model.addAttribute("reasonLabel",
                REASON_LABELS_VI.getOrDefault(parseReason(reportCase.getReason()),
                        reportCase.getReason()));
        model.addAttribute("statusLabel", STATUS_LABELS_VI.getOrDefault(reportCase.getStatus(),
                reportCase.getStatus() != null ? reportCase.getStatus().name() : ""));
        model.addAttribute("targetTypeLabel",
                TARGET_TYPE_LABELS_VI.getOrDefault(reportCase.getTargetType(),
                        reportCase.getTargetType() != null ? reportCase.getTargetType().name() : ""));
        model.addAttribute("statusLabels", STATUS_LABELS_VI);
        model.addAttribute("reasonLabels", REASON_LABELS_VI);
        model.addAttribute("targetTypeLabels", TARGET_TYPE_LABELS_VI);
        model.addAttribute("showCreatedToast", "1".equals(created));
        return "web/report-detail";
    }

    // ============================================================
    // HELPERS
    // ============================================================

    /**
     * Parse reason từ string (legacy Task #26) hoặc enum (Phase 2C).
     */
    private ReportReason parseReason(String reasonStr) {
        if (reasonStr == null || reasonStr.isBlank()) return null;
        try {
            return ReportReason.valueOf(reasonStr);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Build timeline từ ReportCase.createdAt / assignedAt / resolvedAt.
     */
    private List<TimelineEvent> buildTimeline(ReportCase c) {
        List<TimelineEvent> events = new ArrayList<>();
        events.add(new TimelineEvent(
                "CREATED",
                "Đã gửi báo cáo",
                "Bạn đã gửi báo cáo đến đội ngũ kiểm duyệt. Mã case sẽ được hiển thị trong hệ thống.",
                c.getCreatedAt(),
                "fas fa-flag",
                "completed"));
        if (c.getAssignedAt() != null) {
            events.add(new TimelineEvent(
                    "ASSIGNED",
                    "Đã được Moderator tiếp nhận",
                    "Moderator đã nhận xử lý báo cáo của bạn.",
                    c.getAssignedAt(),
                    "fas fa-user-check",
                    "completed"));
        }
        if (c.getResolvedAt() != null) {
            String label;
            String icon;
            switch (c.getDecision() != null ? c.getDecision().name() : "RESOLVED") {
                case "APPROVE" -> {
                    label = "Đã duyệt — Target an toàn";
                    icon = "fas fa-check-circle";
                }
                case "REJECT" -> {
                    label = "Đã từ chối — Target vi phạm";
                    icon = "fas fa-times-circle";
                }
                case "ESCALATE" -> {
                    label = "Đã chuyển lên Admin";
                    icon = "fas fa-level-up-alt";
                }
                default -> {
                    label = "Đã xử lý";
                    icon = "fas fa-check";
                }
            }
            events.add(new TimelineEvent(
                    "RESOLVED",
                    label,
                    c.getDecisionReason() != null ? c.getDecisionReason() : "Đã hoàn tất xử lý.",
                    c.getResolvedAt(),
                    icon,
                    "completed"));
        }
        if (c.getUpdatedAt() != null && c.getResolvedAt() == null) {
            events.add(new TimelineEvent(
                    "UPDATED",
                    "Đã cập nhật",
                    "Hệ thống đã cập nhật trạng thái báo cáo.",
                    c.getUpdatedAt(),
                    "fas fa-sync-alt",
                    "completed"));
        }

        // Sort by timestamp asc
        events.sort(Comparator.comparing(
                TimelineEvent::getTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return events;
    }

    // ============================================================
    // CONSTANTS — Vietnamese labels
    // ============================================================

    private static final Map<ReportCaseStatus, String> STATUS_LABELS_VI = new HashMap<>();
    private static final Map<ReportReason, String> REASON_LABELS_VI = new HashMap<>();
    private static final Map<ReportTargetType, String> TARGET_TYPE_LABELS_VI = new LinkedHashMap<>();

    static {
        // Status
        STATUS_LABELS_VI.put(ReportCaseStatus.PENDING, "Đang chờ Moderator");
        STATUS_LABELS_VI.put(ReportCaseStatus.OPEN, "Đã mở — đang xem xét");
        STATUS_LABELS_VI.put(ReportCaseStatus.IN_REVIEW, "Đang xem xét");
        STATUS_LABELS_VI.put(ReportCaseStatus.RESOLVED, "Đã xử lý");
        STATUS_LABELS_VI.put(ReportCaseStatus.APPROVED, "Đã duyệt");
        STATUS_LABELS_VI.put(ReportCaseStatus.REJECTED, "Bị từ chối");
        STATUS_LABELS_VI.put(ReportCaseStatus.ESCALATED, "Đã chuyển lên Admin");

        // Reason (Phase 2C + legacy)
        REASON_LABELS_VI.put(ReportReason.SPAM, "Spam / Quảng cáo");
        REASON_LABELS_VI.put(ReportReason.FAKE_PRODUCT, "Sản phẩm giả");
        REASON_LABELS_VI.put(ReportReason.COUNTERFEIT_IP, "Hàng nhái / Vi phạm IP");
        REASON_LABELS_VI.put(ReportReason.PROHIBITED_CONTENT, "Nội dung bị cấm");
        REASON_LABELS_VI.put(ReportReason.OFFENSIVE_LANGUAGE, "Ngôn từ xúc phạm");
        REASON_LABELS_VI.put(ReportReason.PII_LEAK, "Lộ thông tin cá nhân");
        REASON_LABELS_VI.put(ReportReason.OFF_PLATFORM_CONTACT, "Liên lạc ngoài sàn");
        REASON_LABELS_VI.put(ReportReason.MISLEADING_DESCRIPTION, "Mô tả sai sự thật");
        REASON_LABELS_VI.put(ReportReason.DUPLICATE_LISTING, "Sản phẩm trùng lặp");
        REASON_LABELS_VI.put(ReportReason.PRICE_ANOMALY, "Giá bất thường");
        REASON_LABELS_VI.put(ReportReason.WRONG_CATEGORY, "Sai danh mục");
        REASON_LABELS_VI.put(ReportReason.REVIEW_MANIPULATION, "Review bị thao túng");
        REASON_LABELS_VI.put(ReportReason.OTHER, "Khác");

        // Target type
        TARGET_TYPE_LABELS_VI.put(ReportTargetType.REVIEW, "Đánh giá");
        TARGET_TYPE_LABELS_VI.put(ReportTargetType.PRODUCT, "Sản phẩm");
    }

    /**
     * Timeline event cho report detail page.
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
}