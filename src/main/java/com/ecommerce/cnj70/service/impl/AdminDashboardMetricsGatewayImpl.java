package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.RefundStatus;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.RefundRequestRepository;
import com.ecommerce.cnj70.service.AdminDashboardMetricsGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Phase 4A — Default Dashboard metrics gateway.
 *
 * <p>Implements {@link AdminDashboardMetricsGateway} using only what is
 * already in this project:</p>
 * <ul>
 *   <li><b>KYC</b> — read from {@link KycProfileRepository}.</li>
 *   <li><b>Refund</b> — read from {@link RefundRequestRepository}: tổng {@code settledAmount}
 *       cho {@link RefundStatus#SUCCEEDED} (đã settle thật). Wire ngay từ bây giờ vì
 *       data đã có sẵn trong collection {@code refunds}.</li>
 *   <li><b>Vendor Sales / Payable / Platform Revenue</b> — chưa có Finance/Settlement
 *       backend → trả {@code null} + availability=false. Khi wire, chỉ thay bean impl.</li>
 * </ul>
 *
 * <p>Per Phase 4A §41, "N/A" được ưu tiên hơn fake 0. UI sẽ render N/A badge cho mọi field
 * null kèm availability=false.</p>
 *
 * <p><b>Phân biệt rõ (Phase 4A §20–24):</b></p>
 * <ul>
 *   <li><b>GMV</b> ≠ <b>Platform Revenue</b> ≠ <b>Vendor Sales</b> ≠ <b>Vendor Payable</b>
 *       ≠ <b>Refund</b>. Mỗi metric lấy từ nguồn riêng; KHÔNG fall-back qua lại.</li>
 *   <li><b>Refund</b> ở đây = tiền thực sự đã settle về Customer (settledAmount, status=SUCCEEDED).
 *       Đây KHÔNG phải GMV, KHÔNG phải Platform Revenue.</li>
 * </ul>
 *
 * <p>Khi Finance / Vendor Finance / Settlement backends được wire, implementation này (hoặc
 * bean mới) sẽ cung cấp giá trị thật.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDashboardMetricsGatewayImpl implements AdminDashboardMetricsGateway {

    private final KycProfileRepository kycProfileRepository;
    private final RefundRequestRepository refundRequestRepository;

    @Override
    public Long countPendingKyc() {
        try {
            // Phase 4A §22: Pending KYC = tất cả hồ sơ chưa xong (cả 3rd-party và admin stage).
            // Enum KycStatus có 2 pending: PENDING_THIRD_PARTY (gửi provider) và PENDING_ADMIN (chờ admin duyệt).
            long thirdParty = kycProfileRepository.countByStatus(KycStatus.PENDING_THIRD_PARTY);
            long admin = kycProfileRepository.countByStatus(KycStatus.PENDING_ADMIN);
            return thirdParty + admin;
        } catch (Exception ex) {
            log.warn("KYC repository unavailable: {}", ex.getMessage());
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
        try {
            // Phase 4A §24: Refund = sum(RefundRequest.settledAmount) where status=SUCCEEDED.
            // Lưu ý: settledAmount KHÁC declaredAmount — chỉ những refund đã provider confirm
            // mới được tính (tránh "fake 0" nếu chỉ có refund đang REQUESTED).
            List<com.ecommerce.cnj70.document.RefundRequest> succeeded =
                    refundRequestRepository.findByStatus(RefundStatus.SUCCEEDED,
                            org.springframework.data.domain.Pageable.unpaged()).getContent();
            BigDecimal total = BigDecimal.ZERO;
            for (com.ecommerce.cnj70.document.RefundRequest rr : succeeded) {
                BigDecimal amount = rr.getSettledAmount();
                if (amount == null) {
                    // Fallback: nếu provider không set settledAmount thì dùng declaredAmount —
                    // vẫn chỉ tính SUCCEEDED (đã thanh toán xong). Không tính REQUESTED/PROCESSING.
                    amount = rr.getDeclaredAmount();
                }
                if (amount != null) total = total.add(amount);
            }
            return total;
        } catch (Exception ex) {
            log.warn("RefundRepository unavailable: {}", ex.getMessage());
            return null;
        }
    }

    @Override
    public BigDecimal getPlatformRevenue() {
        return null; // Finance platform commission — INTEGRATION-READY
    }

    @Override
    public AdminDashboardRes.MetricAvailability getAvailability() {
        return AdminDashboardRes.MetricAvailability.builder()
                .kyc(true)               // KycProfileRepository wired
                .vendorSales(false)      // INTEGRATION-READY
                .vendorPayable(false)    // INTEGRATION-READY
                .refund(true)            // wired from RefundRequestRepository (settledAmount, SUCCEEDED)
                .platformRevenue(false)  // INTEGRATION-READY
                .build();
    }
}
