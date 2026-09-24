package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationResourceType;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ViolationRepository;
import com.ecommerce.cnj70.service.AdminViolationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Phase 3C — Admin Violation monitoring service (read-only).
 *
 * <p>Implements {@link AdminViolationService}. All operations are
 * read-only — no mutation is performed on {@link Violation} records.</p>
 *
 * <h3>Domain Model Note</h3>
 * <p>Violation document uses {@code resolvedAt == null} to represent an active/open
 * violation (Phase 3C does NOT add a status field to Violation). The ViolationStatus
 * enum (OPEN/UNDER_REVIEW/RESOLVED/etc) is a Phase 3C concept that is NOT persisted
 * on the Violation document. All filtering is done via resolvedAt-based queries
 * (active = resolvedAt IS NULL, resolved = resolvedAt IS NOT NULL) and in-memory
 * filtering for severity/type.</p>
 *
 * <h3>Security</h3>
 * <ul>
 *     <li>Route-level: SecurityConfig.hasRole("ADMIN")</li>
 *     <li>Object-level: {@code getDetail} validates existence; returns 404 for missing IDs</li>
 *     <li>No write methods exist in this service</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminViolationServiceImpl implements AdminViolationService {

    private final ViolationRepository violationRepository;

    @Override
    public Page<Violation> listViolations(String q,
                                          ViolationSeverity severityFilter,
                                          ViolationResourceType resourceTypeFilter,
                                          Boolean activeOnly,
                                          Pageable pageable) {
        // Violation uses resolvedAt == null for active/open state.
        // No 'status' field exists on Violation document.
        // Phase 3C filtering is done in-memory after fetching records.
        Page<Violation> page = violationRepository.findAll(pageable);

        List<Violation> filtered = page.getContent().stream()
                .filter(violation -> {
                    if (activeOnly != null && activeOnly) {
                        if (violation.getResolvedAt() != null) return false;
                    }
                    if (severityFilter != null) {
                        if (violation.getSeverity() != severityFilter) return false;
                    }
                    if (resourceTypeFilter != null) {
                        boolean matches = switch (resourceTypeFilter) {
                            case PRODUCT -> violation.getProductId() != null;
                            case SHOP, VENDOR -> violation.getProductId() == null;
                            default -> true;
                        };
                        if (!matches) return false;
                    }
                    return true;
                })
                .toList();

        return new org.springframework.data.domain.PageImpl<>(
                filtered, pageable, page.getTotalElements());
    }

    @Override
    public Violation getDetail(String violationId) {
        return violationRepository.findById(violationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Violation với ID: " + violationId));
    }
}
