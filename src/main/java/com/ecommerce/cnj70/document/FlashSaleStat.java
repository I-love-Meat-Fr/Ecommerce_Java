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

    /**
     * Whether the product carries a real, persisted discount. False when
     * the database has no {@code originalPrice} field for the product, so
     * the template falls back to a non-numeric badge.
     */
    private boolean hasDiscount;

    private int discountPercent;

    /**
     * Real sold count pulled directly from {@link Product#getSold()} so the
     * "ĐÃ BÁN" label reflects what is actually in the database.
     */
    private int soldCount;

    /**
     * Percentage of the original inventory that has been sold
     * ({@code sold / (sold + stock)}), capped at 99 to keep the bar from
     * visually maxing out. Used directly by the template's progress bar
     * width so the rendering matches the database snapshot.
     */
    private int progressPercent;
}