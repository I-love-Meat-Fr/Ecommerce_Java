package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO cho Admin CRUD {@link com.ecommerce.cnj70.document.PremiumPackage}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PremiumPackageFormReq {

    @NotBlank(message = "Tên gói không được để trống")
    @Size(max = 100, message = "Tên gói tối đa 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số ngày không được để trống")
    @Min(value = 1, message = "Số ngày phải >= 1")
    @Max(value = 3650, message = "Số ngày tối đa 3650 (~10 năm)")
    private Integer durationDays;

    /** Độ ưu tiên hiển thị (cao = nổi bật hơn). Mặc định 0. */
    private Integer priority;

    /** Còn bán hay không. Mặc định true. */
    private Boolean active;
}
