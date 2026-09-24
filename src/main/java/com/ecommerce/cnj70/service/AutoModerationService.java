package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.service.automation.AutoModerationResult;

/**
 * C1 — Product Auto Moderation Pipeline (orchestrator trung tâm).
 *
 * <p>Trách nhiệm:
 * <ul>
 *   <li>Chạy tất cả bean {@code AutoCheckStrategy} trên Product.</li>
 *   <li>Aggregate verdicts → quyết định {@code ProductStatus} target:
 *       <ul>
 *         <li>PASS                 → ACTIVE</li>
 *         <li>có SOFT_FLAG         → MANUAL_REVIEW (+ tạo ReportCase từ caller)</li>
 *         <li>có HARD_REJECT       → REJECTED_AUTO</li>
 *       </ul>
 *   </li>
 *   <li>KHÔNG tự ý tạo ReportCase — trả {@link AutoModerationResult} về
 *       caller (ProductServiceImpl) để caller quyết định tạo ReportCase hay
 *       không. Điều này giữ separation of concerns.</li>
 * </ul>
 *
 * <p>Mọi check (C2-C6) là component của pipeline này, KHÔNG tạo workflow
 * song song. Khi thêm check mới (vd {@code BlacklistKeywordCheck}), chỉ
 * cần thêm bean implement {@code AutoCheckStrategy}; orchestrator tự động
 * nhặt và chạy.
 */
public interface AutoModerationService {

    /**
     * Chạy pipeline trên Product đã save.
     *
     * <p>Side effects: KHÔNG ghi DB; chỉ trả verdict. Caller chịu trách nhiệm
     * set Product.status theo {@code result.targetStatus} và save.
     *
     * <p>Read-only transaction để các check có thể query DB an toàn.
     *
     * @param product product đã được persist (có id). KHÔNG được null.
     * @return kết quả aggregate (xem {@link AutoModerationResult}).
     */
    AutoModerationResult runProductChecks(Product product);
}
