package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.LegalDocument;
import com.ecommerce.cnj70.dto.request.LegalDocumentUpdateRequest;
import com.ecommerce.cnj70.enums.LegalDocumentType;
import com.ecommerce.cnj70.service.LegalDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Admin UI cho quản lý LegalDocument (Điều khoản, Chính sách bảo mật, ...).
 *
 * <p>REST API tương ứng:
 * {@link com.ecommerce.cnj70.controller.web.LegalDocumentController}.</p>
 *
 * <p>Routes:</p>
 * <ul>
 *   <li>{@code GET  /admin/legal} - Danh sách 8 loại tài liệu</li>
 *   <li>{@code GET  /admin/legal/{type}/edit} - Form edit</li>
 *   <li>{@code POST /admin/legal/{type}/edit} - Lưu (tăng version)</li>
 * </ul>
 *
 * Security: /admin/** đã bị SecurityConfig.hasRole("ADMIN") lock.
 */
@Controller
@RequestMapping("/admin/legal")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLegalController {

    private final LegalDocumentService legalDocumentService;

    @GetMapping
    public String list(Model model) {
        List<LegalDocument> docs = legalDocumentService.getAllDocuments();
        model.addAttribute("docs", docs);
        model.addAttribute("types", LegalDocumentType.values());
        return "admin/legal-list";
    }

    @GetMapping("/{type}/edit")
    public String editForm(@PathVariable LegalDocumentType type, Model model) {
        LegalDocument doc = legalDocumentService.getByType(type);
        model.addAttribute("doc", doc);
        model.addAttribute("type", type);
        model.addAttribute("updateReq", new LegalDocumentUpdateRequest(
                doc.getTitle(), doc.getContent(), doc.getMetaDescription()));
        return "admin/legal-edit";
    }

    @PostMapping("/{type}/edit")
    public String update(@PathVariable LegalDocumentType type,
                         @Valid @ModelAttribute("updateReq") LegalDocumentUpdateRequest req,
                         @AuthenticationPrincipal UserDetails user,
                         RedirectAttributes ra) {
        try {
            String updatedBy = user != null ? user.getUsername() : "ADMIN";
            legalDocumentService.updateDocument(type, req, updatedBy);
            ra.addFlashAttribute("flashSuccess",
                    "Đã cập nhật " + type.name() + " (version mới).");
        } catch (Exception e) {
            ra.addFlashAttribute("flashError", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/legal/" + type.name() + "/edit";
    }
}
