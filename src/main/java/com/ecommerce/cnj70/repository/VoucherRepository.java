package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends MongoRepository<Voucher, String> {

    // Tìm voucher theo code (dùng khi khách nhập mã)
    Optional<Voucher> findByCode(String code);

    // Tìm voucher theo shopId
    List<Voucher> findByShopId(String shopId);

    // Kiểm tra code đã tồn tại chưa
    boolean existsByCode(String code);
}
