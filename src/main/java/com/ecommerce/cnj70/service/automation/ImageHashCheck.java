package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.service.ImageHashService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * C5 — Image content / placeholder check (task #6.0).
 *
 * <p>Phát hiện khi sản phẩm dùng ảnh placeholder / stock photo thay vì
 * ảnh thật. Theo rule "không tải file/image nguy hiểm": check dựa trên URL
 * pattern + extension whitelist chứ không download binary.
 *
 * <ul>
 *   <li>URL khớp {@link ImageHashService#isStockPlaceholder(String)} →
 *       SOFT_FLAG "STOCK_IMAGE".</li>
 *   <li>URL extension không thuộc whitelist (.jpg/.jpeg/.png/.webp) →
 *       SOFT_FLAG "BAD_IMAGE_EXT".</li>
 *   <li>Không có ảnh nào → PASS (để check khác xử lý).</li>
 * </ul>
 *
 * <p>Severity là SOFT_FLAG (không HARD_REJECT) vì vendor có thể tạm thời
 * dùng placeholder khi chưa kịp upload ảnh thật — Moderator xem xét.
 */
@Slf4j
@Component
public class ImageHashCheck implements AutoCheckStrategy {

    /** ID cố định, dùng cho log + autoFlag. */
    public static final String ID = "IMAGE_CONTENT";

    /** FlagCode cho placeholder/stock image detection. */
    public static final String FLAG_PLACEHOLDER = "STOCK_IMAGE";

    /** FlagCode cho extension không hợp lệ. */
    public static final String FLAG_BAD_EXT = "BAD_IMAGE_EXT";

    private final ImageHashService imageHashService;
    private final List<String> allowedExtensions;

    /**
     * Spring constructor — auto-wire.
     * @param imageHashService     hashing service
     * @param allowedExtensions   whitelist extension config từ {@code moderation.auto.allowed-image-extensions}
     */
    public ImageHashCheck(
            ImageHashService imageHashService,
            @Value("${moderation.auto.allowed-image-extensions:jpg,jpeg,png,webp}")
            List<String> allowedExtensions) {
        this.imageHashService = imageHashService;
        this.allowedExtensions = (allowedExtensions == null || allowedExtensions.isEmpty())
                ? List.of("jpg", "jpeg", "png", "webp")
                : allowedExtensions.stream()
                    .filter(s -> s != null && !s.isBlank())
                    .map(s -> s.toLowerCase().trim())
                    .toList();
        log.info("[ImageHash] Initialized with allowed extensions: {}", this.allowedExtensions);
    }

    /** Test-only constructor. */
    public static ImageHashCheck forTest(ImageHashService imageHashService, List<String> allowed) {
        return new ImageHashCheck(imageHashService, allowed == null ? List.of() : allowed);
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

        List<String> imageUrls = product.getImageUrls();
        if (imageUrls == null || imageUrls.isEmpty()) {
            return AutoCheckVerdict.pass();
        }

        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            // Check 1: URL là placeholder / stock photo → SOFT_FLAG.
            if (imageHashService.isStockPlaceholder(url)) {
                String msg = "Ảnh sản phẩm dùng placeholder/stock photo: " + url;
                log.info("[ImageHash] SOFT_FLAG product id={} stock placeholder detected",
                        product.getId());
                return AutoCheckVerdict.softFlag(FLAG_PLACEHOLDER, msg);
            }

            // Check 2: Extension không thuộc whitelist → SOFT_FLAG.
            String ext = extractExtension(url);
            if (ext != null && !allowedExtensions.contains(ext)) {
                String msg = "Ảnh có định dạng không được phép: ." + ext;
                log.info("[ImageHash] SOFT_FLAG product id={} bad extension: {}",
                        product.getId(), ext);
                return AutoCheckVerdict.softFlag(FLAG_BAD_EXT, msg);
            }
        }

        return AutoCheckVerdict.pass();
    }

    /** Lấy extension từ URL — phần sau dấu chấm cuối cùng trước query string. */
    private static String extractExtension(String url) {
        if (url == null) return null;
        // Bỏ query string trước.
        int q = url.indexOf('?');
        String path = q >= 0 ? url.substring(0, q) : url;
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            path = path.substring(lastSlash + 1);
        }
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) {
            return null;
        }
        return path.substring(dot + 1).toLowerCase();
    }
}
