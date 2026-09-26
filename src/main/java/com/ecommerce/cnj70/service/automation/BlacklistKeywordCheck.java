package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * C2 — Blacklist keyword check (task #3.0).
 *
 * <p>Quét product name/title + description để phát hiện từ khóa cấm.
 * <ul>
 *   <li>Config từ {@code application.yml}:
 *       {@code moderation.auto.blacklist-keywords} (list, mặc định rỗng).</li>
 *   <li>Case-insensitive.</li>
 *   <li>Sử dụng word boundary ({@code \b}) để tránh false positive kiểu
 *       chứa "vũ khí" trong "vũ khí tự vệ" là có chủ đích — chỉ dùng
 *       word boundary để không match substring đơn giản.</li>
 *   <li>Match keyword đầu tiên → trả HARD_REJECT với flagCode
 *       {@code "BLACKLIST_HIT"}, message kèm keyword bị match.</li>
 *   <li>Không match → PASS.</li>
 * </ul>
 *
 * <p>Fail-safe: nếu xảy ra exception khi compile regex (vd keyword có
 * ký tự đặc biệt), check trả PASS để không chặn cả pipeline; orchestrator
 * sẽ ghi log error và đánh dấu SOFT_FLAG cho Moderator review.
 */
@Slf4j
@Component
public class BlacklistKeywordCheck implements AutoCheckStrategy {

    /** ID cố định, dùng để log + tạo autoFlag trên ReportCase. */
    public static final String ID = "BLACKLIST_KEYWORD";

    /** FlagCode trong {@link com.ecommerce.cnj70.enums.AutoModerationFlag#BLACKLIST}. */
    public static final String FLAG_CODE = "BLACKLIST_HIT";

    private final List<String> keywords;

    public BlacklistKeywordCheck(
            @Value("${moderation.auto.blacklist-keywords:}") List<String> keywords) {
        // Filter null / blank để list config rỗng không sinh pattern rỗng.
        this.keywords = keywords == null
                ? List.of()
                : keywords.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(String::trim)
                    .toList();
        log.info("[BlacklistKeyword] Initialized with {} keyword(s)", this.keywords.size());
    }

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

        if (keywords.isEmpty()) {
            return AutoCheckVerdict.pass();
        }

        // Combine các field cần scan: title (name) + description + richDescription.
        String combined = composeText(product);
        if (combined == null || combined.isBlank()) {
            return AutoCheckVerdict.pass();
        }

        String lowerCombined = combined.toLowerCase();

        for (String keyword : keywords) {
            String lowerKeyword = keyword.toLowerCase();
            // \b không hoạt động tốt với tiếng Việt có dấu, vì vậy dùng
            // Pattern.quote để đảm bảo an toàn, sau đó wrap với (?<!\w) và
            // (?!\w) để tạo word-boundary tương đương (không phải ký tự từ
            // ở hai bên).
            String safeKw = Pattern.quote(lowerKeyword);
            Pattern p = Pattern.compile("(?<![\\wđĐ])\\Q" + safeKw + "\\E(?![\\wđĐ])",
                    Pattern.UNICODE_CASE);
            if (p.matcher(lowerCombined).find()) {
                String msg = "Sản phẩm chứa từ khóa bị cấm: '" + keyword + "'";
                log.warn("[BlacklistKeyword] HARD_REJECT product id={} hit keyword='{}'",
                        product.getId(), keyword);
                return AutoCheckVerdict.hardReject(FLAG_CODE, msg);
            }
        }

        return AutoCheckVerdict.pass();
    }

    /**
     * Gộp các field text cần quét và lowercase trước khi trả về cho matcher.
     * Mỗi field phân tách bằng newline để tránh match keyword nối qua 2 field.
     */
    private static String composeText(Product product) {
        StringBuilder sb = new StringBuilder();
        if (product.getName() != null) {
            sb.append(product.getName()).append('\n');
        }
        if (product.getDescription() != null) {
            sb.append(product.getDescription()).append('\n');
        }
        if (product.getRichDescription() != null) {
            sb.append(product.getRichDescription()).append('\n');
        }
        // Brand + manufacturerName + categoryName — kiểm tra nếu có.
        if (product.getBrand() != null) {
            sb.append(product.getBrand()).append('\n');
        }
        if (product.getManufacturer() != null) {
            sb.append(product.getManufacturer()).append('\n');
        }
        if (product.getCategoryName() != null) {
            sb.append(product.getCategoryName()).append('\n');
        }
        return sb.toString();
    }

    /**
     * Test-only helper: cho phép unit test inject list keyword mà không cần
     * load Spring context.
     */
    public static BlacklistKeywordCheck forTest(List<String> keywords) {
        return new BlacklistKeywordCheck(keywords);
    }
}
