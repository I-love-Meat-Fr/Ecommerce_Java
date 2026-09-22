package com.ecommerce.cnj70.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TASK #21 — Request cập nhật LegalDocument (Admin only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalDocumentUpdateRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    private String title;

    @NotBlank(message = "Nội dung không được để trống")
    private String content;

    /** Mô tả ngắn (SEO) */
    private String metaDescription;
}
