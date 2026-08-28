package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.enums.DiscountType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho form tạo/cập nhật voucher
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherFormReq {
    
    @NotBlank(message = "Mã voucher không được để trống")
    @Size(min = 3, max = 20, message = "Mã voucher phải từ 3-20 ký tự")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Mã voucher chỉ chứa chữ hoa, số, gạch ngang và gạch dưới")
    private String code;
    
    @NotBlank(message = "Tên voucher không được để trống")
    @Size(max = 100, message = "Tên voucher tối đa 100 ký tự")
    private String name;
    
    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;
    
    @NotNull(message = "Giá trị giảm không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;
    
    // Số tiền giảm tối đa (chỉ áp dụng cho PERCENT)
    private BigDecimal maxDiscountAmount;
    
    // Giá trị đơn hàng tối thiểu
    @DecimalMin(value = "0", message = "Giá trị tối thiểu không được âm")
    private BigDecimal minOrderValue;
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
    
    // Danh sách sản phẩm áp dụng (chỉ cho voucher WEB, nếu null/empty = áp dụng tất cả)
    private List<String> productIds;
    
    // Ngày bắt đầu (null = bắt đầu ngay)
    private LocalDateTime startDate;
    
    // Ngày kết thúc
    private LocalDateTime endDate;
}
