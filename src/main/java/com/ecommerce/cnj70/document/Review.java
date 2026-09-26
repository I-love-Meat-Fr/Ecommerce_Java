package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
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

    /**
     * TASK #26 — Danh sách ảnh đính kèm Review.
     * Mỗi phần tử là URL path trỏ tới /uploads/{uuid}.{ext}.
     * Giới hạn tối đa {@code MAX_IMAGES_PER_REVIEW} ảnh (enforced trong service).
     * Mỗi ảnh upload qua {@code FileUploadUtil} đã validate size/MIME/extension.
     *
     * <p>Chỉ owner Review mới được thêm/xoá ảnh (enforced trong service).</p>
     */
    @Builder.Default
    private List<String> images = new ArrayList<>();

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

    /**
     * Phase 2B — Moderator who most recently processed this review.
     * Bổ sung từ stash, không xung đột với moderatedBy (đó là field legacy).
     */
    private String moderationActorId;

    /**
     * Phase 2B — Timestamp của moderation action gần nhất (alias cho moderatedAt
     * cho pipeline mới của Moderator).
     */
    private LocalDateTime moderationAt;

    /**
     * Phase 2B — Whether this review is hidden from public view.
     * Distinct from {@link #moderationStatus}: HIDDEN ẩn review khỏi public
     * nhưng vẫn còn trong hệ thống (recoverable qua Unhide).
     */
    @Builder.Default
    private boolean hidden = false;

    /**
     * Phase 2B — Reason for hiding (khi {@link #hidden} = true).
     */
    private String hiddenReason;

    /**
     * Phase 2B — Trạng thái moderation trong pipeline mới (ModerationStatus).
     * Song song với {@link #moderationStatus} (ReviewModerationStatus - legacy).
     * Giữ cả hai để đảm bảo backward compatibility với code dùng ReviewModerationStatus
     * trong khi Phase 2B pipeline dùng ModerationStatus.
     */
    @Indexed
    private ModerationStatus pipelineModerationStatus;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
