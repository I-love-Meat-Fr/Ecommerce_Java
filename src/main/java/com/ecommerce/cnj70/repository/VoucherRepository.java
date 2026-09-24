package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.VoucherType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho Voucher
 */
@Repository
public interface VoucherRepository extends MongoRepository<Voucher, String> {

    // Tìm voucher theo code
    Optional<Voucher> findByCode(String code);

    // Kiểm tra code đã tồn tại chưa
    boolean existsByCode(String code);

    // Tìm voucher theo shopId (của vendor)
    List<Voucher> findByShopId(String shopId);

    // Tìm voucher theo loại
    List<Voucher> findByType(VoucherType type);

    // Tìm voucher của admin (WEB)
    List<Voucher> findByTypeAndActiveTrue(VoucherType type);

    // Tìm voucher đang active và còn hạn
    List<Voucher> findByActiveTrueAndEndDateAfter(LocalDateTime date);

    // Tìm voucher theo người tạo
    List<Voucher> findByCreatedBy(String createdBy);

    // Tìm voucher SHOP của một shop đang active
    List<Voucher> findByShopIdAndActiveTrue(String shopId);

    // === Phase 12: Phân trang cho Admin WEB Voucher Search+Filter ===
    Page<Voucher> findByType(VoucherType type, Pageable pageable);

    Page<Voucher> findByTypeAndActive(VoucherType type, boolean active, Pageable pageable);

    // === Phase 4B — Atomic usedCount operations (race-safe) ===

    /**
     * Atomic increment used when {@code used < quantity}.
     * Used by MongoTemplate.updateFirst() with conditional update.
     * <p>Note: this is a derived Mongo query — actual atomic update is performed
     * via {@code MongoTemplate.updateFirst()} in the service layer.</p>
     */
    long countByTypeAndActive(VoucherType type, boolean active);
}
