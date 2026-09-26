package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * C6 — Forbidden category check (task #7.0).
 *
 * <p>Dựa vào {@link ForbiddenCategoryPolicy} để check
 * {@code Product.categoryId} có nằm trong blacklist hay không.
 *
 * <ul>
 *   <li>Match → HARD_REJECT với flagCode {@code "FORBIDDEN_CAT"}.</li>
 *   <li>Không match → PASS.</li>
 *   <li>Product không có categoryId → PASS (không crash; để các check
 *       khác hoặc Moderator xử lý).</li>
 *   <li>Config rỗng → PASS.</li>
 * </ul>
 *
 * <p>Orchestrator ngắn mạch (short-circuit) khi gặp HARD_REJECT nên chỉ
 * cần trả đúng verdict theo contract — không cần tự xử lý logic dừng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForbiddenCategoryCheck implements AutoCheckStrategy {

    /** Check ID cố định, dùng cho log + autoFlag. */
    public static final String ID = "FORBIDDEN_CATEGORY";

    /** FlagCode trong {@link com.ecommerce.cnj70.enums.AutoModerationFlag#CATEGORY}. */
    public static final String FLAG_CODE = "FORBIDDEN_CAT";

    private final ForbiddenCategoryPolicy policy;

    @Override
    public String id() {
        return ID;
    }

    @Override
    public AutoCheckVerdict check(AutoCheckContext ctx) {
        if (ctx == null || ctx.getProduct() == null) {
            return AutoCheckVerdict.pass();
        }
        Product product = ctx.getProduct();

        String categoryId = product.getCategoryId();
        if (categoryId == null || categoryId.isBlank()) {
            return AutoCheckVerdict.pass();
        }

        if (policy.isForbidden(categoryId)) {
            String msg = "Sản phẩm thuộc danh mục bị cấm (categoryId=" + categoryId + ")";
            log.warn("[ForbiddenCategory] HARD_REJECT product id={} categoryId={}",
                    product.getId(), categoryId);
            return AutoCheckVerdict.hardReject(FLAG_CODE, msg);
        }

        return AutoCheckVerdict.pass();
    }
}
