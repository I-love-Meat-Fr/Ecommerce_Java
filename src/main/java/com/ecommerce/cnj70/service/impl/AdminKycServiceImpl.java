package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminKycService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminKycServiceImpl implements AdminKycService {

    private final KycProfileRepository kycProfileRepository;
    private final UserRepository userRepository;
    /**
     * Phase 4 — dùng cho raw Document query để tránh {@code findById(String)}
     * không match với {@code _id} String (cùng pattern với AdminShop/AdminUser).
     */
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<KycProfile> listPending(Pageable pageable, String search) {
        return listByStatus(KycStatus.PENDING_ADMIN, search, pageable);
    }

    @Override
    public Page<KycProfile> listByStatus(KycStatus status, String search, Pageable pageable) {
        if (status == null) {
            return kycProfileRepository.findAll(pageable);
        }
        if (search != null && !search.isBlank()) {
            return kycProfileRepository
                    .findByStatusAndOwnerFullNameContainingIgnoreCase(status, search, pageable);
        }
        return kycProfileRepository.findByStatus(status, pageable);
    }

    @Override
    public Map<String, Object> getDetail(String profileId) {
        // Phase 5 fix — _id có thể là ObjectId lẫn String (giống pattern AdminShopServiceImpl.getShopById).
        // Bug history: raw query `new Document("_id", profileId)` chỉ match khi DB lưu _id là String.
        // Nếu _id là ObjectId, query thất bại trả null → "Không tìm thấy hồ sơ KYC".
        org.bson.Document raw = null;
        try {
            raw = mongoTemplate.getCollection("kyc_profiles")
                    .find(new org.bson.Document("_id", profileId))
                    .first();
        } catch (Exception e) {
            log.debug("KYC lookup String _id failed for [{}]: {}", profileId, e.getMessage());
        }
        if (raw == null && profileId != null && profileId.length() == 24 && profileId.matches("[0-9a-fA-F]+")) {
            try {
                raw = mongoTemplate.getCollection("kyc_profiles")
                        .find(new org.bson.Document("_id", new org.bson.types.ObjectId(profileId)))
                        .first();
            } catch (Exception e) {
                log.debug("KYC lookup ObjectId _id failed for [{}]: {}", profileId, e.getMessage());
            }
        }
        if (raw == null) {
            throw new ResourceNotFoundException("Không tìm thấy hồ sơ KYC");
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", KycProfile.class.getName());
        }
        raw.put("_id", profileId);
        KycProfile profile = mongoTemplate.getConverter().read(KycProfile.class, raw);
        if (profile.getId() == null) {
            profile.setId(profileId);
        }

        User user = getUserForProfile(profile);

        Map<String, Object> data = new HashMap<>();
        data.put("profile", profile);
        data.put("user", user);
        return data;
    }

    @Override
    public KycProfile approve(String profileId, UserDetails admin, String note) {
        KycProfile profile = kycProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ KYC"));

        // Chỉ approve được khi đã qua 3rd-party (PENDING_ADMIN) hoặc vendor resubmit sau reject
        if (profile.getStatus() != KycStatus.PENDING_ADMIN
                && profile.getStatus() != KycStatus.THIRD_PARTY_REJECTED
                && profile.getStatus() != KycStatus.PENDING_THIRD_PARTY) {
            throw new BadRequestException(
                    "Chỉ có thể duyệt hồ sơ ở trạng thái PENDING_ADMIN / PENDING_THIRD_PARTY / THIRD_PARTY_REJECTED. "
                  + "Hiện tại: " + profile.getStatus());
        }

        profile.setStatus(KycStatus.APPROVED);
        profile.setAdminReviewerId(getAdminId(admin));
        profile.setAdminReviewerName(admin != null ? admin.getUsername() : "admin");
        profile.setAdminNote(note);
        profile.setAdminReviewedAt(LocalDateTime.now());

        KycProfile saved = kycProfileRepository.save(profile);
        syncUserKycStatus(saved);

        log.info("Admin {} approved KYC profile {}", admin != null ? admin.getUsername() : "?", profileId);
        return saved;
    }

    @Override
    public KycProfile reject(String profileId, UserDetails admin, String note) {
        if (note == null || note.isBlank()) {
            throw new BadRequestException("Vui lòng nhập lý do từ chối");
        }

        KycProfile profile = kycProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ KYC"));

        profile.setStatus(KycStatus.ADMIN_REJECTED);
        profile.setAdminReviewerId(getAdminId(admin));
        profile.setAdminReviewerName(admin != null ? admin.getUsername() : "admin");
        profile.setAdminNote(note);
        profile.setAdminReviewedAt(LocalDateTime.now());

        KycProfile saved = kycProfileRepository.save(profile);
        syncUserKycStatus(saved);

        log.info("Admin {} rejected KYC profile {} - reason: {}",
                admin != null ? admin.getUsername() : "?", profileId, note);
        return saved;
    }

    @Override
    public KycProfile suspend(String profileId, UserDetails admin, String note) {
        KycProfile profile = kycProfileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ KYC"));

        if (profile.getStatus() != KycStatus.APPROVED) {
            throw new BadRequestException("Chỉ có thể đình chỉ hồ sơ đã được duyệt.");
        }

        profile.setStatus(KycStatus.SUSPENDED);
        profile.setAdminNote(note);
        profile.setAdminReviewedAt(LocalDateTime.now());

        KycProfile saved = kycProfileRepository.save(profile);
        syncUserKycStatus(saved);
        return saved;
    }

    @Override
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        for (KycStatus s : List.of(KycStatus.PENDING_THIRD_PARTY, KycStatus.PENDING_ADMIN,
                KycStatus.APPROVED, KycStatus.THIRD_PARTY_REJECTED, KycStatus.ADMIN_REJECTED)) {
            stats.put(s.name(), kycProfileRepository.countByStatus(s));
        }
        stats.put("TOTAL", kycProfileRepository.count());
        return stats;
    }

    @Override
    public User getUserForProfile(KycProfile profile) {
        if (profile.getUserId() == null) return null;
        // Phase 4 — raw Document fetch (cùng lý do với AdminShopController.shopDetail).
        org.bson.Document rawUser = mongoTemplate.getCollection("users")
                .find(new org.bson.Document("_id", profile.getUserId()))
                .first();
        if (rawUser == null) return null;
        if (!rawUser.containsKey("_class")) {
            rawUser.put("_class", User.class.getName());
        }
        rawUser.put("_id", profile.getUserId());
        User user = mongoTemplate.getConverter().read(User.class, rawUser);
        if (user.getId() == null) {
            user.setId(profile.getUserId());
        }
        return user;
    }

    private void syncUserKycStatus(KycProfile profile) {
        if (profile.getUserId() == null) return;
        userRepository.findById(profile.getUserId()).ifPresent(user -> {
            user.setKycStatus(profile.getStatus());
            userRepository.save(user);
        });
    }

    private String getAdminId(UserDetails admin) {
        if (admin instanceof CustomUserDetails cud) {
            return cud.getId();
        }
        return null;
    }
}
