package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.repository.CategoryRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {
    
    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    
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
    public String productDetail(@PathVariable String id, Model model) {
        Product product = productService.getProductById(id);
        
        // Get reviews for the product
        List<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(id);
        if (reviews == null) {
            reviews = Collections.emptyList();
        }
        model.addAttribute("reviews", reviews);
        
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
        
        model.addAttribute("product", product);
        return "web/product-detail";
    }
}
