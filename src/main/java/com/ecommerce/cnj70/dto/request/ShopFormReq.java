package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopFormReq {
    
    @NotBlank(message = "Tên shop không được để trống")
    @Size(min = 3, max = 100, message = "Tên shop phải từ 3 đến 100 ký tự")
    private String shopName;
    
    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;
    
    private String logoUrl;
    
    private String bannerUrl;
    
    private String contactPhone;
    
    private String contactEmail;
    
    private String address;
}
