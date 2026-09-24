package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface EscalationRepository extends MongoRepository<Escalation, String> {

    Page<Escalation> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Phase 3A §14 — queue by status (default: PENDING + IN_REVIEW). */
    Page<Escalation> findByStatusIn(Collection<Escalation.Status> statuses, Pageable pageable);

    Page<Escalation> findByStatusInAndSeverity(
            Collection<Escalation.Status> statuses, EscalationSeverity severity, Pageable pageable);

    Page<Escalation> findByStatusInAndResourceType(
            Collection<Escalation.Status> statuses, ReportCaseResourceType resourceType, Pageable pageable);

    Page<Escalation> findByStatusInAndSeverityAndResourceType(
            Collection<Escalation.Status> statuses, EscalationSeverity severity,
            ReportCaseResourceType resourceType, Pageable pageable);

    // === Phase 4A — count for Admin Dashboard ===

    long countByStatusIn(Collection<Escalation.Status> statuses);

    // === Phase 3B — duplicate escalation protection ===

    /**
     * Find active escalations for a ReportCase.
     * Used to prevent duplicate escalations per Phase 3B §14.
     */
    List<Escalation> findByReportCaseIdAndStatusIn(String reportCaseId, Collection<Escalation.Status> statuses);

    /**
     * Count active escalations for a ReportCase.
     */
    long countByReportCaseIdAndStatusIn(String reportCaseId, Collection<Escalation.Status> statuses);
}
