package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Premium Package configuration (managed by Admin).
 *
 * <p>Vendor chọn một gói Premium, mock-checkout, và shop sẽ được gắn cờ
 * {@code premiumActive=true} trong suốt khoảng {@code durationDays}.</p>
 *
 * <p>Đây là catalog gói, không phải subscription thực tế. Subscription thực
 * được lưu trong {@link VendorSubscription}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "premium_packages")
public class PremiumPackage {

    @Id
    private String id;

    /** Tên hiển thị, vd: "Premium 1 tháng", "Premium 3 tháng", "Premium 12 tháng". */
    private String name;

    /** Mô tả ngắn gọi các đặc quyền của gói. */
    private String description;

    /** Giá cố định (admin config). */
    private BigDecimal price;

    /** Số ngày premium có hiệu lực sau khi mua (vd: 30 / 90 / 365). */
    private int durationDays;

    /**
     * Còn bán hay không. Soft-delete: admin set false thay vì xóa cứng để
     * giữ reference ở các subscription lịch sử.
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Độ ưu tiên hiển thị trên UI vendor (cao = nổi bật hơn).
     * Dùng cho sort trong card grid và có thể dùng cho boost ranking sau này.
     */
    @Builder.Default
    private int priority = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
