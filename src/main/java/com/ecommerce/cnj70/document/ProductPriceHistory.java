package com.ecommerce.cnj70.document;

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

/**
 * C4 — Lịch sử giá sản phẩm (task #5.0 - PriceAnomalyCheck).
 *
 * <p>Mỗi lần sản phẩm được cập nhật giá (hoặc tạo mới), một bản ghi price
 * history được ghi lại để {@code PriceAnomalyCheck} so sánh với giá hiện
 * tại và phát hiện bất thường.
 *
 * <p>Chỉ chứa thông tin cần thiết cho MAD/median calculation; không tham
 * chiếu nặng để tránh ảnh hưởng performance của pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "product_price_history")
public class ProductPriceHistory {

    @Id
    private String id;

    @Indexed
    private String productId;

    /** Giá ghi nhận tại thời điểm. */
    private BigDecimal price;

    /** Loại action: CREATE / UPDATE / REJECTED. */
    private String action;

    /** Actor ID — null nếu auto (pipeline). */
    private String actorId;

    @CreatedDate
    private LocalDateTime recordedAt;
}
