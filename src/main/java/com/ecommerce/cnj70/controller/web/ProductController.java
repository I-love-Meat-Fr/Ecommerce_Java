package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.dto.response.ReviewRes;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    private final ReviewService reviewService;
    
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable String id, Model model, @AuthenticationPrincipal CustomUserDetails user) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        
        List<Review> reviews = reviewService.getReviewsByProductId(id);
        List<ReviewRes> reviewList = reviews.stream()
                .map(review -> ReviewRes.builder()
                        .id(review.getId())
                        .productId(review.getProductId())
                        .userId(review.getUserId())
                        .userName(review.getUserName())
                        .userAvatar(review.getUserAvatar())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
        model.addAttribute("reviews", reviewList);
        
        if (user != null) {
            boolean hasReviewed = reviewService.hasUserReviewedProduct(user.getId(), id);
            model.addAttribute("hasReviewed", hasReviewed);
        }
        
        return "web/product-detail";
    }
}
