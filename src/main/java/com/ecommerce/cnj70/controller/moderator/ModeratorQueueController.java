package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.dto.request.AssignReportCaseReq;
import com.ecommerce.cnj70.dto.request.ReportCaseCreateReq;
import com.ecommerce.cnj70.dto.request.ReportCaseDecisionReq;
import com.ecommerce.cnj70.dto.response.ReportCaseRes;
import com.ecommerce.cnj70.dto.response.ReportCaseStatsRes;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportTargetType;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ReportCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TASK #26 — Moderator Queue Controller (REST API).
 *
 * Endpoints (tất cả đều yêu cầu @PreAuthorize("hasRole('MODERATOR')")):
 *
 *   Queue:
 *     GET    /api/moderator/cases                              - Lấy queue (filter status + targetType)
 *     GET    /api/moderator/cases/{id}                         - Chi tiết case
 *     POST   /api/moderator/cases                              - Tạo case (từ Auto Moderation hoặc Admin)
 *     GET    /api/moderator/cases/target/{type}/{targetId}     - Lịch sử case của 1 target
 *     GET    /api/moderator/cases/stats                        - Thống kê cho dashboard
 *
 *   Assignment:
 *     POST   /api/moderator/cases/{id}/assign                  - Assign case cho mình
 *
 *   Decision (3 Actions):
 *     POST   /api/moderator/cases/{id}/approve                - Action 1: APPROVE
 *     POST   /api/moderator/cases/{id}/reject                 - Action 2: REJECT (cần reason)
 *     POST   /api/moderator/cases/{id}/escalate               - Action 3: ESCALATE (cần reason)
 */
@Slf4j
@RestController
@RequestMapping("/api/moderator/cases")
@PreAuthorize("hasRole('MODERATOR')")
@RequiredArgsConstructor
public class ModeratorQueueController {

    private final ReportCaseService reportCaseService;

    // ===== Queue CRUD =====

    /**
     * GET /api/moderator/cases
     * Lấy queue ReportCase cho Moderator.
     *
     * @param status     PENDING (mặc định) / RESOLVED / ESCALATED
     * @param targetType PRODUCT / REVIEW / COMPLAINT (optional)
     */
    @GetMapping
    public ResponseEntity<Page<ReportCaseRes>> getQueue(
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ReportCaseStatus statusEnum = parseStatus(status);
        ReportTargetType typeEnum = parseTargetType(targetType);
        Pageable pageable = PageRequest.of(Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "priority")
                        .and(Sort.by(Sort.Direction.ASC, "createdAt")));

        Page<ReportCaseRes> result = reportCaseService.getQueue(statusEnum, typeEnum, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/moderator/cases/{id}
     * Chi tiết một case.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportCaseRes> getCaseDetail(@PathVariable String id) {
        return ResponseEntity.ok(reportCaseService.getCaseById(id));
    }

    /**
     * POST /api/moderator/cases
     * Tạo case mới (chủ yếu dành cho Auto Moderation hoặc Admin escalate).
     * Customer report sẽ dùng endpoint riêng /api/reviews/{id}/report.
     */
    @PostMapping
    public ResponseEntity<ReportCaseRes> createCase(
            @Valid @RequestBody ReportCaseCreateReq request,
            @AuthenticationPrincipal CustomUserDetails user) {
        String reporterId = user != null ? user.getId() : null;
        String reporterUsername = user != null ? user.getUsername() : "SYSTEM";
        ReportCaseRes created = reportCaseService.createCase(request, reporterId, reporterUsername);
        return ResponseEntity.ok(created);
    }

    /**
     * GET /api/moderator/cases/target/{type}/{targetId}
     * Lịch sử ReportCase của một target (product/review).
     */
    @GetMapping("/target/{type}/{targetId}")
    public ResponseEntity<Page<ReportCaseRes>> getCasesByTarget(
            @PathVariable String type,
            @PathVariable String targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        ReportTargetType typeEnum = parseTargetType(type);
        if (typeEnum == null) {
            return ResponseEntity.badRequest().build();
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ResponseEntity.ok(reportCaseService.getCasesByTarget(typeEnum, targetId, pageable));
    }

    /**
     * GET /api/moderator/cases/stats
     * Thống kê cho dashboard.
     */
    @GetMapping("/stats")
    public ResponseEntity<ReportCaseStatsRes> getStats() {
        return ResponseEntity.ok(reportCaseService.getStats());
    }

    // ===== Assignment =====

    /**
     * POST /api/moderator/cases/{id}/assign
     * Moderator tự assign case cho mình.
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<ReportCaseRes> assignCase(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {

        String moderatorId = user != null ? user.getId() : null;
        String moderatorUsername = user != null ? user.getUsername() : "MODERATOR";

        if (moderatorId == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(reportCaseService.assignCase(id, moderatorId, moderatorUsername));
    }

    // ===== Decision: 3 Actions =====

    /**
     * POST /api/moderator/cases/{id}/approve
     * Action 1 - APPROVE: target an toàn, hiển thị lại.
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<ReportCaseRes> approveCase(
            @PathVariable String id,
            @Valid @RequestBody ReportCaseDecisionReq request,
            @AuthenticationPrincipal CustomUserDetails user) {

        String moderatorId = user != null ? user.getId() : null;
        String moderatorUsername = user != null ? user.getUsername() : "MODERATOR";

        if (moderatorId == null) {
            return ResponseEntity.status(401).build();
        }
        ReportCaseRes result = reportCaseService.approveCase(id, request, moderatorId, moderatorUsername);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /api/moderator/cases/{id}/reject
     * Action 2 - REJECT: bắt buộc có lý do (>= 10 ký tự).
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectCase(
            @PathVariable String id,
            @Valid @RequestBody ReportCaseDecisionReq request,
            @AuthenticationPrincipal CustomUserDetails user) {

        String moderatorId = user != null ? user.getId() : null;
        String moderatorUsername = user != null ? user.getUsername() : "MODERATOR";

        if (moderatorId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ReportCaseRes result = reportCaseService.rejectCase(id, request, moderatorId, moderatorUsername);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("[Moderator] REJECT failed for case {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "caseId", id,
                    "action", "REJECT"
            ));
        }
    }

    /**
     * POST /api/moderator/cases/{id}/escalate
     * Action 3 - ESCALATE: chuyển lên Admin (cần reason >= 10 ký tự).
     */
    @PostMapping("/{id}/escalate")
    public ResponseEntity<?> escalateCase(
            @PathVariable String id,
            @Valid @RequestBody ReportCaseDecisionReq request,
            @AuthenticationPrincipal CustomUserDetails user) {

        String moderatorId = user != null ? user.getId() : null;
        String moderatorUsername = user != null ? user.getUsername() : "MODERATOR";

        if (moderatorId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ReportCaseRes result = reportCaseService.escalateCase(id, request, moderatorId, moderatorUsername);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.warn("[Moderator] ESCALATE failed for case {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "caseId", id,
                    "action", "ESCALATE"
            ));
        }
    }

    // ===== Helper =====

    private ReportCaseStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return ReportCaseStatus.PENDING;
        try {
            return ReportCaseStatus.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ReportCaseStatus.PENDING;
        }
    }

    private ReportTargetType parseTargetType(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return ReportTargetType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

