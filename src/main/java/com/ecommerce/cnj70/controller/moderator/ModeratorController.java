package com.ecommerce.cnj70.controller.moderator;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.moderation.ModeratorKycRow;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.KycProfileRepository;
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

import java.util.ArrayList;
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

    // ===== DEBUG INSTRUMENTATION (debug-mode session 04f262) — DO NOT REMOVE BEFORE VERIFICATION =====
    private static final java.nio.file.Path DEBUG_LOG =
            java.nio.file.Paths.get("d:\\Ecommerce_Java\\.cursor\\debug-04f262.log");
    static void debugLog(String hypothesisId, String location, String message,
                                 java.util.Map<String, Object> data) {
        try {
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("sessionId", "04f262");
            payload.put("hypothesisId", hypothesisId);
            payload.put("runId", "kyc-approve-debug");
            payload.put("location", location);
            payload.put("message", message);
            payload.put("data", data);
            payload.put("timestamp", System.currentTimeMillis());
            String line = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(payload);
            java.nio.file.Files.writeString(DEBUG_LOG, line + "\n",
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            // ignore instrumentation errors
        }
    }
    // Public static accessor for cross-class instrumentation
    public static void debugLogStatic(String hypothesisId, String location, String message,
                                       java.util.Map<String, Object> data) {
        debugLog(hypothesisId, location, message, data);
    }
    // ===== END DEBUG INSTRUMENTATION =====

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ModeratorService moderatorService;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final KycProfileRepository kycProfileRepository;
    private final ReviewService reviewService;
    private final CryptoUtil cryptoUtil;
    private final AuditLogService auditLogService;
    private final MongoTemplate mongoTemplate;

    // TEMP DEBUG: check moderator2 user state
    @GetMapping("/_diag_user")
    @org.springframework.web.bind.annotation.ResponseBody
    public String diagUser() {
        var doc = mongoTemplate.getCollection("users")
                .find(new org.bson.Document("email", "moderator2@gmail.com")).first();
        if (doc == null) return "USER_NOT_FOUND";
        return "email=" + doc.getString("email")
                + " role=" + doc.getString("role")
                + " status=" + doc.getString("status")
                + " pwdLen=" + (doc.getString("password") == null ? 0 : doc.getString("password").length())
                + " pwdPrefix=" + (doc.getString("password") == null ? "null" : doc.getString("password").substring(0, Math.min(10, doc.getString("password").length())));
    }

    // ===== Dashboard — Gốc của tôi (stash feature/admin) =====
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Review> reportedReviews = reviewService.getReviewsByModerationStatus(ReviewModerationStatus.REPORTED);
        List<Review> hiddenReviews = reviewService.getReviewsByModerationStatus(ReviewModerationStatus.HIDDEN);

        // Đếm KYC từ KycProfile (bao gồm vendor mới chưa tạo Shop) + Shop (legacy mirror)
        List<com.ecommerce.cnj70.document.KycProfile> pendingKycProfiles = moderatorService.getKycProfilesByStatus(
                java.util.List.of(KycStatus.PENDING_THIRD_PARTY, KycStatus.PENDING_ADMIN));
        List<com.ecommerce.cnj70.document.KycProfile> rejectedKycProfiles = moderatorService.getKycProfilesByStatus(
                java.util.List.of(KycStatus.THIRD_PARTY_REJECTED, KycStatus.ADMIN_REJECTED));

        model.addAttribute("reportedCount", reportedReviews.size());
        model.addAttribute("hiddenCount", hiddenReviews.size());
        model.addAttribute("pendingKycCount", pendingKycProfiles.size());
        model.addAttribute("rejectedKycCount", rejectedKycProfiles.size());

        Page<Review> recentReported = moderatorService.getRecentReportedReviews(PageRequest.of(0, 5));
        model.addAttribute("recentReported", recentReported.getContent());

        return "moderator/dashboard";
    }

    // ===== KYC Queue =====
    /**
     * KYC queue dựa trên KycProfile (source of truth) — bao gồm vendor đã submit KYC
     * nhưng chưa tạo Shop. Trước đây query ShopRepository.findByKycStatus bỏ sót
     * các vendor mới này.
     */
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
                        org.springframework.data.domain.Sort.Direction.DESC, "lastResubmittedAt"));

        KycStatus targetStatus = parseKycStatus(status);

        Page<KycProfile> profiles = moderatorService.getKycProfilesByStatusPaged(targetStatus, pageable);

        List<ModeratorKycRow> rows = buildQueueRows(profiles.getContent());

        model.addAttribute("rows", rows);
        model.addAttribute("shops", rows); // legacy alias for any other template fragment
        model.addAttribute("page", profiles.getNumber());
        model.addAttribute("size", profiles.getSize());
        model.addAttribute("totalPages", profiles.getTotalPages());
        model.addAttribute("totalItems", profiles.getTotalElements());
        model.addAttribute("status", targetStatus != null ? targetStatus.name() : "");
        model.addAttribute("hasNext", profiles.hasNext());
        model.addAttribute("hasPrev", profiles.hasPrevious());

        return "moderator/kyc-queue";
    }

    /**
     * Build display rows từ KycProfile + User lookup.
     * Nếu vendor đã tạo Shop sẽ hiển thị tên shop; nếu chưa thì hiển thị tên doanh nghiệp
     * từ KYC hoặc tên user.
     */
    private List<ModeratorKycRow> buildQueueRows(List<KycProfile> profiles) {
        List<ModeratorKycRow> rows = new ArrayList<>(profiles.size());
        for (KycProfile p : profiles) {
            User vendor = p.getUserId() != null ? userRepository.findById(p.getUserId()).orElse(null) : null;
            boolean shopCreated = vendor != null && vendor.getShopId() != null;
            String shopName = null;
            if (shopCreated) {
                shopName = shopRepository.findById(vendor.getShopId())
                        .map(Shop::getShopName).orElse(null);
            }
            String displayShopName = shopName != null ? shopName
                    : (p.getBusinessName() != null && !p.getBusinessName().isBlank()
                        ? p.getBusinessName()
                        : (vendor != null ? vendor.getFullName() : null));
            String displayOwner = p.getOwnerFullName() != null && !p.getOwnerFullName().isBlank()
                    ? p.getOwnerFullName()
                    : (vendor != null ? vendor.getFullName() : null);

            rows.add(ModeratorKycRow.builder()
                    .profileId(p.getId())
                    .userId(p.getUserId())
                    .shopName(displayShopName != null ? displayShopName : "(chưa đặt tên)")
                    .ownerName(displayOwner)
                    .ownerEmail(vendor != null ? vendor.getEmail() : null)
                    .ownerPhone(vendor != null ? vendor.getPhone() : null)
                    .status(p.getStatus())
                    .submittedAt(p.getLastResubmittedAt() != null ? p.getLastResubmittedAt() : p.getSubmittedAt())
                    .rejectionReason(p.getThirdPartyRejectionReason() != null
                            ? p.getThirdPartyRejectionReason()
                            : p.getAdminNote())
                    .submitCount(p.getSubmitCount())
                    .shopCreated(shopCreated)
                    .build());
        }
        return rows;
    }

    @GetMapping("/kyc/{id}")
    public String kycDetail(@PathVariable String id,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            @RequestParam(required = false) String status,
                            Model model,
                            @AuthenticationPrincipal UserDetails userDetails) {
        // Thử load theo KycProfile trước (vendor mới submit KYC trước khi tạo Shop)
        KycProfile profile = kycProfileRepository.findById(id).orElse(null);

        User vendor = null;
        Shop shop = null;
        if (profile != null && profile.getUserId() != null) {
            vendor = userRepository.findById(profile.getUserId()).orElse(null);
            if (vendor != null && vendor.getShopId() != null) {
                shop = shopRepository.findById(vendor.getShopId()).orElse(null);
            }
        } else {
            // Fallback: id này có thể là Shop id (legacy flow)
            shop = loadShopWithFallback(id);
            if (shop != null && shop.getOwnerId() != null) {
                vendor = userRepository.findById(shop.getOwnerId()).orElse(null);
                if (vendor != null) {
                    profile = kycProfileRepository.findByUserId(vendor.getId()).orElse(null);
                }
            }
        }

        if (profile == null && shop == null) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ KYC / Shop với ID: " + id);
        }

        String decryptedCitizenId = null;
        String decryptedTaxCode = null;
        String decryptedBankAccount = null;
        String ownerEmail = vendor != null ? vendor.getEmail() : null;
        String ownerPhone = vendor != null ? vendor.getPhone() : null;

        // PII có thể nằm trên KycProfile (ưu tiên) hoặc Shop (legacy mirror)
        String encryptedCccd = profile != null ? profile.getIdNumber() : null; // KycProfile.idNumber là plaintext số CCCD
        String encryptedTax = profile != null ? profile.getTaxCode() : null;
        String encryptedBank = profile != null ? profile.getBankAccount() : null;
        if (shop != null) {
            // Shop lưu encrypted, override
            if (shop.getEncryptedCitizenId() != null) encryptedCccd = shop.getEncryptedCitizenId();
            if (shop.getEncryptedTaxCode() != null) encryptedTax = shop.getEncryptedTaxCode();
            if (shop.getEncryptedBankAccount() != null) encryptedBank = shop.getEncryptedBankAccount();
        }

        if (encryptedCccd != null && !encryptedCccd.isBlank()) {
            try {
                // CCCD có thể đã là plaintext trên KycProfile — chỉ decrypt nếu có "==" base64 padding
                if (encryptedCccd.contains("==") || encryptedCccd.contains("===")) {
                    decryptedCitizenId = cryptoUtil.decrypt(encryptedCccd);
                } else {
                    decryptedCitizenId = encryptedCccd;
                }
                auditLogService.logInfo(
                        AuditAction.PII_ACCESSED,
                        profile != null ? "KYC_PROFILE" : "SHOP",
                        profile != null ? profile.getId() : shop.getId(),
                        userDetails != null ? userDetails.getUsername() : "MODERATOR",
                        userDetails != null ? userDetails.getUsername() : "MODERATOR",
                        "MODERATOR",
                        "Moderator xem CCCD của " + (profile != null ? ("profile " + profile.getId()) : ("Shop: " + shop.getShopName()))
                );
            } catch (Exception e) {
                log.debug("Failed to decrypt CCCD for {}: {}", id, e.getMessage());
                decryptedCitizenId = encryptedCccd; // fallback show plaintext nếu không phải encrypted
            }
        }
        if (encryptedTax != null && !encryptedTax.isBlank()) {
            try {
                decryptedTaxCode = (encryptedTax.contains("==")) ? cryptoUtil.decrypt(encryptedTax) : encryptedTax;
            } catch (Exception e) {
                decryptedTaxCode = encryptedTax;
            }
        }
        if (encryptedBank != null && !encryptedBank.isBlank()) {
            try {
                decryptedBankAccount = (encryptedBank.contains("==")) ? cryptoUtil.decrypt(encryptedBank) : encryptedBank;
            } catch (Exception e) {
                decryptedBankAccount = encryptedBank;
            }
        }

        KycStatus effectiveStatus = profile != null && profile.getStatus() != null
                ? profile.getStatus()
                : (shop != null ? shop.getKycStatus() : null);
        java.time.LocalDateTime submittedAt = profile != null
                ? (profile.getLastResubmittedAt() != null ? profile.getLastResubmittedAt() : profile.getSubmittedAt())
                : (shop != null ? shop.getKycSubmittedAt() : null);
        String rejectionReason = profile != null
                ? (profile.getThirdPartyRejectionReason() != null
                        ? profile.getThirdPartyRejectionReason()
                        : profile.getAdminNote())
                : (shop != null ? shop.getKycRejectionReason() : null);
        String referenceId = profile != null ? profile.getThirdPartyReferenceId() : null;

        model.addAttribute("profile", profile);
        model.addAttribute("shop", shop); // có thể null nếu vendor chưa tạo shop
        model.addAttribute("vendor", vendor);
        model.addAttribute("kycStatus", effectiveStatus);
        model.addAttribute("kycSubmittedAt", submittedAt);
        model.addAttribute("kycApprovedAt", shop != null ? shop.getKycApprovedAt() : null);
        model.addAttribute("kycRejectionReason", rejectionReason);
        model.addAttribute("kycReferenceId", referenceId);
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
     * Moderator approves KYC.
     * <p>Hỗ trợ cả 2 loại id:</p>
     * <ul>
     *   <li>KycProfile id (vendor mới submit KYC, chưa có Shop)</li>
     *   <li>Shop id (legacy / vendor đã tạo Shop)</li>
     * </ul>
     */
    @PostMapping("/kyc/{id}/approve")
    public String kycApprove(@PathVariable String id,
                             @RequestParam(required = false) String note,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        // ===== DEBUG INSTRUMENTATION — debug-mode session 04f262 =====
        debugLog("H1", "ModeratorController.kycApprove.entry", "approve entry",
                java.util.Map.of(
                        "id", id,
                        "idLen", id == null ? 0 : id.length(),
                        "is24Hex", id != null && id.length() == 24 && id.matches("[0-9a-fA-F]+"),
                        "note", note == null ? "" : note,
                        "actor", userDetails != null ? userDetails.getUsername() : "<null-anonymous>"));
        try {
            String actor = userDetails != null ? userDetails.getUsername() : "MODERATOR";
            boolean profileExists = kycProfileRepository.existsById(id);
            // ===== DEBUG INSTRUMENTATION — branch decision =====
            debugLog("H1", "ModeratorController.kycApprove.branch", "existsById check",
                    java.util.Map.of("profileExists", profileExists, "id", id));
            if (profileExists) {
                moderatorService.approveKycProfile(id, actor, note);
            } else {
                moderatorService.approveKyc(id, actor, note);
            }
            auditLogService.logInfo(
                    com.ecommerce.cnj70.enums.AuditAction.KYC_APPROVED,
                    "KYC",
                    id,
                    actor,
                    actor,
                    "MODERATOR",
                    "Moderator duyệt KYC: " + id + (note != null ? " | note=" + note : "")
            );
            // ===== DEBUG INSTRUMENTATION — success =====
            debugLog("H1", "ModeratorController.kycApprove.success", "approve ok",
                    java.util.Map.of("id", id, "actor", actor));
            redirectAttributes.addFlashAttribute("success", "Đã duyệt hồ sơ KYC.");
        } catch (Exception e) {
            // ===== DEBUG INSTRUMENTATION — failure =====
            debugLog("H1", "ModeratorController.kycApprove.exception", "approve failed",
                    java.util.Map.of(
                            "id", id,
                            "exClass", e.getClass().getName(),
                            "exMsg", e.getMessage() == null ? "" : e.getMessage()));
            log.debug("Approve KYC failed for [{}]: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/moderator/kyc/" + id;
    }

    /**
     * Moderator rejects KYC.
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
            if (kycProfileRepository.existsById(id)) {
                moderatorService.rejectKycProfile(id, actor, note);
            } else {
                moderatorService.rejectKyc(id, actor, note);
            }
            auditLogService.logInfo(
                    com.ecommerce.cnj70.enums.AuditAction.KYC_REJECTED,
                    "KYC",
                    id,
                    actor,
                    actor,
                    "MODERATOR",
                    "Moderator từ chối KYC: " + id + " | note=" + note
            );
            redirectAttributes.addFlashAttribute("success", "Đã từ chối hồ sơ KYC.");
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
