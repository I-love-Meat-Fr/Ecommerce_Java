package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "violations")
public class Violation {

    @Id
    private String id;

    /** Shop bị vi phạm */
    @Indexed
    private String shopId;

    /** Sản phẩm vi phạm (optional - có thể là vi phạm chung của shop) */
    private String productId;

    /** Tên sản phẩm vi phạm (snapshot, optional) */
    private String productName;

    /** Đơn hàng liên quan (optional) */
    private String orderId;

    @Indexed
    private ViolationType type;

    @Indexed
    private ViolationSeverity severity;

    /** Mô tả ngắn gọn về vi phạm */
    private String reason;

    /** Ghi chú của admin */
    private String adminNote;

    /** Username admin tạo violation */
    private String createdBy;

    @CreatedDate
    @Indexed
    private LocalDateTime createdAt;

    /** Thời điểm đã xử lý (resolved). Null nếu chưa xử lý. */
    private LocalDateTime resolvedAt;

    /** Admin đã xử lý */
    private String resolvedBy;

    /** Ghi chú khi xử lý */
    private String resolutionNote;

    public boolean isActive() {
        return resolvedAt == null;
    }
}
