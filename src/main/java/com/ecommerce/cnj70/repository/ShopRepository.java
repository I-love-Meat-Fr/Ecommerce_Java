package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends MongoRepository<Shop, String> {

    Optional<Shop> findByOwnerId(String ownerId);

    Optional<Shop> findByShopName(String shopName);

    boolean existsByShopName(String shopName);

    List<Shop> findByVerified(boolean verified);

    List<Shop> findByActive(boolean active);

    Page<Shop> findByVerified(boolean verified, Pageable pageable);

    Page<Shop> findByShopNameContainingIgnoreCase(String shopName, Pageable pageable);

    Page<Shop> findByVerifiedAndShopNameContainingIgnoreCase(boolean verified, String shopName, Pageable pageable);

    // === Phase 10: Shop Status Filter (Admin) ===
    Page<Shop> findByStatus(ShopStatus status, Pageable pageable);

    Page<Shop> findByStatusAndShopNameContainingIgnoreCase(
            ShopStatus status, String shopName, Pageable pageable);

    // === TASK #18 KYC Callback: find shop by provider referenceId ===
    Optional<Shop> findByKycReferenceId(String kycReferenceId);

    // ===== TASK #15/#18: KYC Queue for Moderator =====
    List<Shop> findByKycStatus(KycStatus kycStatus);

    Page<Shop> findByKycStatus(KycStatus kycStatus, Pageable pageable);

    Page<Shop> findByKycStatusIn(List<KycStatus> kycStatuses, Pageable pageable);
}
