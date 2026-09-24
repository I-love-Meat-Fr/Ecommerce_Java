package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.ReturnRequest;
import com.ecommerce.cnj70.enums.ReturnStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Phase 3A §23 — Return Repository.
 *
 * <p>Persistence layer for {@link ReturnRequest}. Phase 3A chỉ expose query
 * cần thiết cho ownership/security checks. Workflow-specific queries sẽ
 * thuộc Phase 3B/3C theo policy.</p>
 */
@Repository
public interface ReturnRequestRepository extends MongoRepository<ReturnRequest, String> {

    /** Customer xem Return của mình. */
    Page<ReturnRequest> findByCustomerId(String customerId, Pageable pageable);

    /** Vendor xem Return của Shop mình. */
    Page<ReturnRequest> findByShopId(String shopId, Pageable pageable);

    /** Moderator xem queue theo status. */
    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);

    /** Phase 3A §31 — tìm Return theo Complaint liên quan. */
    List<ReturnRequest> findByComplaintId(String complaintId);

    /** Phase 3A §32 — tìm Return theo Order. */
    List<ReturnRequest> findByOrderId(String orderId);
}
