package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.LegalDocument;
import com.ecommerce.cnj70.dto.request.LegalDocumentUpdateRequest;
import com.ecommerce.cnj70.enums.LegalDocumentType;
import com.ecommerce.cnj70.service.LegalDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TASK #21 — Public API cho legal documents.
 *
 * - GET public: ai cũng xem được
 * - PUT /admin: chỉ Admin
 */
@RestController
@RequestMapping("/api/legal")
@RequiredArgsConstructor
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping
    public ResponseEntity<List<LegalDocument>> getAllDocuments() {
        return ResponseEntity.ok(legalDocumentService.getAllDocuments());
    }

    @GetMapping("/{type}")
    public ResponseEntity<LegalDocument> getByType(@PathVariable LegalDocumentType type) {
        return ResponseEntity.ok(legalDocumentService.getByType(type));
    }

    /**
     * Update nội dung tài liệu (chỉ Admin).
     */
    @PutMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LegalDocument> updateDocument(
            @PathVariable LegalDocumentType type,
            @Valid @RequestBody LegalDocumentUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String updatedBy = userDetails != null ? userDetails.getUsername() : "ADMIN";
        return ResponseEntity.ok(legalDocumentService.updateDocument(type, request, updatedBy));
    }
}
