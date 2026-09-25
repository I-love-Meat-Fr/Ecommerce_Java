package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.enums.ComplaintLevel;
import com.ecommerce.cnj70.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends MongoRepository<Complaint, String> {

    Page<Complaint> findByCustomerId(String customerId, Pageable pageable);

    Page<Complaint> findByShopId(String shopId, Pageable pageable);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByLevel(ComplaintLevel level, Pageable pageable);

    List<Complaint> findByShopIdAndStatus(String shopId, ComplaintStatus status);

    /**
     * Phase 3 §31-§34 — Scheduler dùng để tìm complaint Level 0 quá hạn.
     * Idempotent: status = OPEN/VENDOR_RESPONDED + deadline &lt; now.
     */
    List<Complaint> findByStatusInAndVendorResponseDeadlineBefore(
            List<ComplaintStatus> statuses, LocalDateTime cutoff);

    /**
     * Phase 3 §34 — Scheduler dùng để tìm complaint Level 1 quá hạn.
     */
    List<Complaint> findByLevelAndModeratorResolutionDeadlineBefore(
            ComplaintLevel level, LocalDateTime cutoff);
}
