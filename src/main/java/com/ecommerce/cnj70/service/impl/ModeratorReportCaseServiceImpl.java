package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseDecision;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseDetail;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseQueueItem;
import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportReason;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.EscalationRepository;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReportCaseRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.repository.ViolationRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.EscalationWriter;
import com.ecommerce.cnj70.service.ModeratorReportCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 2C — Moderator ReportCase Service Implementation.
 *
 * <p>See {@link ModeratorReportCaseService} for the contract.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeratorReportCaseServiceImpl implements ModeratorReportCaseService {

    /** Queue universe — cases a Moderator is allowed to see & act on. */
    private static final Set<ReportCaseStatus> QUEUE_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(ReportCaseStatus.OPEN, ReportCaseStatus.IN_REVIEW));

    private final ReportCaseRepository reportCaseRepository;
    private final EscalationRepository escalationRepository;
    private final ModerationHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final ViolationRepository violationRepository;
    private final AuditEventWriter auditEventWriter;
    private final EscalationWriter escalationWriter;

    // ======================== QUEUE ========================

    @Override
    public Page<ReportCaseQueueItem> listQueue(ReportCaseStatus statusFilter,
                                               ReportReason reasonFilter,
                                               ReportCaseResourceType resourceTypeFilter,
                                               Pageable pageable) {
        // Defensive: terminal states are not part of the queue. Short-circuit to avoid
        // issuing a DB query that would necessarily return nothing useful.
        if (statusFilter != null && statusFilter.isTerminal()) {
            return Page.empty(pageable);
        }
        Set<ReportCaseStatus> effectiveStatuses = (statusFilter != null
                && QUEUE_STATUSES.contains(statusFilter))
                ? Collections.singleton(statusFilter)
                : QUEUE_STATUSES;

        boolean hasReason = reasonFilter != null;
        boolean hasResource = resourceTypeFilter != null;

        Page<ReportCase> page;
        if (hasReason && hasResource) {
            page = reportCaseRepository.findByStatusInAndReasonAndResourceType(
                    effectiveStatuses, reasonFilter, resourceTypeFilter, pageable);
        } else if (hasReason) {
            page = reportCaseRepository.findByStatusInAndReason(
                    effectiveStatuses, reasonFilter, pageable);
        } else if (hasResource) {
            page = reportCaseRepository.findByStatusInAndResourceType(
                    effectiveStatuses, resourceTypeFilter, pageable);
        } else {
            page = reportCaseRepository.findByStatusIn(effectiveStatuses, pageable);
        }

        List<ReportCaseQueueItem> items = page.getContent().stream()
                .map(ReportCaseQueueItem::fromEntity)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    // ======================== DETAIL ========================

    @Override
    public ReportCaseDetail getDetail(String caseId) {
        ReportCase rc = loadAndGuard(caseId);

        // Pull live resource name (if available) — denormalized resourceName on the
        // case is best-effort; we prefer the live record for the detail page.
        String liveResourceName = rc.getResourceName();
        if (rc.getResourceId() != null) {
            if (rc.getResourceType() == ReportCaseResourceType.PRODUCT) {
                Product p = productRepository.findById(rc.getResourceId()).orElse(null);
                if (p != null) liveResourceName = p.getName();
            } else if (rc.getResourceType() == ReportCaseResourceType.REVIEW) {
                Review r = reviewRepository.findById(rc.getResourceId()).orElse(null);
                if (r != null) liveResourceName = "Review by " + (r.getUserName() != null ? r.getUserName() : r.getUserId());
            }
        }

        // Vendor / Shop enrichment
        String vendorName = rc.getVendorName();
        String shopName = rc.getShopName();
        if (rc.getShopId() != null) {
            Shop shop = shopRepository.findById(rc.getShopId()).orElse(null);
            if (shop != null) {
                shopName = shop.getShopName();
                if (rc.getVendorId() == null && shop.getOwnerId() != null) {
                    User vendor = userRepository.findById(shop.getOwnerId()).orElse(null);
                    if (vendor != null) {
                        vendorName = vendor.getFullName();
                    }
                }
            }
        }
        if (vendorName == null && rc.getVendorId() != null) {
            User vendor = userRepository.findById(rc.getVendorId()).orElse(null);
            if (vendor != null) vendorName = vendor.getFullName();
        }

        // Reporter enrichment
        String reporterEmail = rc.getReporterEmail();
        if (reporterEmail == null && rc.getReporterId() != null) {
            User reporter = userRepository.findById(rc.getReporterId()).orElse(null);
            if (reporter != null) reporterEmail = reporter.getEmail();
        }

        // Previous cases on the same resource (Phase 2C §18)
        List<ReportCaseQueueItem> previousCases = rc.getResourceId() != null && rc.getResourceType() != null
                ? reportCaseRepository
                    .findByResourceTypeAndResourceIdOrderByCreatedAtDesc(rc.getResourceType(), rc.getResourceId())
                    .stream()
                    .filter(prev -> !prev.getId().equals(rc.getId()))
                    .map(ReportCaseQueueItem::fromEntity)
                    .collect(Collectors.toList())
                : List.of();

        // Vendor history snapshot (Phase 2C §17)
        long vendorTotal = 0L, vendorOpen = 0L, vendorEsc = 0L;
        if (rc.getVendorId() != null) {
            List<ReportCase> vendorCases = reportCaseRepository
                    .findByVendorIdOrderByCreatedAtDesc(rc.getVendorId());
            vendorTotal = vendorCases.size();
            vendorOpen = vendorCases.stream()
                    .filter(c -> c.getStatus() == ReportCaseStatus.OPEN || c.getStatus() == ReportCaseStatus.IN_REVIEW)
                    .count();
            vendorEsc = vendorCases.stream()
                    .filter(c -> c.getStatus() == ReportCaseStatus.ESCALATED)
                    .count();
        }

        return ReportCaseDetail.builder()
                .caseId(rc.getId())
                .resourceType(rc.getResourceType())
                .resourceId(rc.getResourceId())
                .resourceName(liveResourceName)
                .reporterId(rc.getReporterId())
                .reporterName(rc.getReporterName())
                .reporterEmail(reporterEmail)
                .reason(rc.getReportReason())
                .description(rc.getDescription())
                .autoModerationStatus(rc.getAutoModerationStatus())
                .autoFlags(rc.getAutoFlags())
                .evidence(rc.getEvidence())
                .vendorId(rc.getVendorId())
                .vendorName(vendorName)
                .shopId(rc.getShopId())
                .shopName(shopName)
                .status(rc.getStatus())
                .assignedModeratorId(rc.getAssignedModeratorId())
                .assignedModeratorEmail(rc.getAssignedModeratorEmail())
                .decisionModeratorId(rc.getDecisionModeratorId())
                .decisionModeratorEmail(rc.getDecisionModeratorEmail())
                .decisionReason(rc.getDecisionReason())
                .decisionNote(rc.getDecisionNote())
                .decidedAt(rc.getDecidedAt())
                .escalationSeverity(rc.getEscalationSeverity())
                .createdAt(rc.getCreatedAt())
                .updatedAt(rc.getUpdatedAt())
                .previousCases(previousCases)
                .vendorTotalCases(vendorTotal)
                .vendorOpenCases(vendorOpen)
                .vendorEscalatedCases(vendorEsc)
                .build();
    }

    // ======================== CLAIM (open|pending → in_review) ========================

    @Override
    @Transactional
    public ReportCaseDecision claim(String caseId, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        ReportCase rc = loadAndGuard(caseId);

        // Phase 1 — accept both Phase 2C entry state (OPEN) and legacy entry state (PENDING).
        // PENDING is what ReportCaseServiceImpl.createCase() writes; OPEN is what other
        // new pipelines write. Both transition to IN_REVIEW here.
        if (rc.getStatus() != ReportCaseStatus.OPEN
                && rc.getStatus() != ReportCaseStatus.IN_REVIEW
                && rc.getStatus() != ReportCaseStatus.PENDING) {
            throw new ConflictException("Case đã ở trạng thái cuối, không thể claim (status=" + rc.getStatus() + ")");
        }

        ReportCaseStatus before = rc.getStatus();
        rc.setStatus(ReportCaseStatus.IN_REVIEW);
        rc.setAssignedModeratorId(moderator.getId());
        rc.setAssignedModeratorEmail(moderator.getUsername());
        reportCaseRepository.save(rc);

        ReportCaseDecision decision = ReportCaseDecision.builder()
                .caseId(caseId)
                .action("CLAIM")
                .newStatus(ReportCaseStatus.IN_REVIEW)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã nhận xử lý case (ID: " + caseId + ")")
                .build();

        recordHistory(rc, decision, before, null);
        emitAudit(rc, decision, before, clientIp);
        log.info("Moderator claim case: caseId={} moderatorId={} before={} after=IN_REVIEW",
                caseId, moderator.getId(), before);
        return decision;
    }

    // ======================== APPROVE ========================

    @Override
    @Transactional
    public ReportCaseDecision approve(String caseId, String reason, String note,
                                      CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        ReportCase rc = loadAndGuard(caseId);
        ensureClaimed(rc, moderator);

        ReportCaseStatus before = rc.getStatus();
        if (before.isTerminal()) {
            throw new ConflictException("Case đã ở trạng thái cuối, không thể duyệt (status=" + before + ")");
        }

        rc.setStatus(ReportCaseStatus.APPROVED);
        rc.setDecisionModeratorId(moderator.getId());
        rc.setDecisionModeratorEmail(moderator.getUsername());
        rc.setDecisionReason(reason);
        rc.setDecisionNote(note);
        rc.setDecidedAt(LocalDateTime.now());
        reportCaseRepository.save(rc);

        ReportCaseDecision decision = ReportCaseDecision.builder()
                .caseId(caseId)
                .action("APPROVE")
                .newStatus(ReportCaseStatus.APPROVED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã duyệt case (ID: " + caseId + ")")
                .build();

        recordHistory(rc, decision, before, reason);
        emitAudit(rc, decision, before, clientIp);
        log.info("Moderator approve case: caseId={} moderatorId={} before={} after=APPROVED",
                caseId, moderator.getId(), before);
        return decision;
    }

    // ======================== REJECT ========================

    @Override
    @Transactional
    public ReportCaseDecision reject(String caseId, String reason, String note,
                                     CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        validateReason(reason);
        ReportCase rc = loadAndGuard(caseId);
        ensureClaimed(rc, moderator);

        ReportCaseStatus before = rc.getStatus();
        if (before.isTerminal()) {
            throw new ConflictException("Case đã ở trạng thái cuối, không thể từ chối (status=" + before + ")");
        }

        rc.setStatus(ReportCaseStatus.REJECTED);
        rc.setDecisionModeratorId(moderator.getId());
        rc.setDecisionModeratorEmail(moderator.getUsername());
        rc.setDecisionReason(reason);
        rc.setDecisionNote(note);
        rc.setDecidedAt(LocalDateTime.now());
        reportCaseRepository.save(rc);

        // Phase 1 — ReportCase → Violation consistency:
        // Khi Moderator REJECT một ReportCase nghiêm trọng (case có shopId +
        // không phải INAPPROPRIATE_CONTEXT nhỏ), tạo Violation document để
        // ViolationSeverity/ViolationAction có đầy đủ history cho Admin.
        // Severity mặc định MEDIUM theo ViolationServiceImpl.createViolation().
        // KHÔNG tự tạo penalty rules (3 violations = BAN…) — chỉ ghi nhận sự kiện.
        if (StringUtils.hasText(rc.getShopId())) {
            try {
                Violation violation = Violation.builder()
                        .shopId(rc.getShopId())
                        .productId(rc.getResourceType() == ReportCaseResourceType.PRODUCT
                                ? rc.getResourceId() : null)
                        .productName(rc.getResourceName())
                        .type(ViolationType.WARNING)
                        .severity(ViolationSeverity.MEDIUM)
                        .reason("[Moderator REJECT ReportCase] " + reason)
                        .adminNote(note)
                        .createdBy(moderator.getUsername())
                        .build();
                violationRepository.save(violation);
            } catch (Exception ex) {
                // Phase 1: log only — không block reject flow vì Violation là side-effect
                log.warn("Failed to create Violation from ReportCase reject: caseId={} error={}",
                        caseId, ex.getMessage());
            }
        }

        ReportCaseDecision decision = ReportCaseDecision.builder()
                .caseId(caseId)
                .action("REJECT")
                .newStatus(ReportCaseStatus.REJECTED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã từ chối case (ID: " + caseId + ")")
                .build();

        recordHistory(rc, decision, before, reason);
        emitAudit(rc, decision, before, clientIp);
        log.info("Moderator reject case: caseId={} moderatorId={} reason.length={}",
                caseId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== ESCALATE ========================

    @Override
    @Transactional
    public ReportCaseDecision escalate(String caseId, String reason, EscalationSeverity severity,
                                       CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        validateReason(reason);
        if (severity == null) {
            throw new BadRequestException("Severity is required for Escalate");
        }
        ReportCase rc = loadAndGuard(caseId);
        ensureClaimed(rc, moderator);

        ReportCaseStatus before = rc.getStatus();
        if (before.isTerminal()) {
            throw new ConflictException("Case đã ở trạng thái cuối, không thể leo thang (status=" + before + ")");
        }

        // Phase 3B §14 — duplicate escalation protection
        // Check if there's already an active escalation for this ReportCase
        List<Escalation> existingEscalations = escalationRepository.findByReportCaseIdAndStatusIn(
                caseId,
                List.of(Escalation.Status.PENDING, Escalation.Status.IN_REVIEW)
        );
        if (!existingEscalations.isEmpty()) {
            throw new ConflictException("Case này đã có escalation đang chờ xử lý (ID: "
                    + existingEscalations.get(0).getId() + ")");
        }

        rc.setStatus(ReportCaseStatus.ESCALATED);
        rc.setDecisionModeratorId(moderator.getId());
        rc.setDecisionModeratorEmail(moderator.getUsername());
        rc.setDecisionReason(reason);
        rc.setDecidedAt(LocalDateTime.now());
        rc.setEscalationSeverity(severity);
        reportCaseRepository.save(rc);

        // Persist Escalation document
        Escalation esc = Escalation.builder()
                .reportCaseId(rc.getId())
                .resourceType(rc.getResourceType())
                .resourceId(rc.getResourceId())
                .vendorId(rc.getVendorId())
                .shopId(rc.getShopId())
                .reason(reason)
                .severity(severity)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .moderatorRole(UserRole.MODERATOR.name())
                .sourceStatus(ReportCaseStatus.ESCALATED)
                .build();
        Escalation savedEsc;
        try {
            savedEsc = escalationRepository.save(esc);
            escalationWriter.notify(savedEsc);
        } catch (RuntimeException ex) {
            log.error("Failed to persist or notify Escalation: caseId={}", caseId, ex);
            throw ex;
        }

        // Phase 3B — Emit ESCALATION_CREATED audit event (after escalation is persisted)
        AuditEvent escalationAudit = AuditEvent.builder()
                .actorId(moderator.getId())
                .actorEmail(moderator.getUsername())
                .role(UserRole.MODERATOR)
                .action("ESCALATION_CREATED")
                .resourceType("ESCALATION")
                .resourceId(savedEsc.getId())
                .reason(reason)
                .severity(severity != null ? severity.name() : null)
                .before(before != null ? before.name() : "NULL")
                .after(Escalation.Status.PENDING.name())
                .ip(clientIp)
                .createdAt(LocalDateTime.now())
                .build();
        safeEmitAudit(escalationAudit);

        ReportCaseDecision decision = ReportCaseDecision.builder()
                .caseId(caseId)
                .action("ESCALATE")
                .newStatus(ReportCaseStatus.ESCALATED)
                .reason(reason)
                .escalationSeverity(severity)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã leo thang case (ID: " + caseId + ") lên Admin")
                .build();

        recordHistory(rc, decision, before, reason);
        emitAudit(rc, decision, before, clientIp);
        log.info("Moderator escalate case: caseId={} escalationId={} moderatorId={} severity={}",
                caseId, savedEsc.getId(), moderator.getId(), severity);
        return decision;
    }

    // ======================== HELPERS ========================

    private ReportCase loadAndGuard(String caseId) {
        if (!StringUtils.hasText(caseId)) {
            throw new BadRequestException("Case ID không hợp lệ");
        }
        return reportCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ReportCase với ID: " + caseId));
    }

    /** On first action, an OPEN/PENDING case auto-claims to the current moderator. */
    private void ensureClaimed(ReportCase rc, CustomUserDetails moderator) {
        // Phase 1 — Source-of-truth reconciliation: PENDING is the legacy initial state
        // set by ReportCaseServiceImpl.createCase() (still used by ReviewServiceImpl.reportReview()
        // and ModeratorQueueController). OPEN/IN_REVIEW are the Phase 2C pipeline states.
        // Both are valid entry points — auto-transition both to IN_REVIEW on first action.
        if (rc.getStatus() == ReportCaseStatus.OPEN
                || rc.getStatus() == ReportCaseStatus.PENDING) {
            rc.setStatus(ReportCaseStatus.IN_REVIEW);
            rc.setAssignedModeratorId(moderator.getId());
            rc.setAssignedModeratorEmail(moderator.getUsername());
            // No history entry — the next decision action will record the transition
            // from initial → terminal status. Stays as a side-effect of the action.
        }
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Reason is required and must not be blank");
        }
    }

    private void recordHistory(ReportCase rc, ReportCaseDecision decision,
                                ReportCaseStatus before, String reason) {
        ModerationAction mappedAction = switch (decision.getAction()) {
            case "APPROVE" -> ModerationAction.APPROVE;
            case "REJECT" -> ModerationAction.REJECT;
            case "ESCALATE" -> ModerationAction.ESCALATE;
            default -> null;
        };
        if (mappedAction == null) {
            return; // CLAIM and other side-effects don't go to history
        }
        ModerationHistory history = ModerationHistory.builder()
                .resourceType("REPORTCASE")
                .resourceId(rc.getId())
                .resourceName(rc.getResourceName() != null
                        ? "Case on " + rc.getResourceName()
                        : "Case " + rc.getId())
                .action(mappedAction)
                .beforeStatus(toModerationStatus(before))
                .afterStatus(toModerationStatus(decision.getNewStatus()))
                .reason(reason)
                .moderatorId(decision.getModeratorId())
                .moderatorEmail(decision.getModeratorEmail())
                .moderatorRole(UserRole.MODERATOR)
                .build();
        try {
            historyRepository.save(history);
        } catch (RuntimeException ex) {
            log.error("Failed to persist ModerationHistory for caseId={} action={}",
                    rc.getId(), decision.getAction(), ex);
        }
    }

    private void emitAudit(ReportCase rc, ReportCaseDecision decision,
                            ReportCaseStatus before, String clientIp) {
        AuditEvent event = AuditEvent.builder()
                .actorId(decision.getModeratorId())
                .actorEmail(decision.getModeratorEmail())
                .role(UserRole.MODERATOR)
                .action("CASE_" + decision.getAction())
                .resourceType("REPORTCASE")
                .resourceId(rc.getId())
                .reason(decision.getReason())
                .before(before != null ? before.name() : "NULL")
                .after(decision.getNewStatus() != null ? decision.getNewStatus().name() : "NULL")
                .ip(clientIp)
                .createdAt(decision.getDecidedAt())
                .build();
        try {
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.error("Failed to emit AuditEvent for caseId={} action={}",
                    rc.getId(), decision.getAction(), ex);
        }
    }

    /**
     * Phase 3B — Safe emit audit event.
     * Catches exceptions to prevent audit failure from breaking business flow.
     */
    private void safeEmitAudit(AuditEvent event) {
        try {
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.error("Failed to emit AuditEvent: action={} resourceType={} resourceId={}",
                    event.getAction(), event.getResourceType(), event.getResourceId(), ex);
        }
    }

    /** Map a {@link ReportCaseStatus} to {@link ModerationStatus} for history display. */
    private static ModerationStatus toModerationStatus(ReportCaseStatus s) {
        if (s == null) return null;
        return switch (s) {
            case PENDING -> ModerationStatus.PENDING_MANUAL;
            case OPEN -> ModerationStatus.PENDING_MANUAL;
            case IN_REVIEW -> ModerationStatus.PENDING_MANUAL;
            case RESOLVED -> ModerationStatus.APPROVED;
            case APPROVED -> ModerationStatus.APPROVED;
            case REJECTED -> ModerationStatus.REJECTED;
            case ESCALATED -> ModerationStatus.ESCALATED;
        };
    }

    private static final class ModeratorGuard {
        static void verify(CustomUserDetails moderator) {
            if (moderator == null) {
                throw new UnauthorizedException("Yêu cầu đăng nhập Moderator");
            }
            if (!"MODERATOR".equals(moderator.getRole())) {
                throw new UnauthorizedException("Tài khoản không có quyền Moderator");
            }
            if (!moderator.isEnabled() || !moderator.isAccountNonLocked()) {
                throw new UnauthorizedException("Tài khoản Moderator đã bị khóa hoặc chưa kích hoạt");
            }
        }
    }
}
