package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.ProductStatus;
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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "products")
public class Product {

    @Id
    private String id;

    private String shopId;

    private String shopName;

    @Indexed
    private String name;

    private String description;

    private BigDecimal price;

    private int stock;

    private String categoryId;

    private String categoryName;

    // Ảnh đại diện chính của sản phẩm (lấy từ variant đầu tiên nếu có)
    private String mainImage;

    private List<String> imageUrls;

    private String thumbnailUrl;

    // Thông số kỹ thuật chung (hãng, bảo hành, NSX...)
    private CommonSpecs commonSpecs;

    // Thông số kỹ thuật tuỳ chỉnh (vendor tự tạo)
    @Builder.Default
    private List<SpecItem> customSpecs = new ArrayList<>();

    // Danh sách biến thể (cấu hình/màu sắc khác nhau)
    @Builder.Default
    private List<Variant> variants = new ArrayList<>();

    @Builder.Default
    private ProductStatus status = ProductStatus.DRAFT;

    @Builder.Default
    private double rating = 0.0;

    @Builder.Default
    private int reviewCount = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
