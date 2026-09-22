package com.ecommerce.cnj70.kyc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #17 — Kết quả từ ThirdPartyKycProvider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycResult {

    /** APPROVED | REJECTED */
    private KycDecision decision;

    /** Lý do từ chối (nếu REJECTED) */
    private String rejectionReason;

    /** Thời điểm provider hoàn thành xử lý */
    private java.time.LocalDateTime verifiedAt;

    public enum KycDecision {
        APPROVED,
        REJECTED
    }
}
