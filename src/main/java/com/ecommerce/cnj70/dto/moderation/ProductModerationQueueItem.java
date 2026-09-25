package com.ecommerce.cnj70.dto.moderation;

import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phase 2A — Product Moderation Queue Item.
 *
 * <p>Single row in the moderator product queue. Aggregates minimal data
 * needed to decide whether to drill into product detail. Vendor/Shop
 * context is included inline so the queue page does not need N+1 lookups
 * for vendor names.</p>
 *
 * <p>Auto Moderation fields are optional — if the Auto Moderation backend
 * is not yet wired in, those fields are null.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductModerationQueueItem {
    private String productId;
    private String productName;
    private String shopId;
    private String shopName;
    private String vendorId;
    private String vendorName;
    private String categoryId;
    private String categoryName;
    private BigDecimal price;
    private ProductStatus productStatus;
    private ModerationStatus moderationStatus;
    private AutoModerationResult autoModerationResult;
    private LocalDateTime createdAt;
}
