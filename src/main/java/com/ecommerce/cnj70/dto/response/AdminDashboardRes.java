package com.ecommerce.cnj70.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Phase 4A — Admin Dashboard response.
 *
 * <p>Extends the original {@code AdminDashboardRes} with metric groupings
 * per Phase 4A §13:</p>
 * <ul>
 *     <li><b>Platform</b>: totalUsers, totalShops, totalProducts, totalOrders</li>
 *     <li><b>Financial</b>: gmv, platformRevenue, vendorSales, vendorPayable, refund</li>
 *     <li><b>Compliance</b>: pendingKyc, pendingModeration, openEscalation, openViolation</li>
 *     <li><b>Risk</b>: suspendedShops</li>
 * </ul>
 *
 * <h3>Financial semantics — strictly distinguished</h3>
 * <ul>
 *     <li>{@link #gmv} — Gross Merchandise Value: sum of all DELIVERED order totals.</li>
 *     <li>{@link #platformRevenue} — platform fee taken (today = 0; placeholder until Finance contract defined).</li>
 *     <li>{@link #vendorSales} — sum of vendor-side sales (today = null/N/A; seam).</li>
 *     <li>{@link #vendorPayable} — settlement amount owed to vendors (today = null/N/A; seam).</li>
 *     <li>{@link #refund} — total refunded amount (today = null/N/A; seam).</li>
 * </ul>
 *
 * <p>Phase 4A §20–24: GMV ≠ Platform Revenue ≠ Vendor Sales ≠ Vendor Payable ≠ Refund.
 * Each field is sourced from its own contract — never derived from another.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardRes {

    /* ===== PLATFORM OVERVIEW ===== */
    private long totalUsers;
    private long totalShops;
    private long totalProducts;
    private long totalOrders;

    /* ===== FINANCIAL OVERVIEW ===== */
    /**
     * <b>GMV</b> — Gross Merchandise Value: tổng tiền khách hàng đã trả cho đơn đã giao.
     * <p>Nguồn: {@code sum(Order.totalAmount) where Order.status = DELIVERED}.</p>
     * <p>totalAmount = subtotal + shippingFee − discount (gồm cả phí ship, trừ voucher).</p>
     * <p><b>KHÔNG</b> được dùng làm Platform Revenue / Vendor Sales / Vendor Payable.</p>
     */
    private BigDecimal gmv;
    /**
     * <b>Platform Revenue</b> — hoa hồng / phí sàn nhận được.
     * <p>Nguồn dự kiến: {@code sum(Order.commissionAmount) where DELIVERED}. Hiện Order.commissionAmount
     * chưa được wire nên giá trị trả về là {@code null} → UI hiển thị N/A.</p>
     * <p><b>KHÔNG BAO GIỜ</b> fall-back về GMV (Phase 4A §11, §43).</p>
     */
    private BigDecimal platformRevenue;
    /**
     * <b>Vendor Sales</b> — doanh số bán hàng của vendor (chưa trừ hoa hồng sàn, không gồm ship).
     * <p>Nguồn: {@code sum(OrderItem.subtotal) where Order.status = DELIVERED}.</p>
     * <p><b>KHÁC GMV</b>: GMV là tổng tiền khách trả; Vendor Sales là doanh số thuần của vendor.</p>
     * <p><b>KHÁC Vendor Payable</b>: Payable = subtotal − commission (vendor còn được nhận);
     * Vendor Sales = subtotal (vendor đã bán được, chưa trừ phí).</p>
     */
    private BigDecimal vendorSales;
    /**
     * <b>Vendor Payable</b> — số tiền sàn còn nợ vendor sau khi trừ hoa hồng.
     * <p>Nguồn dự kiến: {@code sum(Order.vendorNetAmount) where DELIVERED}. Hiện Order.vendorNetAmount
     * chưa wire nên giá trị trả về là {@code null} → UI hiển thị N/A.</p>
     */
    private BigDecimal vendorPayable;
    /**
     * <b>Refund</b> — tổng tiền đã hoàn trả thực sự cho Customer.
     * <p>Nguồn: {@code sum(RefundRequest.settledAmount) where status = SUCCEEDED}.</p>
     * <p>Chỉ tính các refund đã provider confirm (SUCCEEDED), KHÔNG tính REQUESTED/PROCESSING/FAILED.</p>
     * <p><b>KHÁC GMV/Platform Revenue/Vendor Sales</b>: đây là chiều đi của dòng tiền (ra khỏi
     * platform), không phải doanh thu.</p>
     */
    private BigDecimal refund;

    /* ===== COMPLIANCE ===== */
    /**
     * Pending KYC count (KycStatus.PENDING_KYC).
     * null/N/A when KYC backend is not wired.
     */
    private Long pendingKyc;
    /**
     * Pending Moderation count = Product (PENDING_MANUAL) + Review (PENDING_MANUAL) + AUTO_*
     * from existing Product/Review moderation contracts.
     */
    private Long pendingModeration;
    /**
     * Open Escalation count = Escalation.Status.PENDING + IN_REVIEW (Phase 3A).
     */
    private Long openEscalation;
    /**
     * Open Violation count = ViolationStatus.OPEN + UNDER_REVIEW + APPEALED (Phase 3C).
     */
    private Long openViolation;

    /* ===== OPERATIONAL RISK ===== */
    /**
     * Suspended Shops count = ShopStatus.SUSPENDED + RESTRICTED (Phase 3A).
     */
    private Long suspendedShops;

    /* ===== EXISTING (revenue trend, recent activities) ===== */
    private List<RecentActivity> recentActivities;
    private List<DailyMetric> revenueTrend;

    /* ===== DATA FRESHNESS / AVAILABILITY ===== */
    /**
     * True when each external seam is available.
     * Per Phase 4A §41: missing data MUST show N/A, never fake 0.
     */
    @Builder.Default
    private MetricAvailability availability = new MetricAvailability();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricAvailability {
        /** KYC backend integration status. */
        private boolean kyc;
        /** Vendor Sales backend integration status. */
        private boolean vendorSales;
        /** Vendor Payable backend integration status. */
        private boolean vendorPayable;
        /** Refund backend integration status. */
        private boolean refund;
        /** Platform Revenue backend integration status. */
        private boolean platformRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String type;
        private String description;
        private String time;
        private String orderId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMetric {
        private String label;
        private BigDecimal revenue;
        private long orders;
    }
}
