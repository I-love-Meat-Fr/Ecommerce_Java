package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.VoucherType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vouchers")
public class Voucher {

    @Id
    private String id;

    @Indexed(unique = true)  // Code không trùng lặp
    private String code;

    private String name;

    private VoucherType type;  // SHOP hoặc WEB

    private String shopId;  // null nếu là WEB

    private DiscountType discountType;  // PERCENT hoặc AMOUNT

    private BigDecimal discountValue;  // Giá trị giảm

    private BigDecimal minOrderValue;  // Đơn hàng tối thiểu

    private int quantity;  // Tổng số lượt sử dụng

    private int used;  // Số lượt đã dùng

    private LocalDateTime startDate;  // Ngày bắt đầu

    private LocalDateTime endDate;  // Ngày kết thúc

    @Builder.Default
    private boolean active = true;  // Có đang kích hoạt không

    private String createdBy;  // Ai tạo (admin/vendor)

    @CreatedDate
    private LocalDateTime createdAt;
}
