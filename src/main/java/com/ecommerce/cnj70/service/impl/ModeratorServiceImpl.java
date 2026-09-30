package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.ModeratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TASK #15 — Moderator Service Implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ModeratorServiceImpl implements ModeratorService {

    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;
    private final KycProfileRepository kycProfileRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Review> getReviewsByModerationStatusPaged(ReviewModerationStatus status, Pageable pageable) {
        if (status == null) {
            // Return all non-VISIBLE reviews (REPORTED + HIDDEN)
            return reviewRepository.findByModerationStatusIn(
                    List.of(ReviewModerationStatus.REPORTED, ReviewModerationStatus.HIDDEN),
                    pageable
            );
        }
        return reviewRepository.findByModerationStatus(status, pageable);
    }

    @Override
    public Page<Review> getRecentReportedReviews(Pageable pageable) {
        return reviewRepository.findByModerationStatus(ReviewModerationStatus.REPORTED, pageable);
    }

    @Override
    public List<Shop> getShopsByKycStatus(KycStatus status) {
        if (status == null) {
            return List.of();
        }
        return shopRepository.findByKycStatus(status);
    }

    @Override
    public Page<Shop> getShopsByKycStatusPaged(KycStatus status, Pageable pageable) {
        if (status == null) {
            // Return all shops with KYC issues (PENDING_THIRD_PARTY + PENDING_ADMIN + THIRD_PARTY_REJECTED)
            return shopRepository.findByKycStatusIn(
                    List.of(KycStatus.PENDING_THIRD_PARTY, KycStatus.PENDING_ADMIN,
                            KycStatus.THIRD_PARTY_REJECTED),
                    pageable
            );
        }
        return shopRepository.findByKycStatus(status, pageable);
    }

    @Override
    public Page<KycProfile> getKycProfilesByStatusPaged(KycStatus status, Pageable pageable) {
        if (status == null) {
            // All profiles cần moderator xử lý (PENDING_THIRD_PARTY + PENDING_ADMIN + THIRD_PARTY_REJECTED)
            return kycProfileRepository.findByStatusIn(
                    List.of(KycStatus.PENDING_THIRD_PARTY, KycStatus.PENDING_ADMIN,
                            KycStatus.THIRD_PARTY_REJECTED),
                    pageable
            );
        }
        return kycProfileRepository.findByStatus(status, pageable);
    }

    @Override
    public List<KycProfile> getKycProfilesByStatus(List<KycStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return List.of();
        }
        return kycProfileRepository.findByStatusIn(statuses);
    }

    @Override
    public KycProfile approveKycProfile(String profileId, String moderatorEmail, String note) {
        // ===== DEBUG INSTRUMENTATION — debug-mode session 04f262 =====
        com.ecommerce.cnj70.controller.moderator.ModeratorController
                .debugLogStatic("H2", "ModeratorServiceImpl.approveKycProfile.entry", "service entry",
                java.util.Map.of("profileId", profileId,
                        "moderator", moderatorEmail == null ? "" : moderatorEmail,
                        "note", note == null ? "" : note));
        KycProfile profile = kycProfileRepository.findById(profileId)
                .orElseThrow(() -> {
                    com.ecommerce.cnj70.controller.moderator.ModeratorController
                            .debugLogStatic("H2", "ModeratorServiceImpl.approveKycProfile.notFound",
                                    "profile not found",
                                    java.util.Map.of("profileId", profileId));
                    return new ResourceNotFoundException("Không tìm thấy hồ sơ KYC: " + profileId);
                });

        KycStatus previous = profile.getStatus();
        profile.setStatus(KycStatus.APPROVED);
        profile.setAdminReviewedAt(LocalDateTime.now());
        profile.setAdminNote(note);
        profile.setAdminReviewerName(moderatorEmail != null ? moderatorEmail : "MODERATOR");
        KycProfile saved = kycProfileRepository.save(profile);

        // ===== DEBUG INSTRUMENTATION — sync decision =====
        com.ecommerce.cnj70.controller.moderator.ModeratorController
                .debugLogStatic("H2", "ModeratorServiceImpl.approveKycProfile.beforeSync", "before sync",
                        java.util.Map.of("profileId", profileId,
                                "userId", profile.getUserId() == null ? "<null>" : profile.getUserId(),
                                "previousStatus", previous == null ? "null" : previous.name(),
                                "newStatus", "APPROVED"));

        // Sync denormalized fields
        try {
            syncAfterKycDecision(profile.getUserId(), KycStatus.APPROVED, null, note, moderatorEmail);
            com.ecommerce.cnj70.controller.moderator.ModeratorController
                    .debugLogStatic("H2", "ModeratorServiceImpl.approveKycProfile.syncOk", "sync ok",
                            java.util.Map.of("profileId", profileId));
        } catch (Exception ex) {
            // ===== DEBUG INSTRUMENTATION — sync failure =====
            com.ecommerce.cnj70.controller.moderator.ModeratorController
                    .debugLogStatic("H2", "ModeratorServiceImpl.approveKycProfile.syncFailed", "sync threw",
                            java.util.Map.of("profileId", profileId,
                                    "exClass", ex.getClass().getName(),
                                    "exMsg", ex.getMessage() == null ? "" : ex.getMessage()));
            throw ex;
        }

        log.info("[ModeratorKyc] KycProfile {} APPROVED by {} (was {}) note={}",
                profileId, moderatorEmail, previous, note);
        // ===== DEBUG INSTRUMENTATION — done =====
        com.ecommerce.cnj70.controller.moderator.ModeratorController
                .debugLogStatic("H2", "ModeratorServiceImpl.approveKycProfile.done", "service done",
                        java.util.Map.of("profileId", profileId, "savedStatus", saved.getStatus().name()));
        return saved;
    }

    @Override
    public KycProfile rejectKycProfile(String profileId, String moderatorEmail, String note) {
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        }
        KycProfile profile = kycProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ KYC: " + profileId));

        KycStatus previous = profile.getStatus();
        profile.setStatus(KycStatus.ADMIN_REJECTED);
        profile.setAdminReviewedAt(LocalDateTime.now());
        profile.setAdminNote(note);
        profile.setAdminReviewerName(moderatorEmail != null ? moderatorEmail : "MODERATOR");
        KycProfile saved = kycProfileRepository.save(profile);

        // Sync denormalized fields
        syncAfterKycDecision(profile.getUserId(), KycStatus.ADMIN_REJECTED, note, note, moderatorEmail);

        log.info("[ModeratorKyc] KycProfile {} REJECTED by {} (was {}) note={}",
                profileId, moderatorEmail, previous, note);
        return saved;
    }

    /**
     * Sync User.kycStatus + (nếu có) Shop.kycStatus + Shop.audit fields sau khi
     * moderator ra quyết định trên KycProfile. Đảm bảo mọi nơi đọc KYC status
     * đều nhất quán bất kể vendor đã tạo Shop hay chưa.
     */
    private void syncAfterKycDecision(String userId, KycStatus newStatus,
                                      String rejectionReason, String moderationNote,
                                      String moderatorEmail) {
        if (userId == null) return;

        // 1) User.kycStatus
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("[ModeratorKyc] Cannot sync User.kycStatus: user {} not found", userId);
            return;
        }
        user.setKycStatus(newStatus);
        userRepository.save(user);

        // 2) Shop.kycStatus (nếu vendor đã có shop)
        if (user.getShopId() != null) {
            shopRepository.findById(user.getShopId()).ifPresent(shop -> {
                shop.setKycStatus(newStatus);
                if (newStatus == KycStatus.APPROVED) {
                    shop.setKycApprovedAt(LocalDateTime.now());
                }
                if (rejectionReason != null) {
                    shop.setKycRejectionReason(rejectionReason);
                }
                if (moderationNote != null) {
                    shop.setActionBy(moderatorEmail != null ? moderatorEmail : "MODERATOR");
                    shop.setActionAt(LocalDateTime.now());
                }
                shopRepository.save(shop);
            });
        }
    }

    @Override
    public Shop approveKyc(String shopId, String moderatorEmail, String note) {
        Object nativeId = resolveNativeShopIdAsObject(shopId);
        org.bson.Document filter = new org.bson.Document("_id", nativeId);
        java.util.Date now = java.util.Date.from(java.time.LocalDateTime.now()
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
        org.bson.Document setDoc = new org.bson.Document()
                .append("kycStatus", KycStatus.APPROVED.name())
                .append("kycApprovedAt", now)
                .append("kycModerationActor", moderatorEmail != null ? moderatorEmail : "MODERATOR")
                .append("updatedAt", now);
        if (note != null && !note.isBlank()) {
            setDoc.append("kycRejectionReason", null);
            setDoc.append("kycModerationNote", note);
        }
        org.bson.Document update = new org.bson.Document("$set", setDoc);
        // Use the NATIVE _id type returned from resolveNativeShopIdAsObject, so updates hit
        // the same row regardless of whether _id was stored as String or ObjectId.
        com.mongodb.client.result.UpdateResult result = mongoTemplate.getCollection("shops")
                .updateOne(filter, update);
        if (result.getMatchedCount() == 0) {
            throw new ResourceNotFoundException("Không tìm thấy Shop với ID: " + shopId);
        }
        log.info("[ModeratorKyc] Shop {} KYC approved by {} note={} matched={}", shopId, moderatorEmail, note, result.getMatchedCount());
        return shopRepository.findById(shopId).orElse(null);
    }

    @Override
    public Shop rejectKyc(String shopId, String moderatorEmail, String note) {
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập lý do từ chối");
        }
        Object nativeId = resolveNativeShopIdAsObject(shopId);
        org.bson.Document filter = new org.bson.Document("_id", nativeId);
        java.util.Date now = java.util.Date.from(java.time.LocalDateTime.now()
                .atZone(java.time.ZoneId.systemDefault()).toInstant());
        org.bson.Document setDoc = new org.bson.Document()
                .append("kycStatus", KycStatus.ADMIN_REJECTED.name())
                .append("kycRejectionReason", note)
                .append("kycModerationActor", moderatorEmail != null ? moderatorEmail : "MODERATOR")
                .append("kycModerationNote", note)
                .append("updatedAt", now);
        org.bson.Document update = new org.bson.Document("$set", setDoc);
        com.mongodb.client.result.UpdateResult result = mongoTemplate.getCollection("shops")
                .updateOne(filter, update);
        if (result.getMatchedCount() == 0) {
            throw new ResourceNotFoundException("Không tìm thấy Shop với ID: " + shopId);
        }
        log.info("[ModeratorKyc] Shop {} KYC rejected by {} note={} matched={}", shopId, moderatorEmail, note, result.getMatchedCount());
        return shopRepository.findById(shopId).orElse(null);
    }

    /**
     * Resolve a Shop _id preserving the SAME native type that Mongo returns
     * (String or ObjectId). Returned Object is used directly as the _id value
     * in a raw filter Document, so updateOne matches the document exactly
     * without any driver-side coercion.
     */
    private Object resolveNativeShopIdAsObject(String shopId) {
        if (shopId == null || shopId.isBlank()) {
            throw new ResourceNotFoundException("Không tìm thấy Shop với ID rỗng");
        }
        // Try String _id first
        Document raw = mongoTemplate.getCollection("shops")
                .find(new Document("_id", shopId))
                .projection(new Document("_id", 1))
                .first();
        if (raw != null) {
            return raw.get("_id");
        }
        // Then ObjectId _id
        if (shopId.length() == 24 && shopId.matches("[0-9a-fA-F]+")) {
            raw = mongoTemplate.getCollection("shops")
                    .find(new Document("_id", new org.bson.types.ObjectId(shopId)))
                    .projection(new Document("_id", 1))
                    .first();
            if (raw != null) {
                return raw.get("_id");
            }
        }
        throw new ResourceNotFoundException("Không tìm thấy Shop với ID: " + shopId);
    }
}
