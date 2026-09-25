package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.service.AdminReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Phase 15 — Admin Review Moderation Service Implementation.
 *
 * Đã implement:
 *   - listReviews(search, ratingFilter, pageable)
 *   - getReviewById(id)
 *   - deleteReview(id) - hard delete, không cascade
 *
 * Phase 15 LOCK tuân thủ:
 *   - LOCK 2: Delete Review ≠ Delete Product/Order/Customer
 *   - LOCK 3: Không tự tạo status enum
 *   - LOCK 4: Không đụng vào Product.rating / reviewCount
 *   - LOCK 5: Không tự sửa Customer Visibility
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {

    private final ReviewRepository reviewRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Review> listReviews(Pageable pageable, String q, Integer ratingFilter) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasRating = (ratingFilter != null);

        // CASE 1: no filter
        if (!hasQ && !hasRating) {
            Query qAll = new Query().with(pageable);
            long total = mongoTemplate.count(new Query(), Review.class);
            List<Review> content = mongoTemplate.find(qAll, Review.class);
            return new PageImpl<>(content, pageable, total);
        }

        // CASE 2: only search by comment
        if (hasQ && !hasRating) {
            String trimmed = q.trim();
            Pattern regex = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
            Query query = new Query(Criteria.where("comment").regex(regex)).with(pageable);
            long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Review.class);
            List<Review> content = mongoTemplate.find(query, Review.class);
            return new PageImpl<>(content, pageable, total);
        }

        // CASE 3: only rating filter
        if (!hasQ && hasRating) {
            Query query = new Query(Criteria.where("rating").is(ratingFilter)).with(pageable);
            long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Review.class);
            List<Review> content = mongoTemplate.find(query, Review.class);
            return new PageImpl<>(content, pageable, total);
        }

        // CASE 4: search + filter
        String trimmed = q.trim();
        Pattern regex = Pattern.compile(Pattern.quote(trimmed), Pattern.CASE_INSENSITIVE);
        Query query = new Query(
                Criteria.where("comment").regex(regex)
                        .and("rating").is(ratingFilter)
        ).with(pageable);
        long total = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), Review.class);
        List<Review> content = mongoTemplate.find(query, Review.class);
        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Review getReviewById(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BadRequestException("ID đánh giá không hợp lệ");
        }
        // Phase 6 + Phase 3 fix: Hỗ trợ CẢ 2 kiểu _id (String lẫn ObjectId).
        // KHÔNG ép raw.put("_id", id) vì sẽ làm save() insert document mới
        // khi DB lưu _id là ObjectId → duplicate key error (E11000).
        Document raw = mongoTemplate.getCollection("reviews")
                .find(new Document("_id", id))
                .first();
        if (raw == null && id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            org.bson.types.ObjectId oid = new org.bson.types.ObjectId(id);
            raw = mongoTemplate.getCollection("reviews")
                    .find(new Document("_id", oid))
                    .first();
        }
        if (raw == null) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá với ID: " + id);
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", Review.class.getName());
        }
        Review review = mongoTemplate.getConverter().read(Review.class, raw);
        if (review.getId() == null) {
            review.setId(id);
        }
        return review;
    }

    @Override
    @Transactional
    public void deleteReview(String id) {
        // LOCK 2: Delete Review ≠ Delete Product/Order/Customer.
        // LOCK 4: KHÔNG đụng Product.rating / reviewCount — chờ Contract.
        Review review = getReviewById(id);

        // Phase 3 critical fix: atomic delete dùng native _id (ObjectId hoặc String)
        // để tránh silent miss khi DB lưu _id là ObjectId.
        Object nativeId = resolveIdForQuery(id);
        long deleted = mongoTemplate.getCollection("reviews")
                .deleteOne(new Document("_id", nativeId))
                .getDeletedCount();
        if (deleted == 0) {
            log.warn("AdminReviewService.deleteReview: atomic delete not matched, falling back to repo");
            reviewRepository.deleteById(review.getId());
        }
        log.info("AdminReviewService.deleteReview: removed id={} productId={}",
                review.getId(), review.getProductId());
        // Side effects (Product.rating/reviewCount update, Customer visibility update)
        // bị BỎ QUA theo LOCK 4 và LOCK 5 — chờ Business Contract.
    }

    /**
     * Phase 3 critical fix: trả về _id dạng native (ObjectId hoặc String) để
     * Mongo query match đúng document trong DB.
     */
    private Object resolveIdForQuery(String id) {
        if (id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            Document raw = mongoTemplate.getCollection("reviews")
                    .find(new Document("_id", new org.bson.types.ObjectId(id)))
                    .first();
            if (raw != null) {
                return new org.bson.types.ObjectId(id);
            }
        }
        return id;
    }
}
