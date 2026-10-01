package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.VendorSubscription;
import com.ecommerce.cnj70.enums.SubscriptionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VendorSubscriptionRepository extends MongoRepository<VendorSubscription, String> {

    /**
     * Sub ACTIVE mới nhất của shop, dùng để cộng dồn endDate khi vendor mua tiếp.
     */
    Optional<VendorSubscription> findFirstByShopIdAndStatusOrderByEndDateDesc(
            String shopId, SubscriptionStatus status);

    /**
     * Sub ACTIVE đã quá hạn — scheduler quét hàng ngày để expire.
     */
    List<VendorSubscription> findByStatusAndEndDateBefore(
            SubscriptionStatus status, LocalDateTime cutoff);

    /**
     * Lịch sử subscription của shop (cho vendor xem log các lần mua / gia hạn).
     */
    List<VendorSubscription> findByShopIdOrderByCreatedAtDesc(String shopId);
}
