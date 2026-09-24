package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "carts")
public class Cart {

    @Id
    private String id;

    @Indexed(unique = true)
    private String userId;

    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    private LocalDateTime updatedAt;

    /**
     * Mã voucher đang áp dụng trên Cart.
     * - Được lưu trong Cart document (MongoDB) để persist qua mọi request, không phụ
     *   thuộc HttpSession (vì app chạy SessionCreationPolicy.STATELESS).
     * - null = không có voucher đang áp dụng.
     * - Việc tính discount/finalTotal luôn lấy FRESH từ VoucherService để tránh
     *   cache trạng thái cũ khi voucher bị xóa/hết hạn/đổi điều kiện.
     */
    private String appliedVoucherCode;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItem {
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
        private String shopId;
        private String shopName;
        private Integer stock;
    }
}
