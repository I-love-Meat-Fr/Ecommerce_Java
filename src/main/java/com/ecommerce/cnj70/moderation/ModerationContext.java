package com.ecommerce.cnj70.moderation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Đối tượng ngữ cảnh truyền vào mỗi {@link ModerationChecker} khi pipeline chạy.
 *
 * <p>Đây là interface thống nhất (theo yêu cầu thiết kế) cho phép tái sử dụng
 * pipeline cho cả {@code Product} và {@code Review}. Mỗi implementation
 * (vd {@link ProductModerationContext}, {@link ReviewModerationContext}) sẽ
 * chứa payload riêng nhưng đều cung cấp:</p>
 *
 * <ul>
 *   <li>{@link #getText()} — văn bản chính cần quét (Product name+description
 *       hoặc Review comment).</li>
 *   <li>{@link #getImageUrls()} — danh sách URL ảnh để check trùng lặp.</li>
 *   <li>{@link #getAuthorId()} — tác giả (vendor hoặc customer) để check spam
 *       per-user.</li>
 *   <li>{@link #getTargetId()} — id của đối tượng đang được kiểm duyệt.</li>
 *   <li>{@link #getTargetType()} — {@link ModerationTargetType#PRODUCT} hoặc
 *       {@link ModerationTargetType#REVIEW}.</li>
 *   <li>{@link #getSubmittedAt()} — mốc thời gian submit, phục vụ các checker
 *       phát hiện spam theo cụm thời gian (vd: cùng user đăng 5 review trong
 *       &lt; 60 giây).</li>
 * </ul>
 *
 * <p>Các checker có thể lưu state tạm vào {@link #getSharedState()} để chia sẻ
 * giữa các checker (vd: hash ảnh đã tính ở checker A có thể dùng cho checker B).</p>
 */
public interface ModerationContext {

    /** Id đối tượng đang được kiểm duyệt (vd productId, reviewId). */
    String getTargetId();

    /** Loại đối tượng: PRODUCT hay REVIEW. */
    ModerationTargetType getTargetType();

    /** Id tác giả (vendor hoặc customer). */
    String getAuthorId();

    /** Văn bản cần quét (Product name+description hoặc Review comment). */
    String getText();

    /** Danh sách URL ảnh để check duplicate. Có thể null/empty. */
    List<String> getImageUrls();

    /** Thời điểm submit (phục vụ anti-spam theo cụm thời gian). */
    LocalDateTime getSubmittedAt();

    /**
     * State chia sẻ giữa các checker trong cùng 1 lần chạy pipeline.
     * Cho phép checker A lưu kết quả tính toán (vd hash ảnh) để checker B
     * tái sử dụng, tránh recompute.
     */
    SharedState getSharedState();

    /**
     * State tạm chia sẻ giữa các checker trong cùng 1 lần chạy pipeline.
     * Thread-unsafe theo thiết kế — chỉ dùng trong scope một request.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class SharedState {
        @Builder.Default
        private java.util.Map<String, Object> values = new java.util.HashMap<>();

        @SuppressWarnings("unchecked")
        public <T> T get(String key, Class<T> clazz) {
            if (values == null) return null;
            Object v = values.get(key);
            return v != null && clazz.isInstance(v) ? (T) v : null;
        }

        public void put(String key, Object value) {
            if (values != null) {
                values.put(key, value);
            }
        }
    }
}
