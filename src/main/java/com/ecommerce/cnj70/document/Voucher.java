package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.VoucherType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Document Voucher - lưu trữ thông tin voucher giảm giá
 * - Voucher SHOP: vendor tạo cho shop của mình
 * - Voucher WEB: admin tạo cho toàn hệ thống, có thể áp dụng cho sản phẩm cụ thể (như Shopee)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vouchers")
public class Voucher {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String code;  // Mã voucher, duy nhất
    
    private String name;  // Tên hiển thị voucher
    
    private VoucherType type;  // SHOP hoặc WEB
    
    // Chỉ áp dụng cho voucher SHOP
    private String shopId;
    private String shopName;
    
    // Chỉ áp dụng cho voucher WEB - danh sách sản phẩm được áp dụng
    // Nếu rỗng/null = áp dụng cho tất cả sản phẩm
    @Builder.Default
    private List<String> productIds = new ArrayList<>();
    
    private DiscountType discountType;  // PERCENT hoặc AMOUNT
    
    private BigDecimal discountValue;  // Giá trị giảm
    
    private BigDecimal maxDiscountAmount;  // Số tiền giảm tối đa (chỉ áp dụng cho PERCENT)
    
    private BigDecimal minOrderValue;  // Giá trị đơn hàng tối thiểu để áp dụng
    
    private int quantity;  // Tổng số lượt sử dụng
    
    @Builder.Default
    private int used = 0;  // Số lượt đã sử dụng
    
    private LocalDateTime startDate;  // Ngày bắt đầu
    
    private LocalDateTime endDate;  // Ngày kết thúc
    
    @Builder.Default
    private boolean active = true;  // Còn active hay không
    
    private String createdBy;  // ID người tạo (vendor hoặc admin)
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    /**
     * Kiểm tra voucher có còn khả dụng không
     */
    public boolean isAvailable() {
        if (!active) return false;
        if (used >= quantity) return false;
        
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;
        
        return true;
    }
    
    /**
     * Tính số lượt còn lại
     */
    public int getRemainingQuantity() {
        return Math.max(0, quantity - used);
    }
}
