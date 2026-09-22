package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.dto.request.ReportCaseCreateReq;
import com.ecommerce.cnj70.dto.request.ReportCaseDecisionReq;
import com.ecommerce.cnj70.dto.response.ReportCaseRes;
import com.ecommerce.cnj70.dto.response.ReportCaseStatsRes;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.ReportCaseDecision;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportTargetType;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReportCaseRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.ReportCaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * TASK #26 — ReportCase Service Implementation.
 *
 * Đảm bảo tính toàn vẹn:
 *   - @Transactional cho mỗi action ra quyết định
 *   - Lưu targetStatusBefore để rollback khi cần
 *   - AuditLog luôn được ghi kèm với metadata đầy đủ
 *   - Idempotent: không tạo 2 case PENDING cho cùng target
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCaseServiceImpl implements ReportCaseService {

    private final ReportCaseRepository reportCaseRepository;
    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    // ===== CRUD =====

    @Override
    @Transactional
    public ReportCaseRes createCase(ReportCaseCreateReq request, String reporterId, String reporterUsername) {
        log.info("[ReportCase] Creating case: targetType={}, targetId={}, source={}",
                request.getTargetType(), request.getTargetId(), request.getSource());

        // 1. Validate target tồn tại
        validateTargetExists(request.getTargetType(), request.getTargetId());

        // 2. Idempotent: nếu đã có case PENDING cho target, trả về case cũ
        var existingOpt = reportCaseRepository.findFirstByTargetTypeAndTargetIdAndStatus(
                request.getTargetType(), request.getTargetId(), ReportCaseStatus.PENDING);
        if (existingOpt.isPresent()) {
            log.info("[ReportCase] PENDING case already exists for target {} {}",
                    request.getTargetType(), request.getTargetId());
            return toResponse(existingOpt.get(), reporterUsername, null);
        }

        // 3. Tạo case mới
        String source = request.getSource() != null ? request.getSource() : "USER";
        ReportCase reportCase = ReportCase.builder()
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .targetSnapshot(snapshotTarget(request.getTargetType(), request.getTargetId()))
                .reporterId(reporterId)
                .reason(request.getReason())
                .description(request.getDescription())
                .evidence(request.getEvidence() != null ? request.getEvidence() : List.of())
                .autoFlags(request.getAutoFlags() != null ? request.getAutoFlags() : List.of())
                .source(source)
                .status(ReportCaseStatus.PENDING)
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .build();

        ReportCase saved = reportCaseRepository.save(reportCase);

        // 4. Update trạng thái target sang PENDING_MANUAL
        updateTargetToPendingManual(request.getTargetType(), request.getTargetId());

        // 5. AuditLog
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("targetType", request.getTargetType().name());
        metadata.put("targetId", request.getTargetId());
        metadata.put("source", source);

        AuditSeverity severity = "AUTO".equalsIgnoreCase(source)
                ? AuditSeverity.WARNING
                : AuditSeverity.INFO;

        auditLogService.log(
                AuditAction.REPORT_CASE_CREATED,
                "REPORT_CASE",
                saved.getId(),
                reporterId,
                reporterUsername,
                "AUTO".equalsIgnoreCase(source) ? "SYSTEM" : "CUSTOMER",
                severity,
                "Tạo ReportCase: " + request.getReason(),
                metadata
        );

        log.info("[ReportCase] Created case id={}", saved.getId());
        return toResponse(saved, reporterUsername, null);
    }

    @Override
    public ReportCaseRes getCaseById(String caseId) {
        ReportCase reportCase = reportCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ReportCase: " + caseId));
        return toResponse(reportCase, resolveUsername(reportCase.getReporterId()),
                resolveUsername(reportCase.getAssignedModeratorId()));
    }

    @Override
    public Page<ReportCaseRes> getQueue(ReportCaseStatus status, ReportTargetType targetType, Pageable pageable) {
        ReportCaseStatus effectiveStatus = status != null ? status : ReportCaseStatus.PENDING;

        Page<ReportCase> cases;
        if (targetType != null) {
            cases = reportCaseRepository.findByStatusAndTargetType(effectiveStatus, targetType, pageable);
        } else {
            cases = reportCaseRepository.findByStatusOrderByPriorityDescCreatedAtAsc(effectiveStatus, pageable);
        }

        return cases.map(c -> toResponse(c, resolveUsername(c.getReporterId()),
                resolveUsername(c.getAssignedModeratorId())));
    }

    @Override
    public Page<ReportCaseRes> getCasesByTarget(ReportTargetType targetType, String targetId, Pageable pageable) {
        Page<ReportCase> cases = reportCaseRepository.findByTargetTypeAndTargetId(targetType, targetId, pageable);
        return cases.map(c -> toResponse(c, resolveUsername(c.getReporterId()),
                resolveUsername(c.getAssignedModeratorId())));
    }

    // ===== Assignment =====

    @Override
    @Transactional
    public ReportCaseRes assignCase(String caseId, String moderatorId, String moderatorUsername) {
        ReportCase reportCase = reportCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ReportCase: " + caseId));

        if (reportCase.getStatus() != ReportCaseStatus.PENDING) {
            throw new BadRequestException(
                    "ReportCase không ở trạng thái PENDING (hiện tại: " + reportCase.getStatus() + ")");
        }

        reportCase.setAssignedModeratorId(moderatorId);
        reportCase.setAssignedAt(LocalDateTime.now());
        ReportCase saved = reportCaseRepository.save(reportCase);

        // AuditLog
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("moderatorId", moderatorId);
        auditLogService.logInfo(
                AuditAction.REPORT_CASE_ASSIGNED,
                "REPORT_CASE",
                caseId,
                moderatorId,
                moderatorUsername,
                "MODERATOR",
                "Moderator " + moderatorUsername + " nhận xử lý case"
        );

        log.info("[ReportCase] Case {} assigned to moderator {}", caseId, moderatorUsername);
        return toResponse(saved, resolveUsername(saved.getReporterId()), moderatorUsername);
    }

    // ===== Quyết định =====

    @Override
    @Transactional
    public ReportCaseRes approveCase(String caseId, ReportCaseDecisionReq request,
                                     String moderatorId, String moderatorUsername) {
        ReportCase reportCase = getPendingCaseOrThrow(caseId, moderatorUsername);

        // Lưu trạng thái target TRƯỚC khi xử lý (để audit + rollback)
        String targetStatusBefore = getCurrentTargetStatus(reportCase.getTargetType(), reportCase.getTargetId());

        // Update ReportCase
        reportCase.setDecision(ReportCaseDecision.APPROVE);
        reportCase.setDecisionReason(request.getReason());
        reportCase.setNote(request.getNote());
        reportCase.setStatus(ReportCaseStatus.RESOLVED);
        reportCase.setAssignedModeratorId(moderatorId);
        reportCase.setAssignedAt(reportCase.getAssignedAt() != null ? reportCase.getAssignedAt() : LocalDateTime.now());
        reportCase.setResolvedAt(LocalDateTime.now());
        reportCase.setTargetStatusBefore(targetStatusBefore);
        ReportCase saved = reportCaseRepository.save(reportCase);

        // Update target sang trạng thái APPROVED
        String targetStatusAfter = approveTarget(reportCase.getTargetType(), reportCase.getTargetId());
        saved.setTargetStatusAfter(targetStatusAfter);
        reportCaseRepository.save(saved);

        // AuditLog
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("decision", "APPROVE");
        metadata.put("targetType", reportCase.getTargetType().name());
        metadata.put("targetId", reportCase.getTargetId());
        metadata.put("statusBefore", targetStatusBefore);
        metadata.put("statusAfter", targetStatusAfter);
        auditLogService.logInfo(
                AuditAction.REPORT_CASE_APPROVED,
                "REPORT_CASE",
                caseId,
                moderatorId,
                moderatorUsername,
                "MODERATOR",
                "Duyệt ReportCase " + caseId + " - Target " + reportCase.getTargetType() + " an toàn",
                metadata
        );

        log.info("[ReportCase] Case {} APPROVED by moderator {}", caseId, moderatorUsername);
        return toResponse(saved, resolveUsername(saved.getReporterId()), moderatorUsername);
    }

    @Override
    @Transactional
    public ReportCaseRes rejectCase(String caseId, ReportCaseDecisionReq request,
                                    String moderatorId, String moderatorUsername) {
        // Validate: REJECT bắt buộc phải có lý do (>= 10 ký tự)
        if (request.getReason() == null || request.getReason().trim().length() < 10) {
            throw new BadRequestException("REJECT bắt buộc phải có lý do (tối thiểu 10 ký tự)");
        }

        ReportCase reportCase = getPendingCaseOrThrow(caseId, moderatorUsername);

        String targetStatusBefore = getCurrentTargetStatus(reportCase.getTargetType(), reportCase.getTargetId());

        reportCase.setDecision(ReportCaseDecision.REJECT);
        reportCase.setDecisionReason(request.getReason());
        reportCase.setNote(request.getNote());
        reportCase.setStatus(ReportCaseStatus.RESOLVED);
        reportCase.setAssignedModeratorId(moderatorId);
        reportCase.setAssignedAt(reportCase.getAssignedAt() != null ? reportCase.getAssignedAt() : LocalDateTime.now());
        reportCase.setResolvedAt(LocalDateTime.now());
        reportCase.setTargetStatusBefore(targetStatusBefore);
        ReportCase saved = reportCaseRepository.save(reportCase);

        // Update target sang trạng thái REJECTED/HIDDEN
        String targetStatusAfter = rejectTarget(reportCase.getTargetType(), reportCase.getTargetId(), request.getReason());
        saved.setTargetStatusAfter(targetStatusAfter);
        reportCaseRepository.save(saved);

        // AuditLog - WARNING severity cho REJECT
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("decision", "REJECT");
        metadata.put("targetType", reportCase.getTargetType().name());
        metadata.put("targetId", reportCase.getTargetId());
        metadata.put("reason", request.getReason());
        metadata.put("statusBefore", targetStatusBefore);
        metadata.put("statusAfter", targetStatusAfter);
        auditLogService.logWarning(
                AuditAction.REPORT_CASE_REJECTED,
                "REPORT_CASE",
                caseId,
                moderatorId,
                moderatorUsername,
                "MODERATOR",
                "Từ chối ReportCase " + caseId + " - Lý do: " + request.getReason(),
                metadata
        );

        log.info("[ReportCase] Case {} REJECTED by moderator {} - reason: {}", caseId, moderatorUsername, request.getReason());
        return toResponse(saved, resolveUsername(saved.getReporterId()), moderatorUsername);
    }

    @Override
    @Transactional
    public ReportCaseRes escalateCase(String caseId, ReportCaseDecisionReq request,
                                      String moderatorId, String moderatorUsername) {
        // Validate: ESCALATE bắt buộc phải có lý do
        if (request.getReason() == null || request.getReason().trim().length() < 10) {
            throw new BadRequestException("ESCALATE bắt buộc phải có lý do (tối thiểu 10 ký tự)");
        }

        ReportCase reportCase = getPendingCaseOrThrow(caseId, moderatorUsername);

        String targetStatusBefore = getCurrentTargetStatus(reportCase.getTargetType(), reportCase.getTargetId());

        reportCase.setDecision(ReportCaseDecision.ESCALATE);
        reportCase.setDecisionReason(request.getReason());
        reportCase.setNote(request.getNote());
        reportCase.setStatus(ReportCaseStatus.ESCALATED);
        reportCase.setAssignedModeratorId(moderatorId);
        reportCase.setAssignedAt(reportCase.getAssignedAt() != null ? reportCase.getAssignedAt() : LocalDateTime.now());
        reportCase.setResolvedAt(LocalDateTime.now());
        reportCase.setTargetStatusBefore(targetStatusBefore);
        // ESCALATE: target vẫn giữ trạng thái PENDING_MANUAL (chờ Admin xử lý)
        reportCase.setTargetStatusAfter("PENDING_MANUAL");
        ReportCase saved = reportCaseRepository.save(reportCase);

        // AuditLog - CRITICAL severity cho ESCALATE
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("decision", "ESCALATE");
        metadata.put("targetType", reportCase.getTargetType().name());
        metadata.put("targetId", reportCase.getTargetId());
        metadata.put("reason", request.getReason());
        metadata.put("escalatedBy", moderatorId);
        auditLogService.logCritical(
                AuditAction.REPORT_CASE_ESCALATED,
                "REPORT_CASE",
                caseId,
                moderatorId,
                moderatorUsername,
                "MODERATOR",
                "Escalate ReportCase " + caseId + " lên Admin - Lý do: " + request.getReason(),
                metadata
        );

        log.info("[ReportCase] Case {} ESCALATED by moderator {} - reason: {}", caseId, moderatorUsername, request.getReason());
        return toResponse(saved, resolveUsername(saved.getReporterId()), moderatorUsername);
    }

    // ===== Stats =====

    @Override
    public ReportCaseStatsRes getStats() {
        long pendingTotal = reportCaseRepository.countByStatus(ReportCaseStatus.PENDING);
        long resolvedTotal = reportCaseRepository.countByStatus(ReportCaseStatus.RESOLVED);
        long escalatedTotal = reportCaseRepository.countByStatus(ReportCaseStatus.ESCALATED);

        Map<String, Long> pendingByTarget = new HashMap<>();
        for (ReportTargetType type : ReportTargetType.values()) {
            pendingByTarget.put(type.name(),
                    reportCaseRepository.countByTargetTypeAndStatus(type, ReportCaseStatus.PENDING));
        }

        // Đếm resolved hôm nay (cần query thêm)
        long resolvedToday = countResolvedToday();

        return ReportCaseStatsRes.builder()
                .pendingTotal(pendingTotal)
                .pendingByTargetType(pendingByTarget)
                .resolvedTotal(resolvedTotal)
                .escalatedTotal(escalatedTotal)
                .resolvedToday(resolvedToday)
                .build();
    }

    private long countResolvedToday() {
        // Có thể thêm method vào repository nếu cần query riêng
        return 0L;
    }

    // ===== Helper methods =====

    private ReportCase getPendingCaseOrThrow(String caseId, String moderatorUsername) {
        ReportCase reportCase = reportCaseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ReportCase: " + caseId));

        if (reportCase.getStatus() != ReportCaseStatus.PENDING) {
            throw new BadRequestException(
                    "ReportCase không ở trạng thái PENDING (hiện tại: " + reportCase.getStatus() +
                            "). Chỉ có thể xử lý case PENDING.");
        }
        return reportCase;
    }

    private void validateTargetExists(ReportTargetType targetType, String targetId) {
        switch (targetType) {
            case REVIEW -> {
                if (!reviewRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("Không tìm thấy Review: " + targetId);
                }
            }
            case PRODUCT -> {
                if (!productRepository.existsById(targetId)) {
                    throw new ResourceNotFoundException("Không tìm thấy Product: " + targetId);
                }
            }
            case COMPLAINT -> {
                // TODO: implement Complaint entity
                log.warn("[ReportCase] COMPLAINT target type chưa được implement đầy đủ");
            }
        }
    }

    private String snapshotTarget(ReportTargetType targetType, String targetId) {
        switch (targetType) {
            case REVIEW -> {
                return reviewRepository.findById(targetId)
                        .map(r -> String.format("Review[%s] rating=%d comment=\"%s\"",
                                r.getId(), r.getRating(),
                                r.getComment() != null ? r.getComment().substring(0, Math.min(50, r.getComment().length())) : ""))
                        .orElse(null);
            }
            case PRODUCT -> {
                return productRepository.findById(targetId)
                        .map(p -> "Product[" + p.getId() + "] name=\"" + p.getName() + "\"")
                        .orElse(null);
            }
            default -> {
                return null;
            }
        }
    }

    private void updateTargetToPendingManual(ReportTargetType targetType, String targetId) {
        switch (targetType) {
            case REVIEW -> {
                reviewRepository.findById(targetId).ifPresent(review -> {
                    review.setModerationStatus(ReviewModerationStatus.REPORTED);
                    review.setModeratedAt(LocalDateTime.now());
                    reviewRepository.save(review);
                });
            }
            case PRODUCT -> {
                // Product giữ nguyên status hiện tại, không tự động chuyển sang HIDDEN
                // vì cần Moderator quyết định
                log.info("[ReportCase] Product {} flagged for manual review", targetId);
            }
            case COMPLAINT -> {
                log.info("[ReportCase] Complaint {} flagged for manual review", targetId);
            }
        }
    }

    private String getCurrentTargetStatus(ReportTargetType targetType, String targetId) {
        return switch (targetType) {
            case REVIEW -> reviewRepository.findById(targetId)
                    .map(r -> r.getModerationStatus() != null ? r.getModerationStatus().name() : "VISIBLE")
                    .orElse("UNKNOWN");
            case PRODUCT -> productRepository.findById(targetId)
                    .map(p -> p.getStatus() != null ? p.getStatus().name() : "UNKNOWN")
                    .orElse("UNKNOWN");
            default -> "UNKNOWN";
        };
    }

    private String approveTarget(ReportTargetType targetType, String targetId) {
        return switch (targetType) {
            case REVIEW -> {
                reviewRepository.findById(targetId).ifPresent(review -> {
                    review.setModerationStatus(ReviewModerationStatus.VISIBLE);
                    review.setModeratedAt(LocalDateTime.now());
                    review.setModerationReason(null);
                    review.setModeratedBy("MODERATOR_APPROVED");
                    reviewRepository.save(review);
                });
                yield "VISIBLE";
            }
            case PRODUCT -> {
                productRepository.findById(targetId).ifPresent(product -> {
                    product.setStatus(com.ecommerce.cnj70.enums.ProductStatus.ACTIVE);
                    productRepository.save(product);
                });
                yield "ACTIVE";
            }
            case COMPLAINT -> "PENDING";
        };
    }

    private String rejectTarget(ReportTargetType targetType, String targetId, String reason) {
        return switch (targetType) {
            case REVIEW -> {
                reviewRepository.findById(targetId).ifPresent(review -> {
                    review.setModerationStatus(ReviewModerationStatus.HIDDEN);
                    review.setModerationReason(reason);
                    review.setModeratedAt(LocalDateTime.now());
                    review.setModeratedBy("MODERATOR_REJECTED");
                    reviewRepository.save(review);
                });
                yield "HIDDEN";
            }
            case PRODUCT -> {
                productRepository.findById(targetId).ifPresent(product -> {
                    product.setStatus(com.ecommerce.cnj70.enums.ProductStatus.HIDDEN);
                    productRepository.save(product);
                });
                yield "HIDDEN";
            }
            case COMPLAINT -> "REJECTED";
        };
    }

    private String resolveUsername(String userId) {
        if (userId == null || userId.isBlank()) return null;
        try {
            return userRepository.findById(userId).map(u -> u.getFullName()).orElse(userId);
        } catch (Exception e) {
            return userId;
        }
    }

    private ReportCaseRes toResponse(ReportCase reportCase, String reporterUsername, String moderatorUsername) {
        return ReportCaseRes.builder()
                .id(reportCase.getId())
                .targetType(reportCase.getTargetType())
                .targetId(reportCase.getTargetId())
                .targetSnapshot(reportCase.getTargetSnapshot())
                .reporterId(reportCase.getReporterId())
                .reporterUsername(reporterUsername)
                .source(reportCase.getSource())
                .reason(reportCase.getReason())
                .description(reportCase.getDescription())
                .status(reportCase.getStatus())
                .decision(reportCase.getDecision())
                .decisionReason(reportCase.getDecisionReason())
                .note(reportCase.getNote())
                .assignedModeratorId(reportCase.getAssignedModeratorId())
                .assignedModeratorUsername(moderatorUsername)
                .autoFlags(reportCase.getAutoFlags())
                .evidence(reportCase.getEvidence())
                .targetStatusBefore(reportCase.getTargetStatusBefore())
                .targetStatusAfter(reportCase.getTargetStatusAfter())
                .priority(reportCase.getPriority())
                .createdAt(reportCase.getCreatedAt())
                .assignedAt(reportCase.getAssignedAt())
                .resolvedAt(reportCase.getResolvedAt())
                .updatedAt(reportCase.getUpdatedAt())
                .build();
    }
}
