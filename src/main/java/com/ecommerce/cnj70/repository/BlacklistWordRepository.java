package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.BlacklistWord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistWordRepository extends MongoRepository<BlacklistWord, String> {

    Optional<BlacklistWord> findByKeywordIgnoreCase(String keyword);

    List<BlacklistWord> findByEnabledTrue();

    boolean existsByKeywordIgnoreCase(String keyword);
}
