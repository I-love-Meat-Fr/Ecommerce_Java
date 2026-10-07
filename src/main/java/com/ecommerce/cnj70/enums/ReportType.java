package com.ecommerce.cnj70.enums;

/**
 * PHASE 5 — Loại báo cáo mà Admin có thể sinh từ /admin/reports.
 *
 * <p>Mỗi loại tương ứng với một nhóm dữ liệu và một {@code AdminReportService}
 * method. URL pattern: {@code /admin/reports/{type}?format=csv|xlsx|pdf}.</p>
 */
public enum ReportType {

    /** Danh sách đơn hàng (filter theo status, khoảng ngày). */
    ORDERS("Đơn hàng"),

    /** Doanh thu GMV theo ngày/tuần/tháng (filter theo period). */
    REVENUE("Doanh thu"),

    /**
     * Báo cáo tài chính tổng hợp (GMV, Platform Revenue, Vendor Sales,
     * Vendor Payable, Refund) — theo Phase 4A.
     */
    FINANCIAL("Tài chính"),

    /** Danh sách người dùng (filter theo role, status). */
    USERS("Người dùng"),

    /** Danh sách cửa hàng (filter theo status, KYC). */
    SHOPS("Cửa hàng");

    private final String displayName;

    ReportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse từ path variable, case-insensitive. Trả {@link #ORDERS} nếu input null/blank/không hợp lệ.
     */
    public static ReportType parse(String raw) {
        if (raw == null || raw.isBlank()) return ORDERS;
        try {
            return ReportType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ORDERS;
        }
    }
}