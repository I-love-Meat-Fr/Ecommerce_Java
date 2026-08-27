package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.dto.request.ReviewReq;
import com.ecommerce.cnj70.dto.response.ReviewRes;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;
    
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ReviewRes>> getProductReviews(@PathVariable String productId) {
        List<Review> reviews = reviewService.getReviewsByProductId(productId);
        List<ReviewRes> response = reviews.stream()
                .map(this::toReviewRes)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/products/{productId}/reviews")
    public String createReview(@PathVariable String productId,
                             @AuthenticationPrincipal CustomUserDetails user,
                             @ModelAttribute @Valid ReviewReq request,
                             Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        try {
            reviewService.createReview(user.getId(), productId, request.getRating(), request.getComment());
            return "redirect:/products/" + productId;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/products/" + productId + "?error=" + e.getMessage();
        }
    }
    
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewRes> getReview(@PathVariable String reviewId) {
        Review review = reviewService.getReviewById(reviewId);
        return ResponseEntity.ok(toReviewRes(review));
    }
    
    @PostMapping("/reviews/{reviewId}/edit")
    public String updateReview(@PathVariable String reviewId,
                             @AuthenticationPrincipal CustomUserDetails user,
                             @ModelAttribute @Valid ReviewReq request,
                             Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        try {
            reviewService.updateReview(reviewId, user.getId(), request.getRating(), request.getComment());
            Review review = reviewService.getReviewById(reviewId);
            return "redirect:/products/" + review.getProductId();
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/products/" + reviewService.getReviewById(reviewId).getProductId();
        }
    }
    
    @PostMapping("/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable String reviewId,
                              @AuthenticationPrincipal CustomUserDetails user) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        Review review = reviewService.getReviewById(reviewId);
        String productId = review.getProductId();
        
        try {
            reviewService.deleteReview(reviewId, user.getId());
        } catch (Exception e) {
            // Log error but continue
        }
        
        return "redirect:/products/" + productId;
    }
    
    @GetMapping("/my-reviews")
    public String myReviews(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        List<Review> reviews = reviewService.getReviewsByUserId(user.getId());
        List<ReviewRes> response = reviews.stream()
                .map(this::toReviewRes)
                .collect(Collectors.toList());
        
        model.addAttribute("reviews", response);
        return "web/my-reviews";
    }
    
    @GetMapping("/api/reviews/check")
    @ResponseBody
    public ResponseEntity<Boolean> checkUserReviewed(@AuthenticationPrincipal CustomUserDetails user,
                                                    @RequestParam String productId) {
        if (user == null) {
            return ResponseEntity.ok(false);
        }
        boolean hasReviewed = reviewService.hasUserReviewedProduct(user.getId(), productId);
        return ResponseEntity.ok(hasReviewed);
    }
    
    private ReviewRes toReviewRes(Review review) {
        return ReviewRes.builder()
                .id(review.getId())
                .productId(review.getProductId())
                .userId(review.getUserId())
                .userName(review.getUserName())
                .userAvatar(review.getUserAvatar())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .build();
    }
}
