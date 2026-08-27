package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.dto.response.ReviewRes;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    
    @GetMapping("/products")
    public String productsList(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "1") int page,
            Model model) {
        
        // Get categories for filter bar
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        
        // Build sort
        Sort sortOrder;
        switch (sort) {
            case "price-asc":
                sortOrder = Sort.by(Sort.Direction.ASC, "price");
                break;
            case "price-desc":
                sortOrder = Sort.by(Sort.Direction.DESC, "price");
                break;
            case "rating":
                sortOrder = Sort.by(Sort.Direction.DESC, "rating");
                break;
            default:
                sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        // Pagination
        Pageable pageable = PageRequest.of(page - 1, 20, sortOrder);
        
        // Search and filter
        Page<Product> productPage;
        if (search != null && !search.trim().isEmpty()) {
            productPage = productService.searchProducts(search.trim(), pageable);
        } else if (category != null && !category.trim().isEmpty()) {
            productPage = productService.getProductsByCategory(category, pageable);
        } else {
            productPage = productService.getAllProducts(pageable);
        }
        
        List<Product> products = productPage.getContent();
        
        // Calculate total products (without pagination limit)
        long totalProducts = productPage.getTotalElements();
        
        // Find category name
        String selectedCategoryName = null;
        if (category != null) {
            for (Category cat : categories) {
                if (cat.getId().equals(category)) {
                    selectedCategoryName = cat.getName();
                    break;
                }
            }
        }
        
        // Add attributes
        model.addAttribute("products", products);
        model.addAttribute("search", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedCategoryName", selectedCategoryName);
        model.addAttribute("sort", sort);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalProducts", totalProducts);
        
        return "web/products";
    }
    
    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable String id, Model model, @AuthenticationPrincipal CustomUserDetails user) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        
        // Get reviews using ReviewService
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
        
        // Check if current user has reviewed
        if (user != null) {
            boolean hasReviewed = reviewService.hasUserReviewedProduct(user.getId(), id);
            model.addAttribute("hasReviewed", hasReviewed);
        }
        
        // Get related products from same category
        List<Product> relatedProducts = Collections.emptyList();
        if (product.getCategoryId() != null && !product.getCategoryId().isEmpty()) {
            List<Product> sameCategory = productService.getProductsByCategory(product.getCategoryId());
            relatedProducts = sameCategory.stream()
                    .filter(p -> !p.getId().equals(product.getId()))
                    .limit(5)
                    .toList();
        }
        model.addAttribute("relatedProducts", relatedProducts);
        
        return "web/product-detail";
    }
}
