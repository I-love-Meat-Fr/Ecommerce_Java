package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight view-model wrapper used to render the Flash Deal strip
 * on the home page. Holds a product reference together with the derived
 * statistics (discount, sold count) so the template doesn't need to call
 * helper methods on every render.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlashSaleStat {

    private Product product;

    private boolean hasDiscount;

    private int discountPercent;

    private int soldCount;
}