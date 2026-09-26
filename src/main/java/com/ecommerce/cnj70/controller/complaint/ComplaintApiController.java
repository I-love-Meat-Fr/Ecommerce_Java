package com.ecommerce.cnj70.controller.complaint;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.dto.complaint.CreateComplaintReq;
import com.ecommerce.cnj70.dto.complaint.ResolveComplaintReq;
import com.ecommerce.cnj70.dto.complaint.VendorRespondReq;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Phase 3 — Complaint REST API controller.
 *
 * <p>Customer / Vendor / Moderator / Admin đều gọi vào cùng
 * {@code /api/complaints/**} gateway. Ownership + role + state guards
 * enforced trong {@link ComplaintService}.</p>
 *
 * <p>Phase 3 §52 contract (REST endpoints — JSON in/out):</p>
 * <ul>
 *     <li>POST   /api/complaints                       — Customer create</li>
 *     <li>GET    /api/complaints                       — List theo role</li>
 *     <li>GET    /api/complaints/{id}                  — Detail (ownership-checked)</li>
 *     <li>POST   /api/complaints/{id}/respond          — Vendor respond (L0)</li>
 *     <li>POST   /api/complaints/{id}/escalate         — Customer/Vendor escalate L0→L1</li>
 *     <li>POST   /api/complaints/{id}/resolve          — Both-party resolve L0</li>
 *     <li>POST   /api/complaints/{id}/moderator-act    — Moderator claim/resolve/reject/escalate</li>
 *     <li>POST   /api/complaints/{id}/admin-act        — Admin Level 2</li>
 * </ul>
 *
 * <p>Lưu ý: Customer-facing HTML UI (page rendering) nằm ở
 * {@code controller.web.ComplaintController} xử lý các URL không có
 * prefix {@code /api} (vd: {@code /complaints}, {@code /complaints/new},
 * {@code /complaints/{id}}). Hai controller được tách theo convention
 * REST-vs-Web của project để tránh xung đột bean name + URL mapping.</p>
 *
 * <p>Security: route-level role enforcement bởi SecurityConfig +
 * method-level {@code @PreAuthorize} nếu cần. Ownership/state guards
 * trong service.</p>
 */
@Slf4j
@RestController("complaintApiController")
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintApiController {

    private ComplaintService complaintService;

    // =========== Customer (Level 0) ===========

    @PostMapping
    public ResponseEntity<Complaint> createComplaint(
            @Valid @RequestBody CreateComplaintReq req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(complaintService.createComplaint(req, user));
    }

    @GetMapping
    public ResponseEntity<Page<Complaint>> list(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails user) {
        String role = user.getRole();
        Page<Complaint> page = switch (role) {
            case "CUSTOMER" -> complaintService.listMyComplaints(user, pageable);
            case "VENDOR" -> complaintService.listShopComplaints(user, pageable);
            case "MODERATOR" -> complaintService.listModeratorQueue(user, pageable);
            case "ADMIN" -> complaintService.listAdminLevel2Queue(user, pageable);
            default -> Page.empty(pageable);
        };
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(complaintService.getById(id, user));
    }

    // =========== Vendor (Level 0) ===========

    @PostMapping("/{id}/respond")
    public ResponseEntity<Complaint> vendorRespond(
            @PathVariable String id,
            @Valid @RequestBody VendorRespondReq req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(complaintService.vendorRespond(id, req, user));
    }

    // =========== Both-party escalate/resolve ===========

    @PostMapping("/{id}/escalate")
    public ResponseEntity<Complaint> escalate(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(complaintService.escalateFromLevel0(id, reason, user));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Complaint> resolve(
            @PathVariable String id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String note = body != null ? body.get("note") : null;
        return ResponseEntity.ok(complaintService.resolveLevel0(id, note, user));
    }

    // =========== Moderator (Level 1) ===========

    @PostMapping("/{id}/moderator-act")
    public ResponseEntity<Complaint> moderatorAct(
            @PathVariable String id,
            @Valid @RequestBody ResolveComplaintReq req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(complaintService.moderatorAct(id, req, user));
    }

    // =========== Admin (Level 2) ===========

    @PostMapping("/{id}/admin-act")
    public ResponseEntity<Complaint> adminAct(
            @PathVariable String id,
            @Valid @RequestBody ResolveComplaintReq req,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(complaintService.adminAct(id, req, user));
    }
}
