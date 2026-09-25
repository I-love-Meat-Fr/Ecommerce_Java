package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.ModeratorService;
import com.ecommerce.cnj70.service.ReviewService;
import com.ecommerce.cnj70.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
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
@Slf4j
public class ModeratorController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ModeratorService moderatorService;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final CryptoUtil cryptoUtil;
    private final AuditLogService auditLogService;
    private final MongoTemplate mongoTemplate;

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
        Shop shop = loadShopWithFallback(id);

        String decryptedCitizenId = null;
        String decryptedTaxCode = null;
        String decryptedBankAccount = null;
        String ownerEmail = null;
        String ownerPhone = null;

        // Look up owner (User) for contact info — used in the detail header
        if (shop.getOwnerId() != null) {
            try {
                User owner = userRepository.findById(shop.getOwnerId()).orElse(null);
                if (owner != null) {
                    ownerEmail = owner.getEmail();
                    ownerPhone = owner.getPhone();
                }
            } catch (Exception e) {
                log.debug("Failed to load owner user for shop [{}]: {}", shop.getId(), e.getMessage());
            }
        }

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
        model.addAttribute("ownerEmail", ownerEmail);
        model.addAttribute("ownerPhone", ownerPhone);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("status", status != null ? status : "");

        return "moderator/kyc-detail";
    }

    /**
     * Moderator approves KYC for a Shop.
     * Transitions PENDING_THIRD_PARTY / PENDING_ADMIN / THIRD_PARTY_REJECTED → APPROVED.
     */
    @PostMapping("/kyc/{id}/approve")
    public String kycApprove(@PathVariable String id,
                             @RequestParam(required = false) String note,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            String actor = userDetails != null ? userDetails.getUsername() : "MODERATOR";
            moderatorService.approveKyc(id, actor, note);
            auditLogService.logInfo(
                    com.ecommerce.cnj70.enums.AuditAction.KYC_APPROVED,
                    "SHOP",
                    id,
                    actor,
                    actor,
                    "MODERATOR",
                    "Moderator duyệt KYC Shop: " + id + (note != null ? " | note=" + note : "")
            );
            redirectAttributes.addFlashAttribute("success", "Đã duyệt KYC Shop.");
        } catch (Exception e) {
            log.debug("Approve KYC failed for [{}]: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/moderator/kyc/" + id;
    }

    /**
     * Moderator rejects KYC for a Shop.
     * Transitions to ADMIN_REJECTED with required note.
     */
    @PostMapping("/kyc/{id}/reject")
    public String kycReject(@PathVariable String id,
                            @RequestParam(required = false) String note,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            if (note == null || note.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng nhập lý do từ chối");
                return "redirect:/moderator/kyc/" + id;
            }
            String actor = userDetails != null ? userDetails.getUsername() : "MODERATOR";
            moderatorService.rejectKyc(id, actor, note);
            auditLogService.logInfo(
                    com.ecommerce.cnj70.enums.AuditAction.KYC_REJECTED,
                    "SHOP",
                    id,
                    actor,
                    actor,
                    "MODERATOR",
                    "Moderator từ chối KYC Shop: " + id + " | note=" + note
            );
            redirectAttributes.addFlashAttribute("success", "Đã từ chối KYC Shop.");
        } catch (Exception e) {
            log.debug("Reject KYC failed for [{}]: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/moderator/kyc/" + id;
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

    /**
     * Loads a Shop document by ID with String/ObjectId fallback.
     * Required because MongoDB stores some Shops with String _id and some with ObjectId,
     * and Spring Data's ShopRepository.findById fails to query String _id values.
     */
    private Shop loadShopWithFallback(String id) {
        if (id == null || id.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy Shop với ID rỗng");
        }

        // 1) Try native repository lookup first (covers common cases)
        try {
            java.util.Optional<Shop> fromRepo = shopRepository.findById(id);
            if (fromRepo != null && fromRepo.isPresent()) {
                return fromRepo.get();
            }
        } catch (Exception e) {
            log.debug("ShopRepository.findById failed for [{}]: {}", id, e.getMessage());
        }

        // 2) Direct Mongo collection query — covers String _id via raw getCollection
        try {
            org.bson.Document raw = mongoTemplate.getCollection("shops")
                    .find(new org.bson.Document("_id", id))
                    .first();
            if (raw != null) {
                Shop s = manualMapToShop(raw);
                if (s != null) {
                    return s;
                }
            }
        } catch (Exception e) {
            log.debug("MongoTemplate getCollection String _id fallback failed for [{}]: {}", id, e.getMessage());
        }

        // 3) ObjectId _id fallback
        if (id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            try {
                org.bson.Document raw = mongoTemplate.getCollection("shops")
                        .find(new org.bson.Document("_id", new org.bson.types.ObjectId(id)))
                        .first();
                if (raw != null) {
                    Shop s = manualMapToShop(raw);
                    if (s != null) {
                        return s;
                    }
                }
            } catch (Exception e) {
                log.debug("MongoTemplate getCollection ObjectId _id fallback failed for [{}]: {}", id, e.getMessage());
            }
        }

        throw new ResourceNotFoundException("Không tìm thấy Shop với ID: " + id);
    }

    /**
     * Manual mapping from a raw BSON Document to Shop, used when
     * MappingMongoConverter fails (e.g. _class mismatch).
     * Only fields required by kycDetail are populated.
     */
    private Shop manualMapToShop(org.bson.Document raw) {
        com.ecommerce.cnj70.document.Shop s = new com.ecommerce.cnj70.document.Shop();
        Object rawId = raw.get("_id");
        s.setId(rawId == null ? null : rawId.toString());
        Object ownerId = raw.get("ownerId");
        if (ownerId != null) s.setOwnerId(ownerId.toString());
        Object shopName = raw.get("shopName");
        if (shopName != null) s.setShopName(shopName.toString());
        Object description = raw.get("description");
        if (description != null) s.setDescription(description.toString());
        Object logoUrl = raw.get("logoUrl");
        if (logoUrl != null) s.setLogoUrl(logoUrl.toString());
        Object bannerUrl = raw.get("bannerUrl");
        if (bannerUrl != null) s.setBannerUrl(bannerUrl.toString());
        Object status = raw.get("status");
        if (status != null) {
            try { s.setStatus(com.ecommerce.cnj70.enums.ShopStatus.valueOf(status.toString())); } catch (Exception ignored) {}
        }
        Object kycStatus = raw.get("kycStatus");
        if (kycStatus != null) {
            try { s.setKycStatus(com.ecommerce.cnj70.enums.KycStatus.valueOf(kycStatus.toString())); } catch (Exception ignored) {}
        }
        Object eCit = raw.get("encryptedCitizenId");
        if (eCit != null) s.setEncryptedCitizenId(eCit.toString());
        Object eTax = raw.get("encryptedTaxCode");
        if (eTax != null) s.setEncryptedTaxCode(eTax.toString());
        Object eBank = raw.get("encryptedBankAccount");
        if (eBank != null) s.setEncryptedBankAccount(eBank.toString());
        return s;
    }
}
