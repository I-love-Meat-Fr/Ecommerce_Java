package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.ModeratorService;
import com.ecommerce.cnj70.service.ReviewService;
import com.ecommerce.cnj70.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * TASK #15 — Moderator Controller.
 *
 * Routes còn lại (không conflict với Phase 2B/2C controllers):
 *   GET  /moderator/kyc               - KYC queue
 *   GET  /moderator/kyc/{id}         - KYC detail với PII decryption
 *
 * Routes đã chuyển sang Phase 2B/2C controllers:
 *   GET  /moderator/dashboard         → ModeratorDashboardController
 *   GET  /moderator/reviews           → ModeratorReviewController
 *   GET  /moderator/reviews/{id}      → ModeratorReviewController
 *   POST /moderator/reviews/{id}/hide → ModeratorReviewController
 *   POST /moderator/reviews/{id}/restore → ModeratorReviewController (/unhide)
 *
 * Security: /moderator/** được bảo vệ bởi SecurityConfig.hasRole("MODERATOR").
 */
@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ModeratorService moderatorService;
    private final ShopRepository shopRepository;
    private final ReviewService reviewService;
    private final CryptoUtil cryptoUtil;
    private final AuditLogService auditLogService;

    // ===== Dashboard — Gốc của tôi (stash feature/admin) =====
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Review> reportedReviews = reviewService.getReviewsByModerationStatus(ReviewModerationStatus.REPORTED);
        List<Review> hiddenReviews = reviewService.getReviewsByModerationStatus(ReviewModerationStatus.HIDDEN);

        List<Shop> pendingKycShops = moderatorService.getShopsByKycStatus(KycStatus.PENDING_THIRD_PARTY);
        List<Shop> pendingAdminShops = moderatorService.getShopsByKycStatus(KycStatus.PENDING_ADMIN);
        List<Shop> rejectedKycShops = moderatorService.getShopsByKycStatus(KycStatus.THIRD_PARTY_REJECTED);

        model.addAttribute("reportedCount", reportedReviews.size());
        model.addAttribute("hiddenCount", hiddenReviews.size());
        model.addAttribute("pendingKycCount", pendingKycShops.size() + pendingAdminShops.size());
        model.addAttribute("rejectedKycCount", rejectedKycShops.size());

        Page<Review> recentReported = moderatorService.getRecentReportedReviews(PageRequest.of(0, 5));
        model.addAttribute("recentReported", recentReported.getContent());

        return "moderator/dashboard";
    }

    // ===== KYC Queue =====
    @GetMapping("/kyc")
    public String kycQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            Model model) {

        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "kycSubmittedAt"));

        KycStatus targetStatus = parseKycStatus(status);
        Page<Shop> shops = moderatorService.getShopsByKycStatusPaged(targetStatus, pageable);

        model.addAttribute("shops", shops.getContent());
        model.addAttribute("page", shops.getNumber());
        model.addAttribute("size", shops.getSize());
        model.addAttribute("totalPages", shops.getTotalPages());
        model.addAttribute("totalItems", shops.getTotalElements());
        model.addAttribute("status", targetStatus != null ? targetStatus.name() : "");
        model.addAttribute("hasNext", shops.hasNext());
        model.addAttribute("hasPrev", shops.hasPrevious());

        return "moderator/kyc-queue";
    }

    @GetMapping("/kyc/{id}")
    public String kycDetail(@PathVariable String id,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String status,
                            Model model,
                            @AuthenticationPrincipal UserDetails userDetails) {
        Shop shop = shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Shop"));

        String decryptedCitizenId = null;
        String decryptedTaxCode = null;
        String decryptedBankAccount = null;

        if (shop.getEncryptedCitizenId() != null) {
            decryptedCitizenId = cryptoUtil.decrypt(shop.getEncryptedCitizenId());
            auditLogService.logInfo(
                    AuditAction.PII_ACCESSED,
                    "SHOP",
                    shop.getId(),
                    userDetails != null ? userDetails.getUsername() : "MODERATOR",
                    userDetails != null ? userDetails.getUsername() : "MODERATOR",
                    "MODERATOR",
                    "Moderator xem CCCD của Shop: " + shop.getShopName()
            );
        }

        model.addAttribute("shop", shop);
        model.addAttribute("decryptedCitizenId", decryptedCitizenId);
        model.addAttribute("decryptedTaxCode", decryptedTaxCode);
        model.addAttribute("decryptedBankAccount", decryptedBankAccount);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("status", status != null ? status : "");

        return "moderator/kyc-detail";
    }

    private KycStatus parseKycStatus(String status) {
        if (status == null || status.isBlank()) {
            return KycStatus.PENDING_THIRD_PARTY;
        }
        try {
            return KycStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KycStatus.PENDING_THIRD_PARTY;
        }
    }
}
