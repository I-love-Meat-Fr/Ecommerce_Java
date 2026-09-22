package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.dto.request.KycCallbackRequest;
import com.ecommerce.cnj70.dto.request.KycSubmitRequest;
import com.ecommerce.cnj70.dto.response.KycStatusResponse;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.KycService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * TASK #16/#18 — KYC endpoints.
 *
 * - POST /api/kyc/submit: Vendor nộp KYC
 * - GET  /api/kyc/status: Vendor xem trạng thái KYC của mình
 * - POST /api/kyc/callback: Webhook từ KYC Provider
 *
 * Security:
 * - submit/status: cần authenticate, chỉ role VENDOR mới nộp KYC
 * - callback: PUBLIC endpoint (chỉ provider gọi), dùng HMAC signature để verify
 */
@Slf4j
@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycService kycService;
    private final AuditLogService auditLogService;
    private final ShopRepository shopRepository;

    /**
     * Vendor nộp KYC.
     * Body: { citizenId, taxCode?, bankAccount? }
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<KycStatusResponse> submitKyc(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody KycSubmitRequest request
    ) {
        // Tìm shopId từ user
        Shop shop = shopRepository.findByOwnerId(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("Bạn chưa có Shop. Vui lòng tạo Shop trước."));

        KycStatusResponse response = kycService.submitKyc(shop.getId(), request);

        // Audit log
        auditLogService.logInfo(
                AuditAction.KYC_SUBMITTED,
                "SHOP",
                shop.getId(),
                userDetails.getUsername(),
                userDetails.getUsername(),
                "VENDOR",
                "Vendor nộp KYC, referenceId=" + response.getKycReferenceId()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Vendor xem trạng thái KYC của mình.
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KycStatusResponse> getMyKycStatus(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(kycService.getVendorKycStatus(userDetails.getUsername()));
    }

    /**
     * Webhook callback từ KYC Provider.
     * PUBLIC endpoint - không cần authenticate (chỉ provider gọi).
     *
     * Trong production, cần verify HMAC signature ở đây.
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(
            @Valid @RequestBody KycCallbackRequest request
    ) {
        // TODO: Verify HMAC signature từ provider
        // if (!verifySignature(request)) throw new UnauthorizedException("Invalid signature");

        boolean approved = "APPROVED".equalsIgnoreCase(request.getDecision());
        String rejectionReason = request.getRejectionReason();

        // Gọi service để cập nhật Shop
        kycService.processKycCallback(request.getReferenceId(), approved, rejectionReason);

        // Audit log
        auditLogService.log(
                AuditAction.KYC_CALLBACK,
                "KYC",
                request.getReferenceId(),
                "SYSTEM",
                "KYC_PROVIDER",
                "SYSTEM",
                approved ? AuditSeverity.INFO : AuditSeverity.WARNING,
                "KYC callback: " + request.getDecision() +
                        (rejectionReason != null ? " - " + rejectionReason : ""),
                Map.of(
                        "decision", request.getDecision(),
                        "referenceId", request.getReferenceId()
                )
        );

        log.info("[KycController] Callback processed: refId={}, decision={}",
                request.getReferenceId(), request.getDecision());

        return ResponseEntity.ok(Map.of(
                "status", "processed",
                "referenceId", request.getReferenceId()
        ));
    }
}
