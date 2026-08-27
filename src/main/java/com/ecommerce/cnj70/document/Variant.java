package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Variant {

    // ID riêng của variant (uuid ngắn, dùng cho JS quản lý)
    private String id;

    // Tên biến thể (vendor tự đặt: "i5/16GB/512GB", "256GB", "Đỏ"...)
    private String name;

    // Giá riêng của biến thể (để trống = dùng giá sản phẩm)
    private BigDecimal price;
}