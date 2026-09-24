package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Phase 2A — Product Moderation Context.
 *
 * <p>Aggregated DTO passed to the Product Detail view. Reuses existing
 * {@code Product} entity fields via composition; adds Moderation-specific
 * fields.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductModerationContext {
    private String productId;
    private String productName;
    private String description;
    private String categoryId;
    private String categoryName;
    private ProductStatus productStatus;
    private ModerationStatus moderationStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Shop / vendor context (Phase 2A §22)
    private String shopId;
    private String shopName;
    private ShopStatus shopStatus;
    private String vendorId;
    private String vendorEmail;
    private String vendorName;

    // Auto Moderation result (Phase 2A §24)
    private AutoModerationResult autoModerationResult;
}
