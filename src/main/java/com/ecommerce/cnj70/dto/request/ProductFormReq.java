package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.document.ProductSpecification;
import com.ecommerce.cnj70.document.ProductVariant;
import com.ecommerce.cnj70.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ProductFormReq {

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String brand;

    private Integer warrantyMonths;

    private String manufacturer;

    private String manufacturerAddress;

    private String description;

    private String richDescription;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stock;

    private String categoryId;

    private List<String> imageUrls;

    private List<ProductSpecification> specifications;

    private List<ProductVariant> variants;

    private ProductStatus status;
}
