package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.ModeratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TASK #15 — Moderator Service Implementation.
 */
@Service
@RequiredArgsConstructor
public class ModeratorServiceImpl implements ModeratorService {

    private final ReviewRepository reviewRepository;
    private final ShopRepository shopRepository;

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
            // Return all shops with KYC issues (PENDING_PROVIDER + KYC_REJECTED)
            return shopRepository.findByKycStatusIn(
                    List.of(KycStatus.PENDING_PROVIDER, KycStatus.KYC_REJECTED),
                    pageable
            );
        }
        return shopRepository.findByKycStatus(status, pageable);
    }
}
