package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.LegalDocument;
import com.ecommerce.cnj70.dto.request.LegalDocumentUpdateRequest;
import com.ecommerce.cnj70.enums.LegalDocumentType;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.LegalDocumentRepository;
import com.ecommerce.cnj70.service.LegalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LegalDocumentServiceImpl implements LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;

    @Override
    public List<LegalDocument> getAllDocuments() {
        return legalDocumentRepository.findAll();
    }

    @Override
    public LegalDocument getByType(LegalDocumentType type) {
        return legalDocumentRepository.findByType(type)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy tài liệu: " + type.name()));
    }

    @Override
    public LegalDocument getByTypeAdmin(LegalDocumentType type) {
        return getByType(type);
    }

    @Override
    public LegalDocument updateDocument(LegalDocumentType type, LegalDocumentUpdateRequest request, String updatedBy) {
        LegalDocument doc = getByType(type);

        doc.setTitle(request.getTitle());
        doc.setContent(request.getContent());
        doc.setRenderedContent(request.getContent());
        doc.setMetaDescription(request.getMetaDescription());
        doc.setVersion(doc.getVersion() + 1);
        doc.setEffectiveDate(LocalDateTime.now());
        doc.setUpdatedBy(updatedBy);

        return legalDocumentRepository.save(doc);
    }
}
