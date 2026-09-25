package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ReturnStatus;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3A §23 — Return Request Domain.
 *
 * <p><b>NOT a workflow</b> — this is a contract for Complaint → Return handoff.
 * It captures <i>what</i> a Return is (lifecycle record bổ sung, không xóa
 * Order/OrderItem — Phase 3A §26) without prescribing <i>how</i> it is processed.</p>
 *
 * <p>Trạng thái thật của workflow (vd. khi nào shipper pick-up, khi nào Refund
 * trigger) sẽ được policy/phase sau xác định. Phase 3A chỉ cần:</p>
 * <ul>
 *     <li>Identity (id, complaintId, orderId, orderItemIds)</li>
 *     <li>Ownership (customerId, shopId, vendorId)</li>
 *     <li>Lifecycle status</li>
 *     <li>Timestamps</li>
 *     <li>Quan hệ với Refund (nếu có)</li>
 * </ul>
 *
 * <p>Không tự đặt business rule: deadline, refund amount, fee, v.v. (Phase 3A §6).</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "returns")
public class ReturnRequest {

    @Id
    private String id;

    /** Phase 3A §31 — Return liên quan 1 Complaint (nếu có). Có thể null nếu Return khởi tạo ngoài Complaint. */
    @Indexed
    private String complaintId;

    /** Order gốc — ownership verified tại service. */
    @Indexed
    private String orderId;

    /** Product IDs trong Order mà Return đang trỏ tới. Có thể rỗng → Return cả Order. */
    @Builder.Default
    private List<String> orderItemIds = new ArrayList<>();

    /** Customer yêu cầu Return. */
    @Indexed
    private String customerId;
    private String customerName;

    /** Shop / Vendor đang nhận Return. */
    @Indexed
    private String shopId;
    private String shopName;
    private String vendorId;

    /** Lý do Return — text do Customer cung cấp; không enum hóa để tránh tự phát minh rule. */
    private String reason;

    /** Phase 3A §23 — danh sách evidence tham chiếu. */
    @Builder.Default
    private List<String> evidence = new ArrayList<>();

    /** Trạng thái hiện tại của Return lifecycle. */
    @Indexed
    @Builder.Default
    private ReturnStatus status = ReturnStatus.REQUESTED;

    /**
     * Refund reference (nếu đã trigger). Phase 3A — chỉ là string id,
     * không nhúng Refund document (loose coupling — Refund domain có thể
     * chưa tồn tại).
     */
    private String refundId;

    /** Phase 3A §30 — không tự quyết định refund amount; phase sau sẽ quyết theo policy. */
    private BigDecimal declaredAmount;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
