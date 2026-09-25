package com.ecommerce.cnj70.controller.complaint;

import com.ecommerce.cnj70.document.ReturnRequest;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ReturnService;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.Map;

/**
 * Phase 3A §23 — Return Request REST controller.
 *
 * <p>Customer request Return + ownership-checked list/get. Vendor approve/reject
 * + Refund trigger thuộc Phase 3B (workflow, không phải contract).</p>
 *
 * <h3>Security</h3>
 * <ul>
 *     <li>Route-level: {@code /api/**} is {@code authenticated()} — Phase 4.</li>
 *     <li>Method-level: ownership/scope enforced in {@link ReturnService}.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnRequestController {

    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnRequest> requestReturn(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String orderId = asString(body.get("orderId"));
        @SuppressWarnings("unchecked")
        List<String> orderItemIds = (List<String>) body.get("orderItemIds");
        String reason = asString(body.get("reason"));
        @SuppressWarnings("unchecked")
        List<String> evidence = (List<String>) body.get("evidence");
        ReturnRequest saved = returnService.requestReturn(orderId, orderItemIds, reason, evidence, user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<Page<ReturnRequest>> list(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails user) {
        String role = user.getRole();
        Page<ReturnRequest> page = switch (role) {
            case "CUSTOMER" -> returnService.listMyReturns(user, pageable);
            case "VENDOR" -> returnService.listShopReturns(user, pageable);
            default -> Page.empty(pageable); // Moderator/Admin: chưa expose queue ở Phase 3A.
        };
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReturnRequest> getById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(returnService.getById(id, user));
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
