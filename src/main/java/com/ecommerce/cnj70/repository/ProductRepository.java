package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.ProductStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    
    List<Product> findByShopId(String shopId);
    
    List<Product> findByCategoryId(String categoryId);
    
    List<Product> findByStatus(ProductStatus status);
    
    List<Product> findByShopIdAndStatus(String shopId, ProductStatus status);
    
    List<Product> findByNameContainingIgnoreCaseAndStatus(String name, ProductStatus status);
    
    List<Product> findTop10ByStatusOrderByCreatedAtDesc(ProductStatus status);
}
