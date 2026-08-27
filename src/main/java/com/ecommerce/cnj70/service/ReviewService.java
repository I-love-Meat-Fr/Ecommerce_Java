package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Review;

import java.util.List;

public interface ReviewService {
    
    Review createReview(String userId, String productId, int rating, String comment);
    
    Review updateReview(String reviewId, String userId, int rating, String comment);
    
    void deleteReview(String reviewId, String userId);
    
    Review getReviewById(String reviewId);
    
    List<Review> getReviewsByProductId(String productId);
    
    List<Review> getReviewsByUserId(String userId);
    
    boolean hasUserReviewedProduct(String userId, String productId);
    
    double getAverageRatingByProductId(String productId);
    
    int getReviewCountByProductId(String productId);
}
