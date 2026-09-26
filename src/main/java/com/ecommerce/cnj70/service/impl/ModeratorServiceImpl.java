package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
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
