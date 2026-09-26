package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ComplaintLevel;
import com.ecommerce.cnj70.enums.ComplaintReason;
import com.ecommerce.cnj70.enums.ComplaintStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase 3 — Complaint document.
 *
 * <p>Lưu trữ khiếu nại của Customer về Order (Non-delivery / Wrong product /
 * Damaged / Warranty…). Theo Marketplace Policy §7 có 3 cấp:</p>
 * <ul>
 *     <li>LEVEL_0 — Customer ↔ Vendor</li>
 *     <li>LEVEL_1 — Moderator</li>
 *     <li>LEVEL_2 — Admin</li>
 * </ul>
 *
 * <p>Không trộn với {@link ReportCase} (dùng cho policy violation /
 * content moderation) — đây là customer-vendor order dispute.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "complaints")
public class Complaint {

    @Id
    private String id;

    /** Customer tạo complaint. */
    @Indexed
    private String customerId;

    private String customerName;
    private String customerEmail;

    /** Shop / Vendor đang bị khiếu nại. */
    @Indexed
    private String shopId;

    private String shopName;
    private String vendorId;

    /** Order gốc — phải thuộc customer (Phase 3 §11, §12). */
    @Indexed
    private String orderId;

    /** Snapshot order items tại thời điểm khiếu nại. */
    @Builder.Default
    private List<String> orderItemIds = new ArrayList<>();

    /** Lý do enum (Phase 3 §6 — chỉ các giá trị policy). */
    private ComplaintReason reason;

    /** Mô tả chi tiết (Phase 3 §10). */
    private String description;

    /** Danh sách URL evidence (image / document) — Phase 3 §15. */
    @Builder.Default
    private List<String> evidence = new ArrayList<>();

    /** Trạng thái hiện tại. */
    @Indexed
    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    /** Cấp escalation hiện tại. */
    @Indexed
    @Builder.Default
    private ComplaintLevel level = ComplaintLevel.LEVEL_0;

    /** Vendor response text (Level 0) — append-mode cho phép nhiều lần phản hồi. */
    private String vendorResponse;
    private LocalDateTime vendorRespondedAt;
    private String vendorRespondedById;
    private String vendorRespondedByEmail;

    /**
     * Số lần Vendor đã gửi phản hồi. Đếm các lần append vào {@link #vendorResponse}.
     * Phase 3A §47.
     */
    @Builder.Default
    private int vendorReplyCount = 0;

    /** Moderator assigned (Level 1). */
    private String assignedModeratorId;
    private String assignedModeratorEmail;
    private LocalDateTime moderatorAssignedAt;

    /** Admin assigned (Level 2). */
    private String assignedAdminId;
    private String assignedAdminEmail;
    private LocalDateTime adminAssignedAt;

    /** Decision cuối (resolve / reject). */
    private String decision;
    private String decisionReason;
    private LocalDateTime resolvedAt;
    private String resolvedById;
    private String resolvedByEmail;
    private String resolvedByRole;

    /**
     * Deadline cho Level 0 / Moderator (configurable — Phase 3 §32).
     * Scheduler dùng để auto-escalate.
     */
    private LocalDateTime vendorResponseDeadline;
    private LocalDateTime moderatorResolutionDeadline;

    /** Số lần đã escalate (idempotency — §34). */
    @Builder.Default
    private int escalationCount = 0;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
