package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #18 — Webhook payload từ KYC Provider.
 *
 * Provider gọi endpoint này khi có kết quả KYC.
 * Payload phải chứa referenceId (do provider sinh ra lúc submit).
 *
 * Trong production, cần thêm:
 * - HMAC signature verification (chống spoof)
 * - Timestamp validation (chống replay attack)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycCallbackRequest {

    @NotBlank(message = "referenceId is required")
    private String referenceId;

    /** APPROVED | REJECTED */
    @NotBlank(message = "decision is required")
    private String decision;

    /** Lý do từ chối (chỉ khi REJECTED) */
    private String rejectionReason;

    /** Signature HMAC để verify request từ provider */
    private String signature;
}
