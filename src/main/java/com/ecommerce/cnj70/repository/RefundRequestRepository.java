package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.RefundRequest;
import com.ecommerce.cnj70.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Phase 3A §28 — Refund Repository (read-only persistence layer for the contract).
 */
@Repository
public interface RefundRequestRepository extends MongoRepository<RefundRequest, String> {

    Page<RefundRequest> findByCustomerId(String customerId, Pageable pageable);

    Page<RefundRequest> findByShopId(String shopId, Pageable pageable);

    Page<RefundRequest> findByStatus(RefundStatus status, Pageable pageable);

    List<RefundRequest> findByComplaintId(String complaintId);

    List<RefundRequest> findByReturnId(String returnId);

    List<RefundRequest> findByOrderId(String orderId);
}
