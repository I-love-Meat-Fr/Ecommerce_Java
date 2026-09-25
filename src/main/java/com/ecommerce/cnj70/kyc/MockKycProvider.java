package com.ecommerce.cnj70.kyc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TASK #17 — Mock KYC Provider cho dev/test.
 *
 * Simulates KYC provider behavior:
 * - submit(): tạo referenceId, lưu vào mock store
 * - getResult(): trả về kết quả theo logic:
 *     ✓ APPROVED nếu CCCD hợp lệ (9 hoặc 12 số, checksum logic đơn giản)
 *     ✓ REJECTED nếu CCCD bắt đầu bằng "000" (test reject case)
 *     ✓ REJECTED nếu CCCD chứa "999" (test fraud case)
 *
 * Trong production, thay thế bằng VnptKycProvider hoặc FptKycProvider.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "kyc.provider", havingValue = "mock", matchIfMissing = true)
public class MockKycProvider implements KycProvider {

    // Mock store: referenceId → mock result (simulates provider DB)
    private final Map<String, MockKycRecord> mockStore = new ConcurrentHashMap<>();

    @Override
    public String submit(String citizenId, String taxCode, String bankAccount, String shopId) {
        String referenceId = "MOCK-" + UUID.randomUUID();

        // Lưu mock record với kết quả được quyết định ngay (simulates sync provider)
        // Trong thực tế, provider sẽ xử lý async và gọi callback
        KycResult.KycDecision decision = determineDecision(citizenId);

        mockStore.put(referenceId, new MockKycRecord(
                citizenId,
                taxCode,
                bankAccount,
                shopId,
                decision,
                LocalDateTime.now()
        ));

        log.info("[MockKycProvider] Submitted shop={}, refId={}, decision={}",
                shopId, referenceId, decision);

        return referenceId;
    }

    @Override
    public KycResult getResult(String referenceId) {
        MockKycRecord record = mockStore.get(referenceId);

        if (record == null) {
            log.warn("[MockKycProvider] ReferenceId not found: {}", referenceId);
            return KycResult.builder()
                    .decision(KycResult.KycDecision.REJECTED)
                    .rejectionReason("Reference ID không tồn tại")
                    .verifiedAt(LocalDateTime.now())
                    .build();
        }

        String reason = record.decision == KycResult.KycDecision.REJECTED
                ? getRejectionReason(record.citizenId)
                : null;

        return KycResult.builder()
                .decision(record.decision)
                .rejectionReason(reason)
                .verifiedAt(record.verifiedAt)
                .build();
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }

    private KycResult.KycDecision determineDecision(String citizenId) {
        if (citizenId == null || citizenId.isBlank()) {
            return KycResult.KycDecision.REJECTED;
        }

        // Test reject case: CCCD bắt đầu bằng "000"
        if (citizenId.startsWith("000")) {
            return KycResult.KycDecision.REJECTED;
        }

        // Test fraud case: CCCD chứa "999"
        if (citizenId.contains("999")) {
            return KycResult.KycDecision.REJECTED;
        }

        // Valid CCCD: hợp lệ checksum (đơn giản: không bị reject ở trên)
        return KycResult.KycDecision.APPROVED;
    }

    private String getRejectionReason(String citizenId) {
        if (citizenId.startsWith("000")) {
            return "Số CCCD không hợp lệ: bị cấm";
        }
        if (citizenId.contains("999")) {
            return "Phát hiện CCCD thuộc danh sách nghi ngờ gian lận";
        }
        return "KYC không được duyệt bởi nhà cung cấp";
    }

    private record MockKycRecord(
            String citizenId,
            String taxCode,
            String bankAccount,
            String shopId,
            KycResult.KycDecision decision,
            LocalDateTime verifiedAt
    ) {}
}
