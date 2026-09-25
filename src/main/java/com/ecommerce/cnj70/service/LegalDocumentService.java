package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.LegalDocument;
import com.ecommerce.cnj70.dto.request.LegalDocumentUpdateRequest;
import com.ecommerce.cnj70.enums.LegalDocumentType;

import java.util.List;

/**
 * TASK #21 — Service quản lý LegalDocument.
 */
public interface LegalDocumentService {

    /** Lấy tất cả legal documents (public - không có content chi tiết) */
    List<LegalDocument> getAllDocuments();

    /** Lấy document theo type (public) */
    LegalDocument getByType(LegalDocumentType type);

    /** Lấy document chi tiết cho Admin */
    LegalDocument getByTypeAdmin(LegalDocumentType type);

    /** Cập nhật nội dung document (Admin only) */
    LegalDocument updateDocument(LegalDocumentType type, LegalDocumentUpdateRequest request, String updatedBy);
}
