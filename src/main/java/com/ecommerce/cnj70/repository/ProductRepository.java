package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    List<Product> findByShopId(String shopId);

    List<Product> findByCategoryId(String categoryId);

    Page<Product> findByCategoryId(String categoryId, Pageable pageable);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByShopIdAndStatus(String shopId, ProductStatus status);

    Page<Product> findByShopIdAndStatus(String shopId, ProductStatus status, Pageable pageable);

    List<Product> findByNameContainingIgnoreCaseAndStatus(String name, ProductStatus status);

    Page<Product> findByNameContainingIgnoreCase(String name, Pageable pageable);

    List<Product> findTop10ByStatusOrderByCreatedAtDesc(ProductStatus status);

    List<Product> findByThumbnailUrlIsNull();

    // === Phase 2A — Moderation Queue ===
    Page<Product> findByModerationStatus(ModerationStatus status, Pageable pageable);

    Page<Product> findByModerationStatusIn(Collection<ModerationStatus> statuses, Pageable pageable);

    Page<Product> findByNameContainingIgnoreCaseAndModerationStatusIn(
            String name, Collection<ModerationStatus> statuses, Pageable pageable);

    // === Phase 4A — count for Admin Dashboard Pending Moderation ===

    long countByModerationStatus(ModerationStatus status);

    long countByModerationStatusIn(Collection<ModerationStatus> statuses);

    // ============================================================
    // PERFORMANCE #2 — Mongo-side sort + projection + limit
    // ----------------------------------------------------------
    // Trước đây `getActiveProducts()` gọi `findByStatus(ACTIVE)` rồi
    // sort trong RAM (toàn bộ collection load về JVM). Với hàng ngàn
    // SKU thì rất chậm. Các method bên dưới đẩy sort + limit + field
    // projection xuống MongoDB driver (sử dụng `_id: 0` để loại metadata).
    //
    // Fields projection chỉ lấy các trường cần cho home page:
    // id (luôn có), name, price, sold, rating, reviewCount,
    // thumbnailUrl, shopId, shopName, stock, createdAt.
    // Tránh pull về các trường nặng như imageUrls[], variants[],
    // specifications[], richDescription.
    // ============================================================

    /**
     * Top N active products sorted by rating DESC, chỉ lấy các field
     * cần thiết cho home page (dùng cho tab "Nổi bật").
     */
    @Query(
            value = "{ 'status': ?0 }",
            sort = "{ 'rating': -1 }",
            fields = "{ 'name': 1, 'price': 1, 'sold': 1, 'rating': 1, 'reviewCount': 1, 'thumbnailUrl': 1, 'shopId': 1, 'shopName': 1, 'stock': 1, 'createdAt': 1 }"
    )
    List<Product> findSummaryByStatusOrderByRatingDesc(ProductStatus status, Pageable pageable);

    /**
     * Top N active products sorted by createdAt DESC (mới về), field projection.
     */
    @Query(
            value = "{ 'status': ?0 }",
            sort = "{ 'createdAt': -1 }",
            fields = "{ 'name': 1, 'price': 1, 'sold': 1, 'rating': 1, 'reviewCount': 1, 'thumbnailUrl': 1, 'shopId': 1, 'shopName': 1, 'stock': 1, 'createdAt': 1 }"
    )
    List<Product> findSummaryByStatusOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    /**
     * Tất cả active products với field projection (dùng cho tab "Tất cả",
     * giới hạn tối đa bằng Pageable).
     */
    @Query(
            value = "{ 'status': ?0 }",
            sort = "{ 'createdAt': -1 }",
            fields = "{ 'name': 1, 'price': 1, 'sold': 1, 'rating': 1, 'reviewCount': 1, 'thumbnailUrl': 1, 'shopId': 1, 'shopName': 1, 'stock': 1, 'createdAt': 1 }"
    )
    List<Product> findSummaryByStatus(ProductStatus status, Pageable pageable);

    /**
     * Top N active products có sold > 0 (dùng cho Flash Sale strip).
     * Sort theo sold DESC để hiển thị hàng bán chạy nhất.
     */
    @Query(
            value = "{ 'status': ?0, 'sold': { '$gt': 0 } }",
            sort = "{ 'sold': -1 }",
            fields = "{ 'name': 1, 'price': 1, 'sold': 1, 'rating': 1, 'reviewCount': 1, 'thumbnailUrl': 1, 'shopId': 1, 'shopName': 1, 'stock': 1, 'createdAt': 1 }"
    )
    List<Product> findFlashSaleCandidates(ProductStatus status, Pageable pageable);
}
