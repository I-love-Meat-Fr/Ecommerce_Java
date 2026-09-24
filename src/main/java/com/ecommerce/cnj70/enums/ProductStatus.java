package com.ecommerce.cnj70.enums;

/**
 * Trạng thái của Product trong vòng đời.
 *
 * <p>Luồng (xem {@code AutoModerationService} - pipeline C1):
 * <pre>
 *   Vendor tạo Product
 *      ↓
 *   PENDING_AUTO                     ← ép buộc từ pipeline, không vendor đặt
 *      ↓
 *   ┌─────────┴───────────┐
 *   ↓                     ↓
 *   ACTIVE        MANUAL_REVIEW ── Moderator xử lý → ACTIVE / HIDDEN
 *   ↑                 ↑
 *   │                 └─ Auto check phát hiện nghi vấn
 *   │
 *   └─ REJECTED_AUTO (terminal)     ← Auto check phát hiện vi phạm rõ ràng
 *   └─ HIDDEN                        ← Admin/Moderator ẩn
 *   └─ OUT_OF_STOCK                  ← Stock về 0
 *   └─ DRAFT                         ← Vendor đang soạn (chưa submit - hiện không dùng nhiều)
 * </pre>
 *
 * <p><b>Quy tắc C1:</b> Vendor KHÔNG được bypass pipeline bằng cách set thẳng
 * Product thành ACTIVE. Mọi path đến ACTIVE đều phải qua
 * {@code AutoModerationService.runProductChecks(Product)}.
 *
 * <p>Các trạng thái {@code PENDING_AUTO}, {@code MANUAL_REVIEW},
 * {@code REJECTED_AUTO} được thêm vào task C1 — không có nghĩa tách hệ thống
 * status song song; chúng tích hợp vào enum hiện tại.
 */
public enum ProductStatus {

    /** Vendor đang soạn, chưa submit (legacy / hiện không phát sinh qua flow C1). */
    DRAFT,

    /** C1 — Product vừa được vendor submit, đang chạy Auto Moderation Pipeline. */
    PENDING_AUTO,

    /** Product đang live trên site — đã pass Auto Moderation. */
    ACTIVE,

    /** Hết hàng (do cập nhật stock, không phải moderation). */
    OUT_OF_STOCK,

    /** C1 — Auto Moderation phát hiện dấu hiệu đáng ngờ, cần Moderator review.
     *  Hệ thống tự tạo ReportCase tương ứng. */
    MANUAL_REVIEW,

    /** C1 — Auto Moderation phát hiện vi phạm rõ ràng, auto reject.
     *  Vendor thấy product bị reject và lý do. */
    REJECTED_AUTO,

    /** Product bị ẩn (Admin hide hoặc Moderator reject ReportCase). */
    HIDDEN
}
