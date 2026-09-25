package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phase 3A §28 — Refund Request Contract.
 *
 * <p><b>INTEGRATION-READY</b>: Phản ánh <i>intent</i> trả tiền cho Customer sau khi
 * Complaint/Return được resolve. KHÔNG thực hiện financial operation thật —
 * provider thật (Stripe / Momo / Banking / Internal ledger) sẽ được wire vào
 * Phase 3B/Phase 4 theo policy.</p>
 *
 * <h3>Hard rules (Phase 3A)</h3>
 * <ul>
 *     <li>§29 — Tuyệt đối không fake refund balance / vendor payable / escrow.</li>
 *     <li>§30 — Không tự quyết amount. {@code declaredAmount} chỉ là giá trị
 *         Customer/Vendor/Moderator <i>đề xuất</i> — provider cuối cùng quyết.</li>
 *     <li>§31 — Refund không được mồ côi: phải gắn với Complaint hoặc Return hoặc Order.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "refunds")
public class RefundRequest {

    @Id
    private String id;

    /** Phase 3A §31 — At least one of these must be present (enforced in service). */
    @Indexed
    private String complaintId;

    @Indexed
    private String returnId;

    @Indexed
    private String orderId;

    /** Customer nhận refund. */
    @Indexed
    private String customerId;

    /** Shop / Vendor bị trừ tiền. */
    @Indexed
    private String shopId;
    private String vendorId;

    /** Phase 3A §30 — declared amount (proposal). NOT a final amount. */
    private BigDecimal declaredAmount;

    /** Phase 3A §30 — actual amount from provider (set by external). Null until provider responds. */
    private BigDecimal settledAmount;

    /** Provider đang xử lý (e.g., "stripe", "momo", "internal-ledger"). */
    private String provider;

    /** Provider's transaction id (set after provider accepts). */
    private String providerTransactionId;

    /** Lifecycle status. */
    @Indexed
    @Builder.Default
    private RefundStatus status = RefundStatus.REQUESTED;

    /** Provider message khi FAILED. */
    private String providerMessage;

    /** Người yêu cầu (Customer/Vendor/Moderator/Admin) — for audit. */
    private String requestedById;
    private String requestedByEmail;
    private String requestedByRole;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Set khi Provider confirm SUCCEEDED. */
    private LocalDateTime settledAt;
}
