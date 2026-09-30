package com.ecommerce.cnj70.moderation;

import com.ecommerce.cnj70.document.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation của {@link ModerationContext} cho {@code Product}.
 *
 * <p>Gộp {@code Product.name + description + richDescription + brand + manufacturer}
 * thành một chuỗi {@link #getText()} để các checker (vd {@code BlacklistWordChecker})
 * chỉ cần đọc một field duy nhất.</p>
 *
 * <p>{@link #getImageUrls()} trả về {@code Product.imageUrls} để checker
 * duplicate dùng trực tiếp.</p>
 */
@Getter
public class ProductModerationContext implements ModerationContext {

    private final String targetId;
    private final ModerationTargetType targetType = ModerationTargetType.PRODUCT;
    private final String authorId; // shopId (vendor)
    private final String text;
    private final List<String> imageUrls;
    private final LocalDateTime submittedAt;
    private final SharedState sharedState;
    private final Product product; // raw reference — chỉ để checker nâng cao dùng, không bắt buộc

    @Builder
    public ProductModerationContext(Product product,
                                    SharedState sharedState,
                                    LocalDateTime submittedAt) {
        if (product == null) {
            throw new IllegalArgumentException("product không được null");
        }
        this.product = product;
        this.targetId = product.getId();
        this.authorId = product.getShopId();
        this.text = composeText(product);
        this.imageUrls = product.getImageUrls();
        this.submittedAt = submittedAt != null ? submittedAt : LocalDateTime.now();
        this.sharedState = sharedState != null ? sharedState : SharedState.builder().build();
    }

    private static String composeText(Product p) {
        StringBuilder sb = new StringBuilder();
        appendIfPresent(sb, p.getName());
        appendIfPresent(sb, p.getDescription());
        appendIfPresent(sb, p.getRichDescription());
        appendIfPresent(sb, p.getBrand());
        appendIfPresent(sb, p.getManufacturer());
        appendIfPresent(sb, p.getCategoryName());
        return sb.toString().trim();
    }

    private static void appendIfPresent(StringBuilder sb, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(value).append('\n');
        }
    }
}
