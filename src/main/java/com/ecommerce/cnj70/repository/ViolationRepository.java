package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ViolationRepository extends MongoRepository<Violation, String> {

    List<Violation> findByShopIdOrderByCreatedAtDesc(String shopId);

    Page<Violation> findByShopId(String shopId, Pageable pageable);

    Page<Violation> findByShopIdAndSeverity(String shopId, ViolationSeverity severity, Pageable pageable);

    Page<Violation> findByShopIdAndType(String shopId, ViolationType type, Pageable pageable);

    long countByShopId(String shopId);

    long countByShopIdAndResolvedAtIsNull(String shopId);

    /**
     * Phase 1 — Global count of active violations (resolvedAt IS NULL).
     * Replaces inefficient {@code findAll().stream().filter(...).count()} used
     * by AdminServiceImpl.countActiveViolations() / AdminDashboardRes.openViolation.
     * O(1) DB-side count.
     */
    long countByResolvedAtIsNull();

    long countByShopIdAndSeverity(String shopId, ViolationSeverity severity);

    /**
     * Đếm số violation nghiêm trọng chưa được xử lý (dùng để chặn hoạt động bán hàng).
     * Violation active = resolvedAt == null.
     */
    long countByShopIdAndSeverityInAndResolvedAtIsNull(
            String shopId, List<ViolationSeverity> severities);

    // Note: Violation KHÔNG có field 'status'.
    // Domain model dùng resolvedAt == null để biểu diễn active state.
    // Admin query sử dụng resolvedAt-based queries.
    // ViolationStatus enum (Phase 3C) hiện không được sử dụng trên document.
}
