package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

    @Builder.Default
    private List<ProductSpecification> specifications = new ArrayList<>();

    private BigDecimal price;

    @Builder.Default
    private int stock = 0;

    private String sku;
}
