package com.ecommerce.cnj70.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * PHASE 5 — Bộ filter chung cho Admin Report.
 *
 * <p>Tất cả field đều optional (nullable). Controller parse query param rồi gán vào DTO này,
 * service sẽ tự default nếu null (vd: status = ALL, from/to = 30 ngày gần nhất).</p>
 *
 * <p>Các field theo {@link com.ecommerce.cnj70.enums.ReportType}:</p>
 * <ul>
 *   <li><b>ORDERS</b>: status, fromDate, toDate, q (tìm theo userName/userId)</li>
 *   <li><b>REVENUE</b>: period (DAY/WEEK/MONTH/QUARTER/YEAR/ALL), fromDate, toDate</li>
 *   <li><b>FINANCIAL</b>: fromDate, toDate (mặc định all-time)</li>
 *   <li><b>USERS</b>: role (CUSTOMER/VENDOR/...), status (ACTIVE/LOCKED/UNVERIFIED), q</li>
 *   <li><b>SHOPS</b>: status (PENDING/APPROVED/...), kycStatus, q (tên shop)</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFilter {

    /** Order status filter (PENDING/PREPARING/SHIPPING/DELIVERED/CANCELLED). Null/blank = ALL. */
    private String status;

    /** Period cho revenue report (DAY/WEEK/MONTH/QUARTER/YEAR/ALL). Null = WEEK. */
    private String period;

    /** User role filter (CUSTOMER/VENDOR/MODERATOR/ADMIN). Null = ALL. */
    private String role;

    /** Shop status filter (PENDING/APPROVED/REJECTED/SUSPENDED/RESTRICTED). Null = ALL. */
    private String shopStatus;

    /** KYC status filter. Null = ALL. */
    private String kycStatus;

    /** Account status filter (ACTIVE/LOCKED/UNVERIFIED). Null = ALL. */
    private String accountStatus;

    /** Từ ngày (inclusive). Null = không giới hạn dưới. */
    private LocalDate fromDate;

    /** Đến ngày (inclusive). Null = không giới hạn trên. */
    private LocalDate toDate;

    /** Từ khoá tìm kiếm tự do (userName, userEmail, shopName, ...). Null = không lọc. */
    private String q;
}