package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    /**
     * TASK #14/#20/#21 — ReviewServiceImpl giờ phụ thuộc OrderService để xác thực đã nhận hàng.
     * TASK #15/#24 — Phụ thuộc AuditLogService để ghi log moderation.
     */
    @Mock
    private OrderService orderService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User testUser;
    private Product testProduct;
    private Review testReview;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user-001")
                .fullName("Nguyễn Văn A")
                .avatarUrl("https://cdn.example.com/avatars/user-001.jpg")
                .email("test@example.com")
                .build();

        testProduct = Product.builder()
                .id("prod-001")
                .name("Test Product")
                .price(new BigDecimal("100000"))
                .rating(0.0)
                .reviewCount(0)
                .build();

        testReview = Review.builder()
                .id("review-001")
                .productId("prod-001")
                .userId("user-001")
                .userName("Nguyễn Văn A")
                .userAvatar("https://cdn.example.com/avatars/user-001.jpg")
                .rating(5)
                .comment("Sản phẩm rất tốt!")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // =========================================================================
    // TC01 — Create Review: Authenticated user creates valid review → Success
    // =========================================================================
    @Test
    @DisplayName("TC01: Authenticated user creates valid review → Success")
    void createReview_validInput_savesAndReturnsReview() {
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        // TASK #14/#20/#21: check hasUserReceivedProduct (Order DELIVERED)
        when(orderService.hasUserReceivedProduct("user-001", "prod-001")).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001")).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-new");
            return r;
        });
        when(reviewRepository.findByProductId("prod-001")).thenReturn(List.of(testReview));
        when(reviewRepository.countByProductId("prod-001")).thenReturn(1);

        Review result = reviewService.createReview("user-001", "prod-001", 5, "Sản phẩm rất tốt!");

        assertNotNull(result);
        assertEquals("review-new", result.getId());
        assertEquals("prod-001", result.getProductId());
        assertEquals("user-001", result.getUserId());
        assertEquals("Nguyễn Văn A", result.getUserName());
        assertEquals(5, result.getRating());
        assertEquals("Sản phẩm rất tốt!", result.getComment());

        // Verify TASK #14/#20/#21 purchase verification was invoked
        verify(orderService).hasUserReceivedProduct("user-001", "prod-001");

        // Verify snapshots were taken from User at creation time
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertEquals("Nguyễn Văn A", saved.getUserName());
        assertEquals("https://cdn.example.com/avatars/user-001.jpg", saved.getUserAvatar());
    }

    // =========================================================================
    // TC03 — Create Review: Product does not exist → Error
    // =========================================================================
    @Test
    @DisplayName("TC03: Product does not exist → ResourceNotFoundException")
    void createReview_productNotFound_throwsException() {
        // Note: actual order in code is user check first, then product check.
        // So we must provide a valid user for the product-not-found error to be reached.
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(productRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.createReview("user-001", "nonexistent", 5, "Good!")
        );

        assertEquals("Không tìm thấy sản phẩm", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC — Create Review: User does not exist → Error
    // =========================================================================
    @Test
    @DisplayName("Create review: User does not exist → ResourceNotFoundException")
    void createReview_userNotFound_throwsException() {
        // No repository stubs needed — user check happens first.
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.createReview("nonexistent", "prod-001", 5, "Good!")
        );

        assertEquals("Không tìm thấy người dùng", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC04 — Create Review: Rating = 0 → Validation error
    // =========================================================================
    @Test
    @DisplayName("TC04: Rating = 0 → BadRequestException")
    void createReview_ratingZero_throwsException() {
        // No repository stubs needed — rating validation runs before any DB lookup.
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.createReview("user-001", "prod-001", 0, "Too low rating")
        );

        assertEquals("Rating phải từ 1 đến 5 sao", ex.getMessage());
        verify(reviewRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
        verify(productRepository, never()).findById(any());
    }

    // =========================================================================
    // TC05 — Create Review: Rating = 6 → Validation error
    // =========================================================================
    @Test
    @DisplayName("TC05: Rating = 6 → BadRequestException")
    void createReview_ratingSix_throwsException() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.createReview("user-001", "prod-001", 6, "Too high rating")
        );

        assertEquals("Rating phải từ 1 đến 5 sao", ex.getMessage());
        verify(reviewRepository, never()).save(any());
        verify(userRepository, never()).findById(any());
        verify(productRepository, never()).findById(any());
    }

    // =========================================================================
    // TC06 — Create Review: Rating = -1 → Validation error
    // =========================================================================
    @Test
    @DisplayName("TC06: Rating = -1 → BadRequestException")
    void createReview_ratingNegative_throwsException() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.createReview("user-001", "prod-001", -1, "Negative rating")
        );

        assertEquals("Rating phải từ 1 đến 5 sao", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC07 — Create Review: Empty comment → Validation error
    // =========================================================================
    @Test
    @DisplayName("TC07: Empty comment → BadRequestException")
    void createReview_emptyComment_throwsException() {
        // No repository stubs needed — comment validation runs before any DB lookup.
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.createReview("user-001", "prod-001", 5, "")
        );

        assertEquals("Nội dung đánh giá không được để trống", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC08 — Create Review: Whitespace-only comment → Validation error
    // =========================================================================
    @Test
    @DisplayName("TC08: Whitespace-only comment → BadRequestException")
    void createReview_whitespaceComment_throwsException() {
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.createReview("user-001", "prod-001", 5, "     ")
        );

        assertEquals("Nội dung đánh giá không được để trống", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC09 — Create Review: Duplicate review → Error
    // =========================================================================
    @Test
    @DisplayName("TC09: Duplicate review → BadRequestException")
    void createReview_duplicateReview_throwsException() {
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        // TASK #14: check hasUserReceivedProduct (Order DELIVERED)
        when(orderService.hasUserReceivedProduct("user-001", "prod-001")).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001"))
                .thenReturn(Optional.of(testReview));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.createReview("user-001", "prod-001", 4, "Another review")
        );

        assertEquals("Bạn đã đánh giá sản phẩm này rồi", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC25 — TASK #14/#20/#21: User CHƯA nhận Product (DELIVERED) → Review bị từ chối
    // =========================================================================
    @Test
    @DisplayName("TC25: User has NOT received product (DELIVERED) → BadRequestException")
    void createReview_userHasNotReceived_throwsException() {
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        // TASK #14: hasUserReceivedProduct trả false vì Order chưa DELIVERED
        when(orderService.hasUserReceivedProduct("user-001", "prod-001")).thenReturn(false);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.createReview("user-001", "prod-001", 5, "Trying to review")
        );

        assertTrue(ex.getMessage().contains("nhận"), "Message should mention receiving requirement");
        verify(orderService).hasUserReceivedProduct("user-001", "prod-001");
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC10 — Read Reviews: Get reviews for Product A → Only Product A reviews
    // =========================================================================
    @Test
    @DisplayName("TC10: Get reviews for product → only that product's reviews returned")
    void getReviewsByProductId_returnsOnlyMatchingProduct() {
        Review otherProductReview = Review.builder()
                .id("review-other")
                .productId("prod-002")
                .userId("user-002")
                .rating(3)
                .comment("OK product")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc("prod-001"))
                .thenReturn(List.of(testReview));

        List<Review> result = reviewService.getReviewsByProductId("prod-001");

        assertEquals(1, result.size());
        assertEquals("prod-001", result.get(0).getProductId());
        verify(reviewRepository, never()).findByProductIdOrderByCreatedAtDesc("prod-002");
    }

    // =========================================================================
    // TC11 — Read Reviews: Product has no reviews → Empty list, no crash
    // =========================================================================
    @Test
    @DisplayName("TC11: Product has no reviews → Empty list, no crash")
    void getReviewsByProductId_noReviews_returnsEmptyList() {
        when(reviewRepository.findByProductIdOrderByCreatedAtDesc("prod-empty"))
                .thenReturn(Collections.emptyList());

        List<Review> result = reviewService.getReviewsByProductId("prod-empty");

        assertTrue(result.isEmpty());
    }

    // =========================================================================
    // TC12 — Read Reviews: Sorted newest first
    // =========================================================================
    @Test
    @DisplayName("TC12: Reviews sorted newest first (createdAt DESC)")
    void getReviewsByProductId_sortedNewestFirst() {
        Review older = Review.builder()
                .id("review-old")
                .productId("prod-001")
                .userId("user-001")
                .rating(5)
                .comment("Old review")
                .createdAt(LocalDateTime.now().minusDays(5))
                .build();
        Review newer = Review.builder()
                .id("review-new")
                .productId("prod-001")
                .userId("user-002")
                .rating(4)
                .comment("New review")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewRepository.findByProductIdOrderByCreatedAtDesc("prod-001"))
                .thenReturn(List.of(newer, older));

        List<Review> result = reviewService.getReviewsByProductId("prod-001");

        assertEquals("review-new", result.get(0).getId());
        assertEquals("review-old", result.get(1).getId());
    }

    // =========================================================================
    // TC13 — Edit Review: User edits own review → Success
    // =========================================================================
    @Test
    @DisplayName("TC13: User edits own review → Success")
    void updateReview_ownerEdits_success() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        // Stubs for updateProductRating path (product exists in repo)
        lenient().when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        lenient().when(reviewRepository.findByProductId("prod-001")).thenReturn(List.of(testReview));
        lenient().when(reviewRepository.countByProductId("prod-001")).thenReturn(1);

        Review result = reviewService.updateReview("review-001", "user-001", 4, "Updated comment!");

        assertNotNull(result);
        verify(reviewRepository).save(any(Review.class));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review saved = captor.getValue();
        assertEquals(4, saved.getRating());
        assertEquals("Updated comment!", saved.getComment());
        // productId and userId must remain unchanged
        assertEquals("prod-001", saved.getProductId());
        assertEquals("user-001", saved.getUserId());
    }

    // =========================================================================
    // TC14 — Edit Review: User edits another user's review → Denied
    // =========================================================================
    @Test
    @DisplayName("TC14: User edits another user's review → BadRequestException")
    void updateReview_notOwner_throwsException() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.updateReview("review-001", "user-002", 3, "Trying to edit")
        );

        assertEquals("Bạn không có quyền sửa đánh giá này", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC17 — Edit Review: Review not found → Error
    // =========================================================================
    @Test
    @DisplayName("TC17: Edit nonexistent review → ResourceNotFoundException")
    void updateReview_notFound_throwsException() {
        when(reviewRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.updateReview("nonexistent", "user-001", 4, "Comment")
        );

        assertEquals("Không tìm thấy đánh giá", ex.getMessage());
        verify(reviewRepository, never()).save(any());
    }

    // =========================================================================
    // TC15 — Edit Review: Attempt to change userId → Denied/ignored
    // =========================================================================
    @Test
    @DisplayName("TC15: Edit review → userId cannot be changed")
    void updateReview_userIdNotChanged() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));
        lenient().when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        lenient().when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));

        reviewService.updateReview("review-001", "user-001", 3, "Changed");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals("user-001", captor.getValue().getUserId());
    }

    // =========================================================================
    // TC16 — Edit Review: Attempt to change productId → Denied/ignored
    // =========================================================================
    @Test
    @DisplayName("TC16: Edit review → productId cannot be changed")
    void updateReview_productIdNotChanged() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));
        lenient().when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        lenient().when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));

        reviewService.updateReview("review-001", "user-001", 3, "Changed");

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertEquals("prod-001", captor.getValue().getProductId());
    }

    // =========================================================================
    // TC18 — Delete Review: User deletes own review → Success
    // =========================================================================
    @Test
    @DisplayName("TC18: User deletes own review → Success (hard delete)")
    void deleteReview_ownerDeletes_success() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));
        // Stub for updateProductRating path after delete
        lenient().when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        lenient().when(reviewRepository.findByProductId("prod-001")).thenReturn(Collections.emptyList());
        lenient().when(reviewRepository.countByProductId("prod-001")).thenReturn(0);

        reviewService.deleteReview("review-001", "user-001");

        verify(reviewRepository).delete(testReview);
    }

    // =========================================================================
    // TC19 — Delete Review: User deletes another user's review → Denied
    // =========================================================================
    @Test
    @DisplayName("TC19: User deletes another user's review → BadRequestException")
    void deleteReview_notOwner_throwsException() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> reviewService.deleteReview("review-001", "user-002")
        );

        assertEquals("Bạn không có quyền xóa đánh giá này", ex.getMessage());
        verify(reviewRepository, never()).delete(any());
    }

    // =========================================================================
    // TC20 — Delete Review: Delete nonexistent review → Error
    // =========================================================================
    @Test
    @DisplayName("TC20: Delete nonexistent review → ResourceNotFoundException")
    void deleteReview_notFound_throwsException() {
        when(reviewRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.deleteReview("nonexistent", "user-001")
        );

        assertEquals("Không tìm thấy đánh giá", ex.getMessage());
        verify(reviewRepository, never()).delete(any());
    }

    // =========================================================================
    // getReviewById tests
    // =========================================================================
    @Test
    @DisplayName("getReviewById: existing review → returns review")
    void getReviewById_exists_returnsReview() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));

        Review result = reviewService.getReviewById("review-001");

        assertEquals("review-001", result.getId());
    }

    @Test
    @DisplayName("getReviewById: not found → ResourceNotFoundException")
    void getReviewById_notFound_throwsException() {
        when(reviewRepository.findById("nonexistent")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.getReviewById("nonexistent")
        );

        assertEquals("Không tìm thấy đánh giá", ex.getMessage());
    }

    // =========================================================================
    // getReviewsByUserId tests
    // =========================================================================
    @Test
    @DisplayName("getReviewsByUserId: returns all reviews by user")
    void getReviewsByUserId_returnsReviews() {
        when(reviewRepository.findByUserId("user-001")).thenReturn(List.of(testReview));

        List<Review> result = reviewService.getReviewsByUserId("user-001");

        assertEquals(1, result.size());
        assertEquals("user-001", result.get(0).getUserId());
    }

    // =========================================================================
    // hasUserReviewedProduct tests
    // =========================================================================
    @Test
    @DisplayName("hasUserReviewedProduct: user has reviewed → true")
    void hasUserReviewedProduct_exists_returnsTrue() {
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001"))
                .thenReturn(Optional.of(testReview));

        assertTrue(reviewService.hasUserReviewedProduct("user-001", "prod-001"));
    }

    @Test
    @DisplayName("hasUserReviewedProduct: user has not reviewed → false")
    void hasUserReviewedProduct_notExists_returnsFalse() {
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001"))
                .thenReturn(Optional.empty());

        assertFalse(reviewService.hasUserReviewedProduct("user-001", "prod-001"));
    }

    // =========================================================================
    // getAverageRatingByProductId tests
    // =========================================================================
    @Test
    @DisplayName("getAverageRatingByProductId: multiple reviews → correct average")
    void getAverageRatingByProductId_multipleReviews_calculatesAverage() {
        Review r5 = Review.builder().id("r5").rating(5).build();
        Review r3 = Review.builder().id("r3").rating(3).build();
        Review r4 = Review.builder().id("r4").rating(4).build();

        when(reviewRepository.findByProductId("prod-001")).thenReturn(List.of(r5, r3, r4));

        double avg = reviewService.getAverageRatingByProductId("prod-001");

        assertEquals(4.0, avg, 0.01);
    }

    @Test
    @DisplayName("getAverageRatingByProductId: no reviews → 0.0")
    void getAverageRatingByProductId_noReviews_returnsZero() {
        when(reviewRepository.findByProductId("prod-001")).thenReturn(Collections.emptyList());

        double avg = reviewService.getAverageRatingByProductId("prod-001");

        assertEquals(0.0, avg);
    }

    // =========================================================================
    // getReviewCountByProductId tests
    // =========================================================================
    @Test
    @DisplayName("getReviewCountByProductId: returns count")
    void getReviewCountByProductId_returnsCount() {
        when(reviewRepository.countByProductId("prod-001")).thenReturn(5);

        int count = reviewService.getReviewCountByProductId("prod-001");

        assertEquals(5, count);
    }

    // =========================================================================
    // TC23 — Product rating updated after review creation
    // =========================================================================
    @Test
    @DisplayName("TC23: Product rating updated after review creation")
    void createReview_updatesProductRatingAndCount() {
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        when(orderService.hasUserPurchasedProduct("user-001", "prod-001")).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001")).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-new");
            return r;
        });
        when(reviewRepository.findByProductId("prod-001")).thenReturn(List.of(testReview));
        when(reviewRepository.countByProductId("prod-001")).thenReturn(1);

        reviewService.createReview("user-001", "prod-001", 5, "Great!");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertEquals(5.0, savedProduct.getRating(), 0.01);
        assertEquals(1, savedProduct.getReviewCount());
    }

    // =========================================================================
    // TC24 — Product rating recalculated after review deletion
    // =========================================================================
    @Test
    @DisplayName("TC24: Product rating recalculated after review deletion")
    void deleteReview_recalculatesProductRating() {
        when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findByProductId("prod-001")).thenReturn(Collections.emptyList());
        when(reviewRepository.countByProductId("prod-001")).thenReturn(0);

        reviewService.deleteReview("review-001", "user-001");

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();
        assertEquals(0.0, savedProduct.getRating(), 0.01);
        assertEquals(0, savedProduct.getReviewCount());
    }

    // =========================================================================
    // Validation: updateReview with invalid rating
    // =========================================================================
    @Nested
    @DisplayName("Update review validation")
    class UpdateReviewValidationTests {

        @BeforeEach
        void setupOwnerCheck() {
            when(reviewRepository.findById("review-001")).thenReturn(Optional.of(testReview));
        }

        @Test
        @DisplayName("Update review: rating = 0 → BadRequestException")
        void updateReview_ratingZero_throwsException() {
            BadRequestException ex = assertThrows(
                    BadRequestException.class,
                    () -> reviewService.updateReview("review-001", "user-001", 0, "Comment")
            );
            assertEquals("Rating phải từ 1 đến 5 sao", ex.getMessage());
        }

        @Test
        @DisplayName("Update review: rating = 6 → BadRequestException")
        void updateReview_ratingSix_throwsException() {
            BadRequestException ex = assertThrows(
                    BadRequestException.class,
                    () -> reviewService.updateReview("review-001", "user-001", 6, "Comment")
            );
            assertEquals("Rating phải từ 1 đến 5 sao", ex.getMessage());
        }

        @Test
        @DisplayName("Update review: empty comment → BadRequestException")
        void updateReview_emptyComment_throwsException() {
            BadRequestException ex = assertThrows(
                    BadRequestException.class,
                    () -> reviewService.updateReview("review-001", "user-001", 4, "")
            );
            assertEquals("Nội dung đánh giá không được để trống", ex.getMessage());
        }

        @Test
        @DisplayName("Update review: whitespace comment → BadRequestException")
        void updateReview_whitespaceComment_throwsException() {
            BadRequestException ex = assertThrows(
                    BadRequestException.class,
                    () -> reviewService.updateReview("review-001", "user-001", 4, "   ")
            );
            assertEquals("Nội dung đánh giá không được để trống", ex.getMessage());
        }

        @Test
        @DisplayName("Update review: comment is trimmed before saving")
        void updateReview_commentTrimmed() {
            lenient().when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
            lenient().when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
            lenient().when(reviewRepository.findByProductId("prod-001")).thenReturn(List.of(testReview));
            lenient().when(reviewRepository.countByProductId("prod-001")).thenReturn(1);

            reviewService.updateReview("review-001", "user-001", 4, "  Trimmed comment  ");

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            assertEquals("Trimmed comment", captor.getValue().getComment());
        }
    }

    // =========================================================================
    // TC26 — TASK #14/#20/#21: Hệ thống BẮT BUỘC xác nhận DELIVERED trước khi cho review
    // =========================================================================
    @Test
    @DisplayName("TC26: System enforces DELIVERED verification before review (TASK #14)")
    void createReview_deliveredCheckIsEnforced() {
        // Khi Order đã DELIVERED → hasUserReceivedProduct trả true → review thành công
        when(userRepository.findById("user-001")).thenReturn(Optional.of(testUser));
        when(productRepository.findById("prod-001")).thenReturn(Optional.of(testProduct));
        // TASK #14: check DELIVERED status
        when(orderService.hasUserReceivedProduct("user-001", "prod-001")).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001")).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId("review-new");
            return r;
        });
        when(reviewRepository.findByProductId("prod-001")).thenReturn(List.of(testReview));
        when(reviewRepository.countByProductId("prod-001")).thenReturn(1);

        Review result = reviewService.createReview("user-001", "prod-001", 5, "Purchased review");

        assertNotNull(result);
        // TASK #14/#20/#21 — đã gọi hasUserReceivedProduct (Order DELIVERED)
        verify(orderService, times(1)).hasUserReceivedProduct("user-001", "prod-001");
        verify(reviewRepository).save(any(Review.class));
    }

    // =========================================================================
    // canUserReviewProduct — combined check (DELIVERED purchase + not yet reviewed)
    // TASK #14: phải có Order DELIVERED mới được review
    // =========================================================================
    @Test
    @DisplayName("canUserReviewProduct: DELIVERED + not reviewed → true")
    void canUserReviewProduct_deliveredNotReviewed_returnsTrue() {
        when(orderService.hasUserReceivedProduct("user-001", "prod-001")).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001")).thenReturn(Optional.empty());

        assertTrue(reviewService.canUserReviewProduct("user-001", "prod-001"));
    }

    @Test
    @DisplayName("canUserReviewProduct: DELIVERED + already reviewed → false")
    void canUserReviewProduct_deliveredAlreadyReviewed_returnsFalse() {
        when(orderService.hasUserReceivedProduct("user-001", "prod-001")).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId("prod-001", "user-001")).thenReturn(Optional.of(testReview));

        assertFalse(reviewService.canUserReviewProduct("user-001", "prod-001"));
    }

    @Test
    @DisplayName("canUserReviewProduct: NOT DELIVERED → false")
    void canUserReviewProduct_notDelivered_returnsFalse() {
        when(orderService.hasUserReceivedProduct("user-001", "prod-001")).thenReturn(false);

        assertFalse(reviewService.canUserReviewProduct("user-001", "prod-001"));
    }
}
