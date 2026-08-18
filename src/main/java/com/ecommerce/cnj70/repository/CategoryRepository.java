package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    
    Optional<Category> findByName(String name);
    
    boolean existsByName(String name);
    
    List<Category> findByParentIdIsNull();
    
    List<Category> findByParentId(String parentId);
    
    List<Category> findByActiveTrue();
    
    List<Category> findByActiveTrueOrderBySortOrderAsc();
}
