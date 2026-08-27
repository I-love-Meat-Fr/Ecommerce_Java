package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonSpecs {

    // Hãng sản phẩm (Apple, Samsung, Sony...)
    private String brand;

    // Bảo hành (số tháng)
    private Integer warrantyMonths;

    // Nhà sản xuất
    private String manufacturerName;

    // Địa chỉ nhà sản xuất
    private String manufacturerAddress;
}