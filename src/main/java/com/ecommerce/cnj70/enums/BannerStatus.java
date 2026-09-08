package com.ecommerce.cnj70.enums;

/**
 * Phase 17 — Banner / PR status constants.
 *
 * Phase 17 đã chốt contract rõ 2 giá trị (Task 17.4 Visibility / Publish Contract):
 *   - PUBLISHED: Customer thấy được
 *   - UNPUBLISHED: Ẩn khỏi Customer Home
 *
 * KHÔNG tự thêm status mới (LOCK 4 Phase 17) — chờ Business Contract.
 */
public final class BannerStatus {

    public static final String PUBLISHED = "PUBLISHED";
    public static final String UNPUBLISHED = "UNPUBLISHED";

    private BannerStatus() {
    }

    public static boolean isPublished(String status) {
        return PUBLISHED.equalsIgnoreCase(status);
    }
}
