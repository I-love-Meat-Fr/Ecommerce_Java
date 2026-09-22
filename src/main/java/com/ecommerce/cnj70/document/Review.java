package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ReviewModerationStatus;
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
@Document(collection = "reviews")
public class Review {

    @Id
    private String id;

    @Indexed
    private String productId;

    @Indexed
    private String userId;

    private String userName;

    private String userAvatar;

    private int rating;

    private String comment;

    // ===== TASK #15 Review Moderation =====

    /** Trạng thái moderation: VISIBLE/REPORTED/HIDDEN/DELETED */
    @Indexed
    @Builder.Default
    private ReviewModerationStatus moderationStatus = ReviewModerationStatus.VISIBLE;

    /** Lý do bị report/ẩn/xóa */
    private String moderationReason;

    /** User/Moderator đã xử lý moderation gần nhất */
    private String moderatedBy;

    /** Thời điểm moderation được áp dụng */
    private LocalDateTime moderatedAt;

    /** Số lượng report cho Review này */
    @Builder.Default
    private int reportCount = 0;

    @CreatedDate
    private LocalDateTime createdAt;
}
