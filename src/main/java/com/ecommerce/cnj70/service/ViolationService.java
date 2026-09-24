package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ViolationService {

    /** Lấy lịch sử violation của 1 shop (cho vendor xem) */
    Page<Violation> getViolationsByShopId(String shopId, Pageable pageable);

    /** Lấy tất cả violations có thể lọc theo severity/type (cho admin) */
    Page<Violation> listViolations(String shopId, ViolationSeverity severity, ViolationType type, Pageable pageable);

    /** Đếm số violation chưa xử lý của shop */
    long countActiveViolations(String shopId);

    /** Đếm số violation critical chưa xử lý (HIGH + CRITICAL) - dùng để chặn bán */
    long countCriticalActiveViolations(String shopId);

    /** Tạo violation mới (admin) */
    Violation createViolation(Violation violation);

    /** Đánh dấu violation đã xử lý */
    Violation resolveViolation(String violationId, String resolvedBy, String note);

    /** Lấy violation theo ID */
    Violation getById(String violationId);
}
