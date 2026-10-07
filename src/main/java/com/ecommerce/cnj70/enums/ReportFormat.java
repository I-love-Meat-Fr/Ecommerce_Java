package com.ecommerce.cnj70.enums;

/**
 * PHASE 5 — Admin Report export format.
 *
 * <p>Định dạng file mà Admin chọn khi sinh báo cáo. Mỗi giá trị map sang:</p>
 * <ul>
 *   <li>{@link #CSV}  — text/plain, content-type {@code text/csv}</li>
 *   <li>{@link #XLSX} — Excel Open XML, content-type {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}</li>
 *   <li>{@link #PDF}  — Portable Document Format, content-type {@code application/pdf}</li>
 * </ul>
 *
 * <p>Việc tách enum giúp controller/service validate input một chỗ duy nhất, đảm bảo
 * URL query param {@code format} chỉ nhận 1 trong 3 giá trị này — chống injection /
 * nhập linh tinh gây lỗi runtime.</p>
 */
public enum ReportFormat {
    CSV,
    XLSX,
    PDF;

    /**
     * Parse từ query string, case-insensitive. Trả {@link #CSV} nếu input null/blank/không hợp lệ
     * (CSV là format nhẹ nhất, fallback an toàn khi user nhập sai).
     */
    public static ReportFormat parse(String raw) {
        if (raw == null || raw.isBlank()) return CSV;
        try {
            return ReportFormat.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CSV;
        }
    }

    /**
     * MIME type cho HTTP {@code Content-Type} header khi trả file về browser.
     */
    public String contentType() {
        switch (this) {
            case CSV:  return "text/csv; charset=UTF-8";
            case XLSX: return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case PDF:  return "application/pdf";
            default:   return "application/octet-stream";
        }
    }

    /**
     * File extension (không có dấu chấm).
     */
    public String extension() {
        return name().toLowerCase();
    }
}