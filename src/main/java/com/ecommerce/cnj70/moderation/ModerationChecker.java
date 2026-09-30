package com.ecommerce.cnj70.moderation;

/**
 * Interface trung tâm của <b>Chain of Responsibility Pattern</b> cho Auto Moderation Pipeline.
 *
 * <p>Mỗi implementation đại diện cho 1 bước kiểm tra trong pipeline. Spring tự động
 * inject tất cả bean implement interface này; {@link ModerationPipelineService} chạy
 * lần lượt theo thứ tự Spring quyết định (hoặc theo {@link #order()}).</p>
 *
 * <p><b>Cách thêm checker mới:</b></p>
 * <ol>
 *   <li>Tạo class mới implement {@code ModerationChecker}.</li>
 *   <li>Annotate {@code @Component} để Spring đăng ký bean.</li>
 *   <li>Override {@link #supports(ModerationContext)} nếu checker chỉ áp dụng
 *       cho một số loại đối tượng nhất định (vd chỉ Product).</li>
 *   <li>Implement {@link #check(ModerationContext)} trả về
 *       {@link ModerationDecision}.</li>
 * </ol>
 *
 * <p>KHÔNG cần sửa orchestrator. Đây là lợi thế chính của Chain of Responsibility.</p>
 */
public interface ModerationChecker {

    /**
     * ID định danh checker, dùng để log + gắn vào {@code ReportCase.autoFlags}.
     * Quy ước: CONSTANT_CASE (vd {@code "BLACKLIST_WORD"}, {@code "DUPLICATE_CONTENT"}).
     *
     * @return id không null, không trống
     */
    String id();

    /**
     * Thứ tự ưu tiên (nhỏ chạy trước). Mặc định 100. Checker nặng/nhanh nên để
     * số nhỏ để short-circuit sớm.
     */
    default int order() {
        return 100;
    }

    /**
     * Checker này có áp dụng cho context hiện tại không?
     * Mặc định: true (áp dụng cho cả Product &amp; Review).
     *
     * <p>Override khi checker chỉ dành cho Product (vd check giá bất thường)
     * hoặc chỉ cho Review (vd check rating spam).</p>
     */
    default boolean supports(ModerationContext context) {
        return context != null;
    }

    /**
     * Thực hiện check trên context. Trả về {@link ModerationDecision} mô tả
     * kết quả. KHÔNG được trả null — dùng {@link ModerationDecision#pass()}.
     *
     * <p><b>Idempotent &amp; side-effect free:</b> checker không được thay đổi
     * document gốc, không gọi repository.save(). Việc apply kết quả là của
     * caller.</p>
     */
    ModerationDecision check(ModerationContext context);
}
