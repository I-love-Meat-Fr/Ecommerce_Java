package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.service.AdminDashboardMetricsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;

/**
 * Phase 4A — Default Dashboard metrics gateway.
 *
 * <p>Implements {@link AdminDashboardMetricsGateway} using only what is
 * already in this project. KYC is supported via {@link KycProfileRepository}
 * (added in Phase 3B). Vendor Sales / Payable / Refund / Platform Revenue
 * have no backend yet — they return {@code null} and availability=false.</p>
 *
 * <p>Per Phase 4A §41, "N/A" is preferred over fake 0. The UI will display
 * N/A badges against any null field.</p>
 *
 * <p>When Finance / Vendor Finance / Settlement / Refund backends are wired,
 * this implementation (or a new bean) provides the real values.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDashboardMetricsGatewayImpl implements AdminDashboardMetricsGateway {

    private final KycProfileRepository kycProfileRepository;

    @Override
    public Long countPendingKyc() {
        try {
            return kycProfileRepository.countByStatus(KycStatus.PENDING_THIRD_PARTY);
        } catch (Exception ex) {
            log.warn("KYC repository unavailable (expected until KYC backend is wired): {}",
                    ex.getMessage());
            return null;
        }
    }

    @Override
    public BigDecimal getVendorSales() {
        return null; // Finance vendor sales backend — INTEGRATION-READY
    }

    @Override
    public BigDecimal getVendorPayable() {
        return null; // Settlement backend — INTEGRATION-READY
    }

    @Override
    public BigDecimal getRefund() {
        return null; // Refund backend — INTEGRATION-READY
    }

    @Override
    public BigDecimal getPlatformRevenue() {
        return null; // Finance platform commission — INTEGRATION-READY
    }

    @Override
    public AdminDashboardRes.MetricAvailability getAvailability() {
        return AdminDashboardRes.MetricAvailability.builder()
                .kyc(true)               // KycProfileRepository is in place
                .vendorSales(false)      // INTEGRATION-READY
                .vendorPayable(false)    // INTEGRATION-READY
                .refund(false)           // INTEGRATION-READY
                .platformRevenue(false)  // INTEGRATION-READY
                .build();
    }
}
