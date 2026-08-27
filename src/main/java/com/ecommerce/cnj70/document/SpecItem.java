package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecItem {

    // Tên thông số (vendor tự đặt: "Bluetooth", "Dung lượng pin"...)
    private String name;

    // Giá trị ("5.3", "30 giờ", "i7-13700H"...)
    private String value;
}