package com.ecommerce.cnj70.service;

/**
 * PHASE 5 — Admin Report query service.
 *
 * <p>Truy vấn dữ liệu từ MongoDB theo từng loại báo cáo ({@link com.ecommerce.cnj70.enums.ReportType}),
 * chuẩn hoá thành {@code headers + List<String[]>} để {@link ReportExportService} chuyển sang
 * CSV/XLSX/PDF.</p>
 *
 * <p>Tách riêng {@link ReportExportService} (chỉ lo file format) với {@code AdminReportService}
 * (lo dữ liệu) để tuân thủ SRP — mỗi service có 1 trách nhiệm, unit-test độc lập.</p>
 */
public interface AdminReportService {

    /**
     * Headers (tên cột) của báo cáo. Phải khớp với số cột của {@link #buildRows}.
     */
    String[] headers(com.ecommerce.cnj70.enums.ReportType type);

    /**
     * Tiêu đề báo cáo mặc định (bao gồm filter đang áp dụng, vd: "Báo cáo đơn hàng — 09/2026").
     */
    String defaultTitle(com.ecommerce.cnj70.enums.ReportType type, ReportFilter filter);

    /**
     * Tên file đề xuất (không extension) — dùng cho {@code Content-Disposition}.
     */
    String suggestedFilename(com.ecommerce.cnj70.enums.ReportType type, ReportFilter filter);

    /**
     * Truy vấn dữ liệu theo {@code type} + filter. Trả về danh sách các hàng (mỗi hàng String[]).
     */
    java.util.List<String[]> buildRows(com.ecommerce.cnj70.enums.ReportType type, ReportFilter filter);

    /**
     * Filter dùng cho các query — null-safe.
     */
    ReportFilter normalizeFilter(ReportFilter raw);
}