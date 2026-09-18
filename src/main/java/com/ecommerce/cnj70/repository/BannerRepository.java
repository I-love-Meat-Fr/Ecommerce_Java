package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.Banner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends MongoRepository<Banner, String> {

    Page<Banner> findByStatus(String status, Pageable pageable);

    List<Banner> findByStatusOrderBySortOrderAscCreatedAtAsc(String status);

    List<Banner> findByPositionOrderBySortOrderAscCreatedAtAsc(String position);

    List<Banner> findByStatusAndPositionOrderBySortOrderAscCreatedAtAsc(String status, String position);
}
