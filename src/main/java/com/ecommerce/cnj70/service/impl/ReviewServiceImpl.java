package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {
    
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    
    @Override
    public Review createReview(String userId, String productId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating phải từ 1 đến 5 sao");
        }
        
        if (comment == null || comment.trim().isEmpty()) {
            throw new BadRequestException("Nội dung đánh giá không được để trống");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        
        if (reviewRepository.findByProductIdAndUserId(productId, userId).isPresent()) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này rồi");
        }
        
        Review review = Review.builder()
                .productId(productId)
                .userId(userId)
                .userName(user.getFullName())
                .userAvatar(user.getAvatarUrl())
                .rating(rating)
                .comment(comment.trim())
                .build();
        
        Review savedReview = reviewRepository.save(review);
        
        updateProductRating(product.getId());
        
        return savedReview;
    }
    
    @Override
    public Review updateReview(String reviewId, String userId, int rating, String comment) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        
        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền sửa đánh giá này");
        }
        
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating phải từ 1 đến 5 sao");
        }
        
        if (comment == null || comment.trim().isEmpty()) {
            throw new BadRequestException("Nội dung đánh giá không được để trống");
        }
        
        review.setRating(rating);
        review.setComment(comment.trim());
        
        Review updatedReview = reviewRepository.save(review);
        
        updateProductRating(review.getProductId());
        
        return updatedReview;
    }
    
    @Override
    public void deleteReview(String reviewId, String userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        
        if (!review.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xóa đánh giá này");
        }
        
        String productId = review.getProductId();
        reviewRepository.delete(review);
        
        updateProductRating(productId);
    }
    
    @Override
    public Review getReviewById(String reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
    }
    
    @Override
    public List<Review> getReviewsByProductId(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }
    
    @Override
    public List<Review> getReviewsByUserId(String userId) {
        return reviewRepository.findByUserId(userId);
    }
    
    @Override
    public boolean hasUserReviewedProduct(String userId, String productId) {
        return reviewRepository.findByProductIdAndUserId(productId, userId).isPresent();
    }
    
    @Override
    public double getAverageRatingByProductId(String productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }
        return reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
    }
    
    @Override
    public int getReviewCountByProductId(String productId) {
        return reviewRepository.countByProductId(productId);
    }
    
    private void updateProductRating(String productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            double avgRating = getAverageRatingByProductId(productId);
            int reviewCount = getReviewCountByProductId(productId);
            product.setRating(avgRating);
            product.setReviewCount(reviewCount);
            productRepository.save(product);
        }
    }
}
