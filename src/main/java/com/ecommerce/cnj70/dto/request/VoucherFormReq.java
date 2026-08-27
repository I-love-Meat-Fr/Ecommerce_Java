package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.VoucherType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherFormReq {

    @NotBlank(message = "Mã voucher không được trống")
    @Size(min = 3, max = 20, message = "Mã voucher 3-20 ký tự")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Mã voucher chỉ chứa chữ và số")
    private String code;

    @NotBlank(message = "Tên voucher không được trống")
    @Size(max = 100, message = "Tên voucher tối đa 100 ký tự")
    private String name;

    private VoucherType type;  // SHOP hoặc WEB

    private DiscountType discountType;  // PERCENT hoặc AMOUNT

    @NotNull(message = "Giá trị giảm không được trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải > 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Giá trị đơn hàng tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @NotNull(message = "Số lượt sử dụng không được trống")
    @Min(value = 1, message = "Số lượt sử dụng tối thiểu là 1")
    private Integer quantity;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}
