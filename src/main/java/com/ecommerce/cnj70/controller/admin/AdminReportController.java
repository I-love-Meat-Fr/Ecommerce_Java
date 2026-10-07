package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.enums.ReportFormat;
import com.ecommerce.cnj70.enums.ReportType;
import com.ecommerce.cnj70.service.AdminReportService;
import com.ecommerce.cnj70.service.ReportExportService;
import com.ecommerce.cnj70.service.ReportFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PHASE 5 — Admin Report Controller.
 *
 * <p>Cung cấp 2 nhóm endpoint:</p>
 * <ol>
 *   <li><b>{@code GET /admin/reports}</b> — trang HTML liệt kê các loại báo cáo + filter UI.</li>
 *   <li><b>{@code GET /admin/reports/{type}/export}</b> — sinh file PDF/Excel/CSV.</li>
 * </ol>
 *
 * <p>Tất cả endpoint chỉ cho {@code ADMIN} truy cập (cấu hình ở {@link com.ecommerce.cnj70.config.SecurityConfig}).
 * Dữ liệu tài chính / doanh thu là thông tin nhạy cảm, không nên để Moderator xem.</p>
 *
 * <h3>Quy ước query param</h3>
 * <ul>
 *   <li>{@code format=csv|xlsx|pdf} (mặc định: csv)</li>
 *   <li>{@code fromDate=2026-09-01}, {@code toDate=2026-09-30} (ISO 8601 yyyy-MM-dd, optional)</li>
 *   <li>{@code status}, {@code period}, {@code role}, {@code shopStatus}, {@code kycStatus},
 *       {@code accountStatus}, {@code q} — tùy theo {@code type}, optional.</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;
    private final ReportExportService reportExportService;

    /**
     * Trang landing cho chức năng sinh báo cáo. Hiển thị filter UI cho từng loại báo cáo.
     */
    @GetMapping
    public String reportPage(@RequestParam(value = "type", required = false) String typeStr,
                             @RequestParam(value = "format", required = false) String formatStr,
                             @RequestParam(value = "fromDate", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                             @RequestParam(value = "toDate", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "period", required = false) String period,
                             @RequestParam(value = "role", required = false) String role,
                             @RequestParam(value = "shopStatus", required = false) String shopStatus,
                             @RequestParam(value = "kycStatus", required = false) String kycStatus,
                             @RequestParam(value = "accountStatus", required = false) String accountStatus,
                             @RequestParam(value = "q", required = false) String q,
                             Model model) {
        ReportType type = ReportType.parse(typeStr);
        ReportFormat format = ReportFormat.parse(formatStr);
        ReportFilter filter = ReportFilter.builder()
                .fromDate(fromDate).toDate(toDate)
                .status(blankToNull(status)).period(blankToNull(period))
                .role(blankToNull(role)).shopStatus(blankToNull(shopStatus))
                .kycStatus(blankToNull(kycStatus)).accountStatus(blankToNull(accountStatus))
                .q(blankToNull(q))
                .build();
        ReportFilter normalized = adminReportService.normalizeFilter(filter);

        model.addAttribute("type", type);
        model.addAttribute("format", format);
        model.addAttribute("types", ReportType.values());
        model.addAttribute("formats", ReportFormat.values());
        model.addAttribute("filter", normalized);
        model.addAttribute("headers", adminReportService.headers(type));
        model.addAttribute("rows", adminReportService.buildRows(type, normalized));
        model.addAttribute("suggestedFilename",
                adminReportService.suggestedFilename(type, normalized));
        model.addAttribute("title", adminReportService.defaultTitle(type, normalized));
        model.addAttribute("orderStatuses",
                java.util.Arrays.asList("PENDING", "PREPARING", "SHIPPING", "DELIVERED", "CANCELLED"));
        model.addAttribute("shopStatuses",
                java.util.Arrays.asList("PENDING", "APPROVED", "REJECTED", "SUSPENDED", "RESTRICTED"));
        model.addAttribute("userRoles",
                java.util.Arrays.asList("ADMIN", "MODERATOR", "VENDOR", "CUSTOMER"));
        model.addAttribute("accountStatuses",
                java.util.Arrays.asList("ACTIVE", "LOCKED", "UNVERIFIED"));
        model.addAttribute("periods",
                java.util.Arrays.asList("DAY", "WEEK", "MONTH", "QUARTER", "YEAR", "ALL"));
        model.addAttribute("isAdmin", true);
        return "admin/reports";
    }

    /**
     * Endpoint xuất file PDF/Excel/CSV.
     *
     * <p>Ví dụ:
     * {@code GET /admin/reports/orders/export?format=xlsx&status=DELIVERED&fromDate=2026-09-01&toDate=2026-09-30}</p>
     *
     * <p>Trả về {@code 400} nếu filter invalid (vd: fromDate > toDate mà không swap),
     * {@code 200} với {@code Content-Disposition: attachment} nếu OK.</p>
     */
    @GetMapping("/{type}/export")
    public ResponseEntity<byte[]> export(@org.springframework.web.bind.annotation.PathVariable("type") String typeStr,
                                         @RequestParam(value = "format", required = false) String formatStr,
                                         @RequestParam(value = "fromDate", required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                         @RequestParam(value = "toDate", required = false)
                                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                                         @RequestParam(value = "status", required = false) String status,
                                         @RequestParam(value = "period", required = false) String period,
                                         @RequestParam(value = "role", required = false) String role,
                                         @RequestParam(value = "shopStatus", required = false) String shopStatus,
                                         @RequestParam(value = "kycStatus", required = false) String kycStatus,
                                         @RequestParam(value = "accountStatus", required = false) String accountStatus,
                                         @RequestParam(value = "q", required = false) String q) {
        ReportType type = ReportType.parse(typeStr);
        ReportFormat format = ReportFormat.parse(formatStr);

        ReportFilter filter = ReportFilter.builder()
                .fromDate(fromDate).toDate(toDate)
                .status(blankToNull(status)).period(blankToNull(period))
                .role(blankToNull(role)).shopStatus(blankToNull(shopStatus))
                .kycStatus(blankToNull(kycStatus)).accountStatus(blankToNull(accountStatus))
                .q(blankToNull(q))
                .build();
        ReportFilter normalized = adminReportService.normalizeFilter(filter);

        String title = adminReportService.defaultTitle(type, normalized);
        String[] headers = adminReportService.headers(type);
        List<String[]> rows = adminReportService.buildRows(type, normalized);

        log.info("Admin report export: type={} format={} rows={} userRole=ADMIN",
                type, format, rows.size());

        byte[] body = reportExportService.export(title, headers, rows, format);
        String baseName = adminReportService.suggestedFilename(type, normalized);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.parseMediaType(format.contentType()));
        // Đặt tên file theo RFC 6266 — quote tên để tránh lỗi với non-ASCII.
        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + baseName + "." + format.extension() + "\"");
        // Xlsx/Pdf là binary; CSV đã có charset=UTF-8 trong content-type.
        httpHeaders.setContentLength(body.length);
        // Cache-Control: ngăn browser cache file báo cáo (chứa dữ liệu nhạy cảm).
        httpHeaders.setCacheControl("no-store, no-cache, must-revalidate, max-age=0");

        return new ResponseEntity<>(body, httpHeaders, org.springframework.http.HttpStatus.OK);
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}