package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    
    List<Review> findByProductId(String productId);
    
    List<Review> findByUserId(String userId);
    
    Optional<Review> findByProductIdAndUserId(String productId, String userId);
    
    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);
    
    int countByProductId(String productId);
}
