package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.response.AdminDashboardRes;

import java.math.BigDecimal;

/**
 * Phase 4A — Integration seam for external finance / vendor / KYC metrics.
 *
 * <p>Per Phase 4A §25:</p>
 * <ul>
 *     <li>Finance / Vendor / Settlement / KYC backend modules are NOT a
 *         prerequisite for Phase 4A implementation.</li>
 *     <li>They are integration points exposed through this gateway.</li>
 *     <li>When the real backend lands, the gateway implementation is replaced
 *         and no Dashboard code change is required.</li>
 * </ul>
 *
 * <p>Per Phase 4A §41, when external data is unavailable the implementation
 * MUST return {@code null}/{@link MetricAvailability} flags set to
 * {@code false} — NEVER fake zero.</p>
 */
public interface AdminDashboardMetricsGateway {

    /**
     * @return Pending KYC count (KycStatus.PENDING_KYC); null if KYC backend is not wired.
     */
    Long countPendingKyc();

    /**
     * @return Vendor Sales — total sales attributed to vendors; null if Finance backend not wired.
     */
    BigDecimal getVendorSales();

    /**
     * @return Vendor Payable — settlement amount owed to vendors; null if Settlement not wired.
     */
    BigDecimal getVendorPayable();

    /**
     * @return Refund — total refunded amount; null if Refund backend not wired.
     */
    BigDecimal getRefund();

    /**
     * @return Platform Revenue — commission/fee earned by platform; null if Finance not wired.
     */
    BigDecimal getPlatformRevenue();

    /**
     * Aggregate availability flags so the UI can render N/A badges consistently.
     */
    AdminDashboardRes.MetricAvailability getAvailability();
}
