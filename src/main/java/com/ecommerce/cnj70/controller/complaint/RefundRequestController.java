package com.ecommerce.cnj70.controller.complaint;

import com.ecommerce.cnj70.document.RefundRequest;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.RefundService;
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

import java.math.BigDecimal;
import java.util.Map;

/**
 * Phase 3A §28 — Refund Request REST controller.
 *
 * <p>Customer/Vendor request Refund qua Complaint/Return + ownership-checked
 * list/get + poll status (cho Phase 3C Scheduler).</p>
 *
 * <h3>Security</h3>
 * <ul>
 *     <li>Route-level: {@code /api/**} is {@code authenticated()} — Phase 4.</li>
 *     <li>Method-level: ownership/scope enforced in {@link RefundService}.</li>
 *     <li>Không fake refund; delegate sang RefundProvider (log-only by default).</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundRequestController {

    private final RefundService refundService;

    @PostMapping
    public ResponseEntity<RefundRequest> requestRefund(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        String complaintId = asString(body.get("complaintId"));
        String returnId = asString(body.get("returnId"));
        String orderId = asString(body.get("orderId"));
        Object declaredAmount = body.get("declaredAmount");
        BigDecimal amount = null;
        if (declaredAmount != null) {
            try {
                amount = new BigDecimal(declaredAmount.toString());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("declaredAmount không hợp lệ");
            }
        }
        RefundRequest saved = refundService.requestRefund(complaintId, returnId, orderId, amount, user);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<Page<RefundRequest>> list(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails user) {
        String role = user.getRole();
        Page<RefundRequest> page = switch (role) {
            case "CUSTOMER" -> refundService.listMyRefunds(user, pageable);
            case "VENDOR" -> refundService.listShopRefunds(user, pageable);
            default -> Page.empty(pageable);
        };
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RefundRequest> getById(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(refundService.getById(id, user));
    }

    /** Phase 3C — hook cho Scheduler poll provider. Phase 3A để sẵn endpoint. */
    @PostMapping("/{id}/poll")
    public ResponseEntity<RefundRequest> pollStatus(
            @PathVariable String id,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(refundService.pollStatus(id, user));
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }
}
