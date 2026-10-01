package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.PremiumPackage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PremiumPackageRepository extends MongoRepository<PremiumPackage, String> {

    /** Vendor view: chỉ các gói còn bán, sort theo giá tăng dần. */
    List<PremiumPackage> findByActiveTrueOrderByPriceAsc();

    /** Admin view: tất cả gói (kể cả inactive). */
    List<PremiumPackage> findAllByOrderByPriceAsc();

    /** Admin view: lọc theo active flag. */
    List<PremiumPackage> findByActiveOrderByPriceAsc(boolean active);
}
