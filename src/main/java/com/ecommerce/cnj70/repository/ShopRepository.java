package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Shop;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends MongoRepository<Shop, String> {
    
    Optional<Shop> findByOwnerId(String ownerId);
    
    Optional<Shop> findByShopName(String shopName);
    
    boolean existsByShopName(String shopName);
    
    List<Shop> findByVerified(boolean verified);
    
    List<Shop> findByActive(boolean active);
}
