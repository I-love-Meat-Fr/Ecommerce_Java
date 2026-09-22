package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * TASK #26 — ReportCase Repository.
 */
@Repository
public interface ReportCaseRepository extends MongoRepository<ReportCase, String> {

    /** Tìm các case đang chờ xử lý (cho Moderator Queue) */
    Page<ReportCase> findByStatusOrderByPriorityDescCreatedAtAsc(ReportCaseStatus status, Pageable pageable);

    /** Tìm theo status (cho filter) */
    Page<ReportCase> findByStatus(ReportCaseStatus status, Pageable pageable);

    /** Tìm theo status + targetType */
    Page<ReportCase> findByStatusAndTargetType(ReportCaseStatus status, ReportTargetType targetType, Pageable pageable);

    /** Tìm theo targetType (lịch sử của một target) */
    Page<ReportCase> findByTargetTypeAndTargetId(ReportTargetType targetType, String targetId, Pageable pageable);

    /** Tìm theo moderator đã xử lý */
    Page<ReportCase> findByAssignedModeratorIdOrderByResolvedAtDesc(String assignedModeratorId, Pageable pageable);

    /** Tìm case đang pending cho target cụ thể (tránh duplicate) */
    Optional<ReportCase> findFirstByTargetTypeAndTargetIdAndStatus(
            ReportTargetType targetType, String targetId, ReportCaseStatus status);

    /** Đếm số case pending (cho Dashboard) */
    long countByStatus(ReportCaseStatus status);

    /** Đếm case theo targetType + status */
    long countByTargetTypeAndStatus(ReportTargetType targetType, ReportCaseStatus status);

    /** Lấy tất cả case của một reporter */
    List<ReportCase> findByReporterIdOrderByCreatedAtDesc(String reporterId);
}
