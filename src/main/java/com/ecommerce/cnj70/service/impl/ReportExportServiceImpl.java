package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.enums.ReportFormat;
import com.ecommerce.cnj70.service.ReportExportService;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * PHASE 5 — Implementation cho {@link ReportExportService}.
 *
 * <p>Sinh file CSV / XLSX / PDF. Tham khảo Javadoc của interface để biết thiết kế tổng thể.</p>
 *
 * <h3>CSV encoding</h3>
 * <p>Xuất UTF-8 kèm BOM {@code \uFEFF} ở đầu file để Microsoft Excel auto-detect UTF-8
 * (mặc định Excel hiểu CSV là ANSI/CP1252 → tiếng Việt bị lỗi font).</p>
 *
 * <h3>Excel auto-fit</h3>
 * <p>Dùng heuristic: width = min(60, max(headerLen, maxCellLen) * 1.2 + 2). Không chính xác
 * 100% với CJK / proportional font nhưng đủ dùng cho báo cáo tiếng Việt.</p>
 *
 * <h3>PDF page</h3>
 * <p>A4 landscape để chứa nhiều cột. Header in đậm. Nếu bảng rộng hơn trang, OpenPDF tự wrap.</p>
 */
@Slf4j
@Service
public class ReportExportServiceImpl implements ReportExportService {

    /** Giới hạn độ rộng cột (chars) khi auto-fit Excel — tránh cột quá rộng chiếm cả màn hình. */
    private static final int MAX_EXCEL_COL_WIDTH = 60;

    @Override
    public byte[] export(String title, String[] headers, List<String[]> rows, ReportFormat format) {
        if (headers == null || headers.length == 0) {
            throw new IllegalArgumentException("headers must not be null or empty");
        }
        if (rows == null) rows = List.of();

        switch (format) {
            case CSV:  return exportCsv(title, headers, rows);
            case XLSX: return exportXlsx(title, headers, rows);
            case PDF:  return exportPdf(title, headers, rows);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    /* ===================== CSV ===================== */

    private byte[] exportCsv(String title, String[] headers, List<String[]> rows) {
        // BOM để Excel auto-detect UTF-8.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0xEF);
        baos.write(0xBB);
        baos.write(0xBF);

        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            if (title != null && !title.isBlank()) {
                // Dòng tiêu đề đầu file CSV (một số tool hiển thị, một số skip).
                pw.println("# " + sanitizeForCsvComment(title));
            }
            pw.println(String.join(",", escapeCsvRow(headers)));
            for (String[] row : rows) {
                // Nếu row ngắn hơn headers → pad rỗng; nếu dài hơn → cắt.
                String[] normalized = normalizeRow(row, headers.length);
                pw.println(String.join(",", escapeCsvRow(normalized)));
            }
            pw.flush();
            return baos.toByteArray();
        }
    }

    /** Escape theo RFC 4180: nếu ô chứa {@code , " \n \r} → bọc trong dấu nháy kép, gấp đôi nháy trong. */
    private static String[] escapeCsvRow(String[] cells) {
        String[] out = new String[cells.length];
        for (int i = 0; i < cells.length; i++) {
            out[i] = escapeCsvCell(cells[i]);
        }
        return out;
    }

    private static String escapeCsvCell(String s) {
        if (s == null) return "";
        boolean needsQuote = s.indexOf(',') >= 0
                || s.indexOf('"') >= 0
                || s.indexOf('\n') >= 0
                || s.indexOf('\r') >= 0;
        if (!needsQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String sanitizeForCsvComment(String s) {
        // Comment line trong CSV chỉ là convention; replace newline để không phá cấu trúc.
        return s.replace('\n', ' ').replace('\r', ' ');
    }

    /* ===================== XLSX (Apache POI) ===================== */

    private byte[] exportXlsx(String title, String[] headers, List<String[]> rows) {
        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet(sanitizeSheetName(title));

            // Dòng 0: tiêu đề báo cáo (merge các cột).
            if (title != null && !title.isBlank()) {
                Row titleRow = sheet.createRow(0);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue(title);
                titleCell.setCellStyle(buildTitleStyle(wb));
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
            }

            int headerRowIdx = (title != null && !title.isBlank()) ? 1 : 0;
            Row headerRow = sheet.createRow(headerRowIdx);
            CellStyle headerStyle = buildHeaderStyle(wb);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers[c] == null ? "" : headers[c]);
                cell.setCellStyle(headerStyle);
            }

            int dataStart = headerRowIdx + 1;
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(dataStart + r);
                String[] normalized = normalizeRow(rows.get(r), headers.length);
                for (int c = 0; c < normalized.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(normalized[c] == null ? "" : normalized[c]);
                }
            }

            // Auto-fit column widths (heuristic dựa trên độ dài text).
            for (int c = 0; c < headers.length; c++) {
                int width = (headers[c] == null ? 8 : headers[c].length());
                for (int r = 0; r < rows.size(); r++) {
                    String[] row = normalizeRow(rows.get(r), headers.length);
                    if (c < row.length && row[c] != null) {
                        width = Math.max(width, row[c].length());
                    }
                }
                int charWidth = Math.min(MAX_EXCEL_COL_WIDTH, (int) (width * 1.2) + 2);
                sheet.setColumnWidth(c, charWidth * 256); // POI tính theo 1/256 character width unit.
            }

            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to generate XLSX report", ex);
        }
    }

    private static CellStyle buildTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        style.setFont(font);
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle buildHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        return style;
    }

    /** Excel sheet name giới hạn 31 ký tự và không chứa {@code [ ] : * ? / \}. */
    private static String sanitizeSheetName(String title) {
        if (title == null || title.isBlank()) return "Report";
        String cleaned = title.replaceAll("[\\[\\]:*?/\\\\]", "_");
        return cleaned.length() <= 31 ? cleaned : cleaned.substring(0, 31);
    }

    /* ===================== PDF (OpenPDF) ===================== */

    private byte[] exportPdf(String title, String[] headers, List<String[]> rows) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 36, 36);
        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Tiêu đề.
            if (title != null && !title.isBlank()) {
                Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
                com.lowagie.text.Paragraph p = new com.lowagie.text.Paragraph(title, titleFont);
                p.setAlignment(Element.ALIGN_CENTER);
                p.setSpacingAfter(12f);
                document.add(p);
            }

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            // Tính độ rộng cột: chia đều cho số cột (OpenPDF tự co giãn theo content).
            PdfPTable table = new PdfPTable(headers.length);
            table.setWidthPercentage(100f);
            table.setSpacingBefore(4f);
            table.setSpacingAfter(8f);

            // Header row.
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h == null ? "" : h, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(new java.awt.Color(230, 230, 230));
                cell.setPadding(4);
                table.addCell(cell);
            }
            // Data rows.
            for (String[] row : rows) {
                String[] normalized = normalizeRow(row, headers.length);
                for (String v : normalized) {
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v, cellFont));
                    cell.setPadding(3);
                    table.addCell(cell);
                }
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("PDF export failed: {}", ex.getMessage(), ex);
            throw new IllegalStateException("Failed to generate PDF report", ex);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /* ===================== Helpers ===================== */

    /** Pad rỗng nếu row ngắn hơn headers; cắt bớt nếu dài hơn. */
    private static String[] normalizeRow(String[] row, int expected) {
        if (row == null) {
            String[] empty = new String[expected];
            return empty;
        }
        if (row.length == expected) return row;
        String[] out = new String[expected];
        System.arraycopy(row, 0, out, 0, Math.min(row.length, expected));
        return out;
    }
}