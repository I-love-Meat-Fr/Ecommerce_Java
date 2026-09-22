package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.ReviewService;
import com.ecommerce.cnj70.service.ModeratorService;
import com.ecommerce.cnj70.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * TASK #15 — Moderator Controller.
 *
 * Routes cho Moderator kiểm duyệt:
 *   GET  /moderator/dashboard          - Dashboard với thống kê
 *   GET  /moderator/reviews            - Review queue (REPORTED, HIDDEN)
 *   GET  /moderator/reviews/{id}       - Review detail
 *   POST /moderator/reviews/{id}/hide  - Ẩn review
 *   POST /moderator/reviews/{id}/restore - Khôi phục review
 *   GET  /moderator/kyc               - KYC queue (PENDING_PROVIDER, KYC_REJECTED)
 *
 * Security: /moderator/** được bảo vệ bởi SecurityConfig.hasRole("MODERATOR").
 */
@Controller
@RequestMapping("/moderator")
@RequiredArgsConstructor
public class ModeratorController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ReviewService reviewService;
    private final ModeratorService moderatorService;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final CryptoUtil cryptoUtil;
    private final AuditLogService auditLogService;

    // ===== Dashboard =====
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Review stats
        List<Review> reportedReviews = reviewService.getReviewsByModerationStatus(ReviewModerationStatus.REPORTED);
        List<Review> hiddenReviews = reviewService.getReviewsByModerationStatus(ReviewModerationStatus.HIDDEN);
        
        // KYC stats
        List<Shop> pendingKycShops = moderatorService.getShopsByKycStatus(KycStatus.PENDING_PROVIDER);
        List<Shop> rejectedKycShops = moderatorService.getShopsByKycStatus(KycStatus.KYC_REJECTED);
        
        model.addAttribute("reportedCount", reportedReviews.size());
        model.addAttribute("hiddenCount", hiddenReviews.size());
        model.addAttribute("pendingKycCount", pendingKycShops.size());
        model.addAttribute("rejectedKycCount", rejectedKycShops.size());
        
        // Recent reported reviews
        Page<Review> recentReported = moderatorService.getRecentReportedReviews(PageRequest.of(0, 5));
        model.addAttribute("recentReported", recentReported.getContent());
        
        return "moderator/dashboard";
    }

    // ===== Review Queue =====
    @GetMapping("/reviews")
    public String reviewQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q,
            Model model) {
        
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Default to REPORTED if no status specified
        ReviewModerationStatus targetStatus = parseStatus(status);
        
        Page<Review> reviews = moderatorService.getReviewsByModerationStatusPaged(targetStatus, pageable);
        
        model.addAttribute("reviews", reviews.getContent());
        model.addAttribute("page", reviews.getNumber());
        model.addAttribute("size", reviews.getSize());
        model.addAttribute("totalPages", reviews.getTotalPages());
        model.addAttribute("totalItems", reviews.getTotalElements());
        model.addAttribute("status", targetStatus != null ? targetStatus.name() : "");
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("hasNext", reviews.hasNext());
        model.addAttribute("hasPrev", reviews.hasPrevious());
        
        return "moderator/review-queue";
    }

    @GetMapping("/reviews/{id}")
    public String reviewDetail(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String q,
                               Model model) {
        Review review = reviewService.getReviewById(id);
        
        Product product = null;
        if (review.getProductId() != null && !review.getProductId().isBlank()) {
            try {
                product = productRepository.findById(review.getProductId()).orElse(null);
            } catch (Exception ex) {
                product = null;
            }
        }
        
        model.addAttribute("review", review);
        model.addAttribute("product", product);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("status", status != null ? status : "");
        model.addAttribute("q", q != null ? q : "");
        
        return "moderator/review-detail";
    }

    @PostMapping("/reviews/{id}/hide")
    public String hideReview(@PathVariable String id,
                           @RequestParam(required = false) String reason,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String status,
                           @RequestParam(required = false) String q,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            reviewService.hideReview(id,
                    userDetails != null ? userDetails.getUsername() : "MODERATOR",
                    reason != null ? reason : "Vi phạm nội quy đánh giá");
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã ẩn đánh giá (ID: " + id + ")");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, status, q, "/moderator/reviews");
    }

    @PostMapping("/reviews/{id}/restore")
    public String restoreReview(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String q,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            reviewService.restoreReview(id,
                    userDetails != null ? userDetails.getUsername() : "MODERATOR");
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã khôi phục đánh giá (ID: " + id + ")");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, status, q, "/moderator/reviews");
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
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "kycSubmittedAt"));
        
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
        
        // Decrypt PII for display (will be logged via AuditLog)
        String decryptedCitizenId = null;
        String decryptedTaxCode = null;
        String decryptedBankAccount = null;
        
        if (shop.getEncryptedCitizenId() != null) {
            decryptedCitizenId = cryptoUtil.decrypt(shop.getEncryptedCitizenId());
            // AuditLog: moderator viewed PII
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

    // ===== Helper methods =====
    private ReviewModerationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return ReviewModerationStatus.REPORTED; // Default
        }
        try {
            return ReviewModerationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ReviewModerationStatus.REPORTED;
        }
    }

    private KycStatus parseKycStatus(String status) {
        if (status == null || status.isBlank()) {
            return KycStatus.PENDING_PROVIDER; // Default
        }
        try {
            return KycStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return KycStatus.PENDING_PROVIDER;
        }
    }

    private String buildRedirectUrl(int page, int size, String status, String q, String baseUrl) {
        StringBuilder url = new StringBuilder(baseUrl)
                .append("?page=").append(page)
                .append("&size=").append(size);
        if (status != null && !status.isBlank()) {
            url.append("&status=").append(status);
        }
        if (q != null && !q.isBlank()) {
            url.append("&q=").append(q);
        }
        return "redirect:" + url;
    }
}
