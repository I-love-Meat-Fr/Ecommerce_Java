package com.ecommerce.cnj70.service;

/**
 * PHASE 5 — Generic report export service.
 *
 * <p>Nhận vào một danh sách các hàng dữ liệu (mỗi hàng = {@code String[]})
 * + tiêu đề cột ({@code headers}), sinh ra file {@code CSV / XLSX / PDF}
 * tương ứng với {@link com.ecommerce.cnj70.enums.ReportFormat}.</p>
 *
 * <p>Tách riêng khỏi {@code AdminReportService} (lo dữ liệu) để dễ unit-test:
 * service này không cần biết dữ liệu đến từ MongoDB hay JPA — chỉ làm nhiệm vụ
 * chuyển {@code List&lt;String[]&gt;} + headers thành byte[].</p>
 *
 * <h3>Thiết kế</h3>
 * <ul>
 *   <li><b>CSV</b>: dùng {@link java.io.PrintWriter} với UTF-8 + BOM để Excel
 *       mở không bị lỗi font tiếng Việt.</li>
 *   <li><b>XLSX</b>: dùng Apache POI {@link org.apache.poi.xssf.usermodel.XSSFWorkbook}.
 *       Có header row in đậm, auto-fit column width (best-effort, giới hạn 60 chars).</li>
 *   <li><b>PDF</b>: dùng OpenPDF ({@link com.lowagie.text}). Page A4 landscape, font
 *       Helvetica (mặc định có dấu Latin). Tiêu đề báo cáo + bảng dữ liệu.</li>
 * </ul>
 *
 * @see com.ecommerce.cnj70.service.impl.ReportExportServiceImpl
 */
public interface ReportExportService {

    /**
     * Sinh file báo cáo từ danh sách hàng + header.
     *
     * @param title    tiêu đề báo cáo (vd: "Báo cáo đơn hàng — T9/2026")
     * @param headers  danh sách tên cột (vd: ["ID", "Khách hàng", "Tổng tiền"])
     * @param rows     danh sách các hàng dữ liệu, mỗi hàng là String[] cùng độ dài với headers
     * @param format   {@link com.ecommerce.cnj70.enums.ReportFormat}
     * @return byte[] nội dung file (CSV / XLSX / PDF)
     */
    byte[] export(String title, String[] headers, java.util.List<String[]> rows,
                  com.ecommerce.cnj70.enums.ReportFormat format);
}