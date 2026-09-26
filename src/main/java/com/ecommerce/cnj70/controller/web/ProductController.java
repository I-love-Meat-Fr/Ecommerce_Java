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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private static final Path LOG_FILE = Paths.get(".cursor", "debug-04f262.log");

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
        // #region DEBUG: server-side trace - entered productDetail
        try {
            Path parent = LOG_FILE.getParent();
            if (parent != null && !Files.exists(parent)) Files.createDirectories(parent);
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"sessionId\":\"04f262\",")
              .append("\"runId\":\"initial\",")
              .append("\"hypothesisId\":\"SERVER\",")
              .append("\"location\":\"ProductController.productDetail:ENTRY\",")
              .append("\"message\":\"productDetail entry\",")
              .append("\"data\":{\"id\":\"").append(id == null ? "null" : id).append("\"},")
              .append("\"timestamp\":").append(System.currentTimeMillis())
              .append("}\n");
            Files.writeString(LOG_FILE, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        // #endregion
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);

        // Get reviews using ReviewService.
        // Dùng overload viewer-aware: DELETED ẩn với tất cả, HIDDEN chỉ owner thấy.
        String viewerId = (user != null) ? user.getId() : null;
        List<Review> reviews = reviewService.getVisibleReviewsByProductId(id, viewerId);
        List<ReviewRes> reviewList = reviews.stream()
                .map(review -> ReviewRes.builder()
                        .id(review.getId())
                        .productId(review.getProductId())
                        .userId(review.getUserId())
                        .userName(review.getUserName())
                        .userAvatar(review.getUserAvatar())
                        .rating(review.getRating())
                        .comment(review.getComment())
                        .createdAt(review.getCreatedAt() != null ? review.getCreatedAt() : LocalDateTime.now())
                        // TASK #26 — Trả ảnh + moderation status để template render.
                        .images(review.getImages())
                        .moderationStatus(review.getModerationStatus())
                        .moderationReason(review.getModerationReason())
                        .reportCount(review.getReportCount())
                        .build())
                .collect(Collectors.toList());
        // #region DEBUG: server-side trace - reviews loaded
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"sessionId\":\"04f262\",")
              .append("\"runId\":\"initial\",")
              .append("\"hypothesisId\":\"SERVER\",")
              .append("\"location\":\"ProductController.productDetail:REVIEWS\",")
              .append("\"message\":\"reviews loaded\",")
              .append("\"data\":{\"count\":").append(reviewList.size())
              .append(",\"firstCommentNull\":").append(reviewList.isEmpty() ? "false" : String.valueOf(reviewList.get(0).getComment() == null))
              .append(",\"firstCreatedAtNull\":").append(reviewList.isEmpty() ? "false" : String.valueOf(reviewList.get(0).getCreatedAt() == null))
              .append("},")
              .append("\"timestamp\":").append(System.currentTimeMillis())
              .append("}\n");
            Files.writeString(LOG_FILE, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        // #endregion
        model.addAttribute("reviews", reviewList);

        // Check review eligibility for current user.
        //
        // 3 trạng thái có thể có (chỉ áp dụng khi user đã đăng nhập):
        //   1. hasReviewed=true        → đã review rồi
        //   2. canReview=true          → đã nhận hàng, chưa review, đủ điều kiện
        //   3. canReview=false         → CHƯA đủ điều kiện (chưa mua / chưa nhận / đã hủy)
        //
        // Backend `reviewService.canUserReviewProduct` đã enforce:
        //   order.status == DELIVERED && !hasReviewed
        //
        // Nếu UI cho phép nhập form khi backend từ chối → user submit sẽ bị
        // BadRequestException → redirect kèm ?error=... (xấu + có thể bị mất khi share link).
        // Fix: chỉ render form khi canReview=true; các trường hợp khác hiển thị message
        // tương ứng ("đã đánh giá" / "cần mua và nhận hàng trước").
        if (user != null) {
            boolean hasReviewed = reviewService.hasUserReviewedProduct(user.getId(), id);
            boolean canReview = !hasReviewed && reviewService.canUserReviewProduct(user.getId(), id);
            model.addAttribute("hasReviewed", hasReviewed);
            model.addAttribute("canReview", canReview);
        } else {
            // Anonymous user — không cho review, không có flag nào.
            model.addAttribute("hasReviewed", false);
            model.addAttribute("canReview", false);
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

        // #region DEBUG: server-side trace - about to render template
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{")
              .append("\"sessionId\":\"04f262\",")
              .append("\"runId\":\"initial\",")
              .append("\"hypothesisId\":\"SERVER\",")
              .append("\"location\":\"ProductController.productDetail:RENDER\",")
              .append("\"message\":\"about to render template\",")
              .append("\"data\":{\"relatedCount\":").append(relatedProducts.size()).append("},")
              .append("\"timestamp\":").append(System.currentTimeMillis())
              .append("}\n");
            Files.writeString(LOG_FILE, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        // #endregion

        return "web/product-detail";
    }
}
