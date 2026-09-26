package com.ecommerce.cnj70.dto.response;

import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRes {
    
    private String id;
    private String productId;
    private String userId;
    private String userName;
    private String userAvatar;
    private int rating;
    private String comment;
    private LocalDateTime createdAt;

    /** TASK #26 — Danh sách URL ảnh đính kèm review. */
    @Builder.Default
    private List<String> images = new ArrayList<>();

    /**
     * TASK #15 — Trạng thái moderation (VISIBLE/REPORTED/HIDDEN/DELETED).
     * UI dùng để render badge cảnh báo và ẩn/hiện review tuỳ trạng thái.
     */
    private ReviewModerationStatus moderationStatus;

    /** Lý do moderation (nếu có). */
    private String moderationReason;

    /** Số lượt report. */
    @Builder.Default
    private int reportCount = 0;
}
