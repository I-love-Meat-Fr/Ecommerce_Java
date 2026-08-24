package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends MongoRepository<User, String> {
    
    Optional<User> findByShopId(String shopId);
    
    boolean existsByShopId(String shopId);
}
