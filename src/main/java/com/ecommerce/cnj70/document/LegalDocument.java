package com.ecommerce.cnj70.document;

import com.ecommerce.cnj70.enums.LegalDocumentType;
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
 * TASK #21 — LegalDocument entity.
 *
 * Lưu 8 loại tài liệu pháp lý bắt buộc.
 * Versioning: mỗi lần update nội dung → tăng version + set effectiveDate.
 * Seeder tự động tạo 8 document khi collection rỗng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "legal_documents")
public class LegalDocument {

    @Id
    private String id;

    /** Loại tài liệu (TERMS, PRIVACY, RETURN, SHIPPING, WARRANTY, COMPLAINT, PAYMENT, SITEMAP) */
    @Indexed(unique = true)
    private LegalDocumentType type;

    /** Tiêu đề hiển thị (VD: "Điều khoản sử dụng") */
    private String title;

    /** Nội dung HTML/Markdown của tài liệu */
    @Builder.Default
    private String content = "";

    /**
     * Version hiện tại.
     * Tăng mỗi khi nội dung thay đổi.
     */
    @Builder.Default
    private int version = 1;

    /**
     * Ngày có hiệu lực của phiên bản này.
     */
    @Builder.Default
    private LocalDateTime effectiveDate = LocalDateTime.now();

    /** Người tạo / cập nhật gần nhất */
    private String updatedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /** Rendered HTML content (có thể cache) */
    @Builder.Default
    private String renderedContent = "";

    /** Mô tả ngắn cho SEO/footer */
    private String metaDescription;
}
