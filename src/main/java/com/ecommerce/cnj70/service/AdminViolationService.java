package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationResourceType;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 3C — Admin read-only Violation monitoring service.
 *
 * <p>Contract: Admin can VIEW / SEARCH / FILTER / PAGINATE violations.
 * Admin CANNOT create, update, or delete violation records.</p>
 *
 * <p>Note: Violation document does NOT have a 'status' field.
 * Active/open violations are identified by resolvedAt == null.
 * ViolationStatus enum (OPEN/UNDER_REVIEW/etc) is a Phase 3C concept
 * NOT persisted on the Violation document.</p>
 */
public interface AdminViolationService {

    Page<Violation> listViolations(String q,
                                  ViolationSeverity severityFilter,
                                  ViolationResourceType resourceTypeFilter,
                                  Boolean activeOnly,
                                  Pageable pageable);

    Violation getDetail(String violationId);
}
