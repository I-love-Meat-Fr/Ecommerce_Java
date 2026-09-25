package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.enums.ReviewModerationStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 6 — Unit test cho AdminReviewServiceImpl.
 *
 * Verify rule:
 *  - getReviewById: raw mongoTemplate query; not found → throw.
 *  - getReviewById(blank) → BadRequestException.
 *  - deleteReview: hard delete (calls repository.deleteById).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminReviewService - Phase 6 unit test")
class AdminReviewServiceImplTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;
    @Mock private MongoConverter mongoConverter;

    @InjectMocks private AdminReviewServiceImpl adminReviewService;

    @BeforeEach
    void setUp() {
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Document.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("reviews")).thenReturn(mongoCollection);
        lenient().when(mongoTemplate.getConverter()).thenReturn(mongoConverter);
    }

    private Review validReview() {
        return Review.builder()
                .id("r-1")
                .productId("p-1")
                .userId("u-1")
                .userName("Bob")
                .rating(5)
                .comment("Great product")
                .moderationStatus(ReviewModerationStatus.VISIBLE)
                .build();
    }

    private void stubRawGetReviewById(Review review) {
        Document raw = new Document("_id", review.getId())
                .append("productId", review.getProductId())
                .append("userId", review.getUserId())
                .append("userName", review.getUserName())
                .append("rating", review.getRating())
                .append("comment", review.getComment())
                .append("moderationStatus",
                        review.getModerationStatus() != null
                                ? review.getModerationStatus().name() : null);
        FindIterable<Document> findIterable = mock(FindIterable.class);
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoCollection.find(any(Document.class))).thenReturn(findIterable);
        lenient().when(mongoConverter.read(eq(Review.class), eq(raw))).thenReturn(review);
    }

    @Test
    void getReviewById_existing_returnsReview() {
        Review review = validReview();
        stubRawGetReviewById(review);

        Review result = adminReviewService.getReviewById("r-1");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("r-1");
        assertThat(result.getUserName()).isEqualTo("Bob");
        assertThat(result.getRating()).isEqualTo(5);
    }

    @Test
    void getReviewById_notFound_throws() {
        // Default stub returns null.
        assertThatThrownBy(() -> adminReviewService.getReviewById("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getReviewById_blank_throwsBadRequest() {
        assertThatThrownBy(() -> adminReviewService.getReviewById(""))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> adminReviewService.getReviewById(null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteReview_existing_callsRepository() {
        Review review = validReview();
        stubRawGetReviewById(review);

        adminReviewService.deleteReview("r-1");

        verify(reviewRepository).deleteById("r-1");
    }
}
