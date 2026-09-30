package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.LegalDocument;
import com.ecommerce.cnj70.enums.LegalDocumentType;
import com.ecommerce.cnj70.service.LegalDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Public pages cho Legal Documents.
 *
 * <p>Ai cũng xem được (không cần auth). URL dạng {@code /legal/terms},
 * {@code /legal/privacy}, ... map sang {@link LegalDocumentType}.</p>
 */
@Controller
@RequestMapping("/legal")
@RequiredArgsConstructor
public class PublicLegalController {

    private final LegalDocumentService legalDocumentService;

    /**
     * Hiển thị tài liệu pháp lý theo type.
     */
    @GetMapping("/{type}")
    public String view(@PathVariable String type, Model model) {
        LegalDocumentType docType;
        try {
            docType = LegalDocumentType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "redirect:/404";
        }
        LegalDocument doc = legalDocumentService.getByType(docType);
        model.addAttribute("doc", doc);
        model.addAttribute("type", docType);
        return "web/legal-view";
    }

    /**
     * Trang index tổng hợp tất cả tài liệu pháp lý (footer link).
     */
    @GetMapping
    public String index(Model model) {
        model.addAttribute("docs", legalDocumentService.getAllDocuments());
        return "web/legal-index";
    }
}
