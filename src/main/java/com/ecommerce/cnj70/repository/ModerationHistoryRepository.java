package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.ModerationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModerationHistoryRepository extends MongoRepository<ModerationHistory, String> {

    /**
     * All history entries for a given product, newest first.
     */
    Page<ModerationHistory> findByResourceIdOrderByCreatedAtDesc(String resourceId, Pageable pageable);

    /**
     * Full history list (newest first), for the Moderator history page.
     */
    Page<ModerationHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
