package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ViolationSeverity;
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

/**
 * Group 1 — Từ khóa cấm dùng cho Auto Moderation Pipeline.
 *
 * <p>Quản lý ở MongoDB thay vì hard-code config để Admin có thể thêm/xóa
 * từ khóa mà không cần deploy lại. {@link ViolationSeverity} quyết định
 * mức độ:</p>
 *
 * <ul>
 *   <li>{@code CRITICAL} → Auto Moderation HARD_REJECT Product / HIDDEN Review.</li>
 *   <li>{@code HIGH}     → Auto Moderation HARD_REJECT Product / HIDDEN Review.</li>
 *   <li>{@code MEDIUM}   → Auto Moderation SOFT_FLAG → MANUAL_REVIEW.</li>
 *   <li>{@code LOW}      → Auto Moderation SOFT_FLAG → MANUAL_REVIEW (mức nhẹ).</li>
 * </ul>
 *
 * <p>{@link #enabled} cho phép Admin "vô hiệu hóa mềm" — keyword vẫn còn
 * trong DB nhưng không scan. Hữu ích khi cần tạm ngưng scan keyword để
 * tuning rule mà không mất lịch sử.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "blacklist_words")
public class BlacklistWord {

    @Id
    private String id;

    /** Từ khóa cấm — match không phân biệt hoa thường. */
    @Indexed(unique = true)
    private String keyword;

    /** Mức độ vi phạm. */
    @Indexed
    private ViolationSeverity severity;

    /** Mô tả lý do thêm từ khóa (vd: "theo Nghị định 13/2023"). */
    private String description;

    /** Keyword có đang active không. */
    @Builder.Default
    private boolean enabled = true;

    /** Admin đã thêm keyword này. */
    private String createdBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
