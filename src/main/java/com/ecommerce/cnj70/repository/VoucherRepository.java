package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.VoucherType;
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
}
