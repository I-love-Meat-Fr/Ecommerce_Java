package com.ecommerce.cnj70.dto.response;

import com.ecommerce.cnj70.enums.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * TASK #16 — KYC status response.
 *
 * KHÔNG bao giờ trả về PII plaintext về API.
 * Chỉ trả về trạng thái và metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycStatusResponse {

    private String shopId;

    private KycStatus kycStatus;

    /** Reference ID từ KYC provider (để trace khi gọi hỗ trợ) */
    private String kycReferenceId;

    /** Thời điểm KYC được duyệt (APPROVED) */
    private LocalDateTime kycApprovedAt;

    /** Lý do từ chối (chỉ khi KYC_REJECTED) */
    private String kycRejectionReason;

    /** Thời điểm nộp KYC gần nhất */
    private LocalDateTime kycSubmittedAt;

    /** Mô tả trạng thái bằng tiếng Việt cho UI */
    private String statusMessage;
}
