package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ViolationRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.ViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {

    private final ViolationRepository violationRepository;
    private final MongoTemplate mongoTemplate;
    private final AuditLogService auditLogService;

    @Override
    public Page<Violation> getViolationsByShopId(String shopId, Pageable pageable) {
        return violationRepository.findByShopId(shopId, pageable);
    }

    @Override
    public Page<Violation> listViolations(String shopId, ViolationSeverity severity, ViolationType type, Pageable pageable) {
        boolean hasShop = StringUtils.hasText(shopId);
        boolean hasSeverity = severity != null;
        boolean hasType = type != null;

        // Filter composition rules (Phase 2 Fix BUG-V1):
        //   - hasShop + hasSeverity + hasType → all three
        //   - hasShop + hasSeverity            → shop + severity
        //   - hasShop + hasType                → shop + type
        //   - hasShop only                     → shop
        //   - hasSeverity + hasType            → severity + type
        //   - hasSeverity only                 → severity
        //   - hasType only                     → type
        //   - none                             → all
        if (hasShop && hasSeverity && hasType) {
            // Repository has no (shop + severity + type) combo —
            // narrow by (shop + severity) then filter type in memory (small set, OK).
            Page<Violation> narrowed = violationRepository.findByShopIdAndSeverity(shopId, severity, pageable);
            List<Violation> filtered = narrowed.getContent().stream()
                    .filter(v -> v.getType() == type)
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }
        if (hasShop && hasSeverity) {
            return violationRepository.findByShopIdAndSeverity(shopId, severity, pageable);
        }
        if (hasShop && hasType) {
            return violationRepository.findByShopIdAndType(shopId, type, pageable);
        }
        if (hasShop) {
            return violationRepository.findByShopId(shopId, pageable);
        }
        if (hasSeverity && hasType) {
            return violationRepository.findBySeverityAndType(severity, type, pageable);
        }
        if (hasSeverity) {
            return violationRepository.findBySeverity(severity, pageable);
        }
        if (hasType) {
            return violationRepository.findByType(type, pageable);
        }
        return violationRepository.findAll(pageable);
    }

    @Override
    public long countActiveViolations(String shopId) {
        return violationRepository.countByShopIdAndResolvedAtIsNull(shopId);
    }

    @Override
    public long countCriticalActiveViolations(String shopId) {
        List<ViolationSeverity> criticals = List.of(ViolationSeverity.HIGH, ViolationSeverity.CRITICAL);
        return violationRepository.countByShopIdAndSeverityInAndResolvedAtIsNull(shopId, criticals);
    }

    @Override
    public Violation createViolation(Violation violation) {
        if (!StringUtils.hasText(violation.getShopId())) {
            throw new IllegalArgumentException("shopId is required");
        }
        if (violation.getType() == null) {
            violation.setType(ViolationType.WARNING);
        }
        if (violation.getSeverity() == null) {
            violation.setSeverity(ViolationSeverity.MEDIUM);
        }
        violation.setResolvedAt(null);
        violation.setResolvedBy(null);
        Violation saved = violationRepository.save(violation);

        // ===== PHASE J — Audit log VIOLATION_CREATED =====
        // Mapping action theo AuditAction enum hiện có (không thêm enum mới).
        // Severity INFO cho violation thường, CRITICAL cho violation CRITICAL
        // để Admin Audit Log Viewer lọc được.
        try {
            AuditSeverity severity = saved.getSeverity() == ViolationSeverity.CRITICAL
                    ? AuditSeverity.CRITICAL
                    : AuditSeverity.WARNING;
            String reason = "Tạo violation cho shop=" + saved.getShopId()
                    + " type=" + saved.getType()
                    + " severity=" + saved.getSeverity()
                    + (saved.getProductId() != null
                        ? " product=" + saved.getProductId()
                        : (saved.getOrderId() != null
                            ? " order=" + saved.getOrderId()
                            : ""));
            auditLogService.log(
                    AuditAction.VIOLATION_CREATED,
                    "VIOLATION",
                    saved.getId(),
                    saved.getResolvedBy(),   // actor (null nếu auto/system)
                    null,                    // actorUsername — không có context ở đây
                    "SYSTEM",                // role best-effort: SYSTEM cho auto/admin không truyền
                    severity,
                    reason,
                    null);
        } catch (Exception ex) {
            // Audit log lỗi KHÔNG được phá flow business — đã lưu Violation thành công.
            log.warn("Failed to write VIOLATION_CREATED audit for violation {}: {}",
                    saved.getId(), ex.getMessage());
        }

        return saved;
    }

    @Override
    public Violation resolveViolation(String violationId, String resolvedBy, String note) {
        Violation violation = getById(violationId);
        if (violation.getResolvedAt() != null) {
            log.info("Violation {} already resolved, skip", violationId);
            return violation;
        }
        LocalDateTime now = LocalDateTime.now();
        violation.setResolvedAt(now);
        violation.setResolvedBy(resolvedBy);
        violation.setResolutionNote(note);
        Violation saved = violationRepository.save(violation);

        // ===== PHASE J — Audit log VIOLATION_RESOLVED =====
        try {
            String reason = "Resolve violation shop=" + saved.getShopId()
                    + " type=" + saved.getType()
                    + " severity=" + saved.getSeverity()
                    + (note != null && !note.isBlank() ? " note=" + note : "");
            auditLogService.logInfo(
                    AuditAction.VIOLATION_RESOLVED,
                    "VIOLATION",
                    saved.getId(),
                    resolvedBy,
                    null,
                    resolvedBy != null ? "ADMIN" : "SYSTEM",
                    reason);
        } catch (Exception ex) {
            // KHÔNG throw — Violation đã được resolve thành công trước đó.
            log.warn("Failed to write VIOLATION_RESOLVED audit for violation {}: {}",
                    violationId, ex.getMessage());
        }

        return saved;
    }

    @Override
    public Violation getById(String violationId) {
        if (!StringUtils.hasText(violationId)) {
            throw new ResourceNotFoundException("Violation ID không hợp lệ");
        }
        // Phase 6 fix: handle BOTH String _id and ObjectId _id storage. MongoDB
        // Spring Data lưu _id kiểu ObjectId cho String @Id fields nếu chuỗi
        // hợp lệ 24-hex. Phải thử cả hai dạng khi lookup.
        org.bson.Document raw = null;
        // First try String _id (legacy)
        raw = mongoTemplate.getCollection("violations")
                .find(new Document("_id", violationId))
                .first();
        if (raw == null && org.bson.types.ObjectId.isValid(violationId)) {
            try {
                raw = mongoTemplate.getCollection("violations")
                        .find(new Document("_id", new org.bson.types.ObjectId(violationId)))
                        .first();
            } catch (IllegalArgumentException ignored) {
                // not a valid ObjectId, leave raw == null
            }
        }
        if (raw == null) {
            throw new ResourceNotFoundException("Không tìm thấy violation");
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", Violation.class.getName());
        }
        return mongoTemplate.getConverter().read(Violation.class, raw);
    }
}
