package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.ImageHashService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * C3 — Duplicate product check (task #4.0).
 *
 * <p>Phát hiện khi sản phẩm mới dùng cùng ảnh với sản phẩm đã tồn tại.
 * Cách tiếp cận tối giản:
 *
 * <pre>
 *   Product.imageUrls (đã có sẵn)
 *     ↓
 *   với mỗi URL:
 *     ↓
 *   productRepository.findFirstByImageUrlsContainingAndIdNot(url, currentId)
 *     ↓
 *   match → SOFT_FLAG "DUPLICATE_IMAGE" (để Moderator xem xét; không auto reject)
 *     ↓
 *   không match → PASS
 * </pre>
 *
 * <p>Quyết định thiết kế:
 * <ul>
 *   <li>Dùng URL-based matching thay vì binary hash — không tải file.</li>
 *   <li>Chỉ SOFT_FLAG (không HARD_REJECT) vì 2 sản phẩm có thể hợp lệ
 *       dùng cùng ảnh (vd logo thương hiệu, catalog chính hãng).</li>
 *   <li>Loại trừ chính product khỏi kết quả query (tránh tự match).</li>
 *   <li>Compute &amp; cache hash vào {@code sharedState} để tiết kiệm recompute
 *       (dù {@link ImageHashCheck} chưa dùng ngay).</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DuplicateProductCheck implements AutoCheckStrategy {

    /** ID cố định, dùng để log + tạo autoFlag. */
    public static final String ID = "DUPLICATE_PRODUCT";

    /** FlagCode trong {@code AutoModerationFlag#DUPLICATE}. */
    public static final String FLAG_CODE = "DUPLICATE_IMAGE";

    private final ProductRepository productRepository;
    private final ImageHashService imageHashService;

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

        String currentId = product.getId();

        for (String url : imageUrls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            // Bỏ qua URL placeholder ở bước duplicate — ImageHashCheck sẽ xử lý.
            if (imageHashService.isStockPlaceholder(url)) {
                continue;
            }

            Optional<Product> match = productRepository
                    .findFirstByImageUrlsContainingAndIdNot(url, currentId);

            // Cache hash vào sharedState để check khác tái sử dụng.
            String hash = imageHashService.hashUrl(url);
            ctx.putShared("imageHash:" + url, hash);

            if (match.isPresent()) {
                Product other = match.get();
                String msg = "Ảnh sản phẩm trùng với sản phẩm khác (id=" + other.getId()
                        + ", name='" + other.getName() + "')";
                log.info("[DuplicateProduct] SOFT_FLAG product id={} matched image with product id={}",
                        currentId, other.getId());
                return AutoCheckVerdict.softFlag(FLAG_CODE, msg);
            }
        }

        return AutoCheckVerdict.pass();
    }
}
