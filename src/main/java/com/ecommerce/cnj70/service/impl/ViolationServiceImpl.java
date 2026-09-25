package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ViolationRepository;
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

    @Override
    public Page<Violation> getViolationsByShopId(String shopId, Pageable pageable) {
        return violationRepository.findByShopId(shopId, pageable);
    }

    @Override
    public Page<Violation> listViolations(String shopId, ViolationSeverity severity, ViolationType type, Pageable pageable) {
        boolean hasShop = StringUtils.hasText(shopId);
        boolean hasSeverity = severity != null;
        boolean hasType = type != null;

        if (hasShop && hasSeverity) {
            return violationRepository.findByShopIdAndSeverity(shopId, severity, pageable);
        }
        if (hasShop && hasType) {
            return violationRepository.findByShopIdAndType(shopId, type, pageable);
        }
        if (hasShop) {
            return violationRepository.findByShopId(shopId, pageable);
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
        return violationRepository.save(violation);
    }

    @Override
    public Violation resolveViolation(String violationId, String resolvedBy, String note) {
        Violation violation = getById(violationId);
        if (violation.getResolvedAt() != null) {
            log.info("Violation {} already resolved, skip", violationId);
            return violation;
        }
        violation.setResolvedAt(LocalDateTime.now());
        violation.setResolvedBy(resolvedBy);
        violation.setResolutionNote(note);
        return violationRepository.save(violation);
    }

    @Override
    public Violation getById(String violationId) {
        if (!StringUtils.hasText(violationId)) {
            throw new ResourceNotFoundException("Violation ID không hợp lệ");
        }
        // Phase 6 fix: String _id retrieval — same pattern as Phase 5 fix.
        // Use raw mongoTemplate query for reliable String _id lookup.
        Document raw = mongoTemplate.getCollection("violations")
                .find(new Document("_id", violationId))
                .first();
        if (raw == null) {
            throw new ResourceNotFoundException("Không tìm thấy violation");
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", Violation.class.getName());
        }
        return mongoTemplate.getConverter().read(Violation.class, raw);
    }
}
