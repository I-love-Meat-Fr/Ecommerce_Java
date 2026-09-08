package com.ecommerce.cnj70.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Phase 17 — Banner / PR Document.
 *
 * Phạm vi Phase 17:
 *   - title, description, image, link, status (PUBLISHED/UNPUBLISHED)
 *   - position (string, hiển thị vị trí, optional)
 *   - createdAt, updatedAt
 *
 * KHÔNG bao gồm (Phase 17 LOCK 4/5 — chờ Business Contract):
 *   - startAt / endAt scheduling
 *   - priority algorithm
 *   - campaign / advertising fields
 *
 * Status mapping:
 *   - PUBLISHED: Customer được thấy
 *   - UNPUBLISHED: Admin chưa xuất bản / tạm ẩn
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "banners")
public class Banner {

    @Id
    private String id;

    private String title;

    private String description;

    /**
     * CTA Tag label: ví dụ "MEGA SALE", "FREESHIP 100%"
     */
    private String tag;

    /**
     * Banner CTA icon (Font Awesome class) — ví dụ "fas fa-bolt"
     */
    private String tagIcon;

    /**
     * URL ảnh banner — lưu dưới dạng "/uploads/xxx.jpg" qua StorageService.
     */
    @Indexed
    private String imageUrl;

    /**
     * Click-through URL khi Customer click banner.
     */
    private String link;

    /**
     * CTA text — ví dụ "Mua sắm ngay", "Đăng ký ngay"
     */
    private String ctaText;

    /**
     * CTA icon (Font Awesome class).
     */
    private String ctaIcon;

    /**
     * Theme color: "primary" | "teal" | "purple" | "orange" | "red"
     * Để CSS render banner-slide-1/2/3 đúng theme.
     */
    private String theme;

    /**
     * Banner status theo Phase 17 contract:
     *   - PUBLISHED: Customer thấy được
     *   - UNPUBLISHED: Ẩn
     */
    @Indexed
    private String status;

    /**
     * Vị trí hiển thị (string free): ví dụ "HERO_SLIDER", "PROMO_GRID".
     * Phase 17 chỉ dùng để lưu thông tin — KHÔNG tự định nghĩa priority.
     */
    @Indexed
    private String position;

    /**
     * Thứ tự hiển thị (số nguyên) — chỉ sort, không có priority algorithm.
     * LOCK 5 Phase 17: không tự tạo priority algorithm, chỉ dùng sortOrder cơ bản.
     */
    private Integer sortOrder;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
