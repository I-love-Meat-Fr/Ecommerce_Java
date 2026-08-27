package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.document.CommonSpecs;
import com.ecommerce.cnj70.document.SpecItem;
import com.ecommerce.cnj70.document.Variant;
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

    // Mô tả sản phẩm (TinyMCE rich text HTML)
    private String description;

    @NotNull(message = "Giá sản phẩm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá sản phẩm phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Số lượng tồn kho không được âm")
    private Integer stock;

    private String categoryId;

    private List<String> imageUrls;

    // Ảnh đại diện chính
    private String mainImage;

    // Thông số kỹ thuật chung (hãng, bảo hành, NSX, địa chỉ NSX)
    private CommonSpecs commonSpecs;

    // Thông số kỹ thuật tuỳ chỉnh (vendor tự tạo tên, giá trị, đơn vị, ghi chú)
    @Builder.Default
    private List<SpecItem> customSpecs = new ArrayList<>();

    // Danh sách biến thể (động, thêm bằng nút)
    @Builder.Default
    private List<Variant> variants = new ArrayList<>();

    private ProductStatus status;
}