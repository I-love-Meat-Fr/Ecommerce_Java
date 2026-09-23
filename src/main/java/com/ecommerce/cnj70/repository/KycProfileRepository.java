package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KycProfileRepository extends MongoRepository<KycProfile, String> {

    Optional<KycProfile> findByUserId(String userId);

    boolean existsByUserId(String userId);

    /** Admin: danh sách hồ sơ theo trạng thái */
    Page<KycProfile> findByStatus(KycStatus status, Pageable pageable);

    Page<KycProfile> findByStatusAndOwnerFullNameContainingIgnoreCase(
            KycStatus status, String ownerFullName, Pageable pageable);

    /** Lấy tất cả profile đang cần admin xử lý */
    List<KycProfile> findByStatusIn(List<KycStatus> statuses);

    long countByStatus(KycStatus status);
}
