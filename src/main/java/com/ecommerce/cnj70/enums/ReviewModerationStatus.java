package com.ecommerce.cnj70.enums;

/**
 * TASK #15 — Trạng thái moderation của Review.
 *
 * Luồng:
 * - VISIBLE:        Review hiển thị bình thường
 * - REPORTED:       Review bị report, đang chờ Moderator xử lý
 * - HIDDEN:         Review bị ẩn khỏi public (Moderator quyết định)
 * - DELETED:        Review bị xóa vĩnh viễn (Admin quyết định)
 */
public enum ReviewModerationStatus {
    VISIBLE,
    REPORTED,
    HIDDEN,
    DELETED
}
