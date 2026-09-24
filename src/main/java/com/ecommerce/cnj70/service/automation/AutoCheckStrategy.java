package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * C1 — Một check đơn lẻ trong Product Auto Moderation Pipeline.
 *
 * <p>Mỗi implementation tương ứng với 1 task trong checklist:
 * <ul>
 *   <li><b>BLACKLIST_KEYWORD</b> — task C2</li>
 *   <li><b>DUPLICATE_PRODUCT</b> — task C3</li>
 *   <li><b>ABNORMAL_PRICE</b> — task C4</li>
 *   <li><b>IMAGE_CONTENT</b> — task C5</li>
 *   <li><b>FORBIDDEN_CATEGORY</b> — task C6</li>
 * </ul>
 *
 * <p>Pipeline orchestrator ({@code AutoModerationServiceImpl}) tự động inject
 * toàn bộ các bean implement {@code AutoCheckStrategy} và chạy theo thứ tự
 * Spring quyết định; KHÔNG tự hard-code từng check.
 *
 * <p>Thêm check mới = tạo bean implement interface này; orchestrator tự động
 * chạy. KHÔNG cần sửa orchestrator.
 */
public interface AutoCheckStrategy {

    /**
     * ID định danh check, dùng để log + tạo autoFlag trên ReportCase.
     * <p>Quy ước: CONSTANT_CASE (vd {@code "BLACKLIST_KEYWORD"}).
     *
     * @return id không null, không trống
     */
    String id();

    /**
     * Thực hiện check trên Product.
     *
     * @param ctx context chứa Product + shared state giữa các check
     * @return verdict: {@code PASS} / {@code SOFT_FLAG} / {@code HARD_REJECT}.
     *         KHÔNG được trả null.
     */
    AutoCheckVerdict check(AutoCheckContext ctx);

    /**
     * Context truyền vào mỗi check. Cho phép check truy cập:
     * <ul>
     *   <li>Product đang được evaluate</li>
     *   <li>{@code sharedState} — map chia sẻ giữa các check trong cùng
     *       1 lần chạy pipeline (vd: imageHash đã tính ở C5 có thể được
     *       C3 reuse để so duplicate)</li>
     * </ul>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class AutoCheckContext {
        private Product product;
        private Map<String, Object> sharedState;

        /**
         * Helper: lấy giá trị từ sharedState với cast type an toàn.
         *
         * @param key key trong sharedState
         * @param clazz kiểu mong đợi
         * @param <T> kiểu generic
         * @return giá trị nếu key tồn tại và đúng kiểu, ngược lại null
         */
        @SuppressWarnings("unchecked")
        public <T> T getShared(String key, Class<T> clazz) {
            if (sharedState == null) {
                return null;
            }
            Object value = sharedState.get(key);
            if (value != null && clazz.isInstance(value)) {
                return (T) value;
            }
            return null;
        }

        public void putShared(String key, Object value) {
            if (sharedState != null) {
                sharedState.put(key, value);
            }
        }
    }
}
