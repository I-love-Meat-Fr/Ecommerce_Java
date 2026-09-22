package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * TASK #23 — AuditLog entity.
 *
 * Ghi log mọi action compliance: moderation, KYC, admin escalation, auto flag...
 * Schema:
 * - actor: ai làm gì (userId)
 * - action: loại action (AuditAction enum)
 * - resource: bị tác động (resourceType + resourceId)
 * - before/after: trạng thái trước/sau (để rollback audit)
 * - ip: IP người thực hiện
 * - reason: lý do / giải thích
 * - severity: mức độ nghiêm trọng (INFO/WARNING/CRITICAL)
 * - createdAt: timestamp
 *
 * Câu hỏi audit phải trả lời được: "Ai làm gì, với cái gì, khi nào, từ đâu, tại sao?"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    /** User ID người thực hiện (null nếu hệ thống) */
    @Indexed
    private String actorId;

    /** Username/email của actor (để hiển thị khi user đã bị xóa) */
    private String actorUsername;

    /** Role của actor (ADMIN/MODERATOR/VENDOR/CUSTOMER/SYSTEM) */
    private String actorRole;

    /** Loại action */
    @Indexed
    private AuditAction action;

    /** Loại resource bị tác động (USER/SHOP/PRODUCT/REVIEW/COMPLAINT/KYC...) */
    @Indexed
    private String resourceType;

    /** ID của resource */
    @Indexed
    private String resourceId;

    /** Trạng thái trước khi action (lưu JSON string) */
    private String beforeState;

    /** Trạng thái sau khi action */
    private String afterState;

    /** IP address của actor */
    private String ipAddress;

    /** User agent */
    private String userAgent;

    /** Lý do / giải thích */
    private String reason;

    /** Severity */
    @Indexed
    @Builder.Default
    private AuditSeverity severity = AuditSeverity.INFO;

    /** Metadata bổ sung */
    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;
}
