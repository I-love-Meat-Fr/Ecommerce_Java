package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

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
}
