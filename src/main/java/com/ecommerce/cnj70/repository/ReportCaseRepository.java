package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportReason;
import com.ecommerce.cnj70.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ReportCaseRepository extends MongoRepository<ReportCase, String> {

    // ===== TASK #26: Legacy methods using targetType/targetId =====

    /** Tim các case đang chờ xử lý (cho Moderator Queue) */
    Page<ReportCase> findByStatusOrderByPriorityDescCreatedAtAsc(ReportCaseStatus status, Pageable pageable);

    /** Tim theo status (cho filter) */
    Page<ReportCase> findByStatus(ReportCaseStatus status, Pageable pageable);

    /** Tim theo status + targetType */
    Page<ReportCase> findByStatusAndTargetType(ReportCaseStatus status, ReportTargetType targetType, Pageable pageable);

    /** Tim theo targetType (lịch sử của một target) */
    Page<ReportCase> findByTargetTypeAndTargetId(ReportTargetType targetType, String targetId, Pageable pageable);

    /** Tim theo moderator đã xử lý */
    Page<ReportCase> findByAssignedModeratorIdOrderByResolvedAtDesc(String assignedModeratorId, Pageable pageable);

    /** Tim case đang pending cho target cụ thể (tránh duplicate) */
    Optional<ReportCase> findFirstByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType, String targetId, ReportCaseStatus status);

    /** Đếm số case pending (cho Dashboard) */
    long countByStatus(ReportCaseStatus status);

    /** Đếm case theo targetType + status */
    long countByTargetTypeAndStatus(ReportTargetType targetType, ReportCaseStatus status);

    /** Lấy tất cả case của một reporter */
    List<ReportCase> findByReporterIdOrderByCreatedAtDesc(String reporterId);

    // ===== Phase 2C: Methods using resourceType/resourceId =====

    Page<ReportCase> findByStatusIn(Set<ReportCaseStatus> statuses, Pageable pageable);

    Page<ReportCase> findByStatusInAndResourceType(
            Set<ReportCaseStatus> statuses, ReportCaseResourceType resourceType, Pageable pageable);

    Page<ReportCase> findByStatusInAndReason(
            Set<ReportCaseStatus> statuses, ReportReason reason, Pageable pageable);

    Page<ReportCase> findByStatusInAndReasonAndResourceType(
            Set<ReportCaseStatus> statuses, ReportReason reason,
            ReportCaseResourceType resourceType, Pageable pageable);

    List<ReportCase> findByResourceTypeAndResourceIdOrderByCreatedAtDesc(
            ReportCaseResourceType resourceType, String resourceId);

    List<ReportCase> findByVendorIdOrderByCreatedAtDesc(String vendorId);
}
