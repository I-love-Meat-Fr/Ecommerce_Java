package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.ProductPriceHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductPriceHistoryRepository
        extends MongoRepository<ProductPriceHistory, String> {

    /**
     * Lấy lịch sử giá của product, sắp xếp mới nhất trước.
     *
     * @param productId id product
     * @param pageable  pageable (LIMIT qua {@code PageRequest.of(0, N)})
     * @return danh sách theo thứ tự recordedAt DESC
     */
    List<ProductPriceHistory> findByProductIdOrderByRecordedAtDesc(String productId, Pageable pageable);

    /**
     * Lấy lịch sử giá của product trong khoảng thời gian (30 ngày gần nhất).
     */
    List<ProductPriceHistory> findByProductIdAndRecordedAtGreaterThanEqual(
            String productId, LocalDateTime since, Pageable pageable);
}
