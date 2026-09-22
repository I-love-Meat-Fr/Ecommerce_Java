package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.AdminReviewService;
import com.ecommerce.cnj70.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * TASK #15 — Admin Review Moderation Controller.
 *
 * Routes:
 *   GET  /admin/reviews              - Review List (search + filter rating + pagination)
 *   GET  /admin/reviews/{id}         - Review Detail
 *   POST /admin/reviews/{id}/delete  - Delete (hard delete, dùng cho AdminReviewService)
 *   POST /admin/reviews/{id}/hide   - Moderator ẩn review
 *   POST /admin/reviews/{id}/restore - Moderator khôi phục review
 *
 * Security: /admin/** đã được bảo vệ bởi SecurityConfig.hasRole("ADMIN").
 */
@Controller
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AdminReviewService adminReviewService;
    private final ProductRepository productRepository;
    private final ReviewService reviewService;

    @GetMapping
    public String reviewList(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) Integer rating,
                             Model model) {
        int safeSize = (size <= 0) ? DEFAULT_PAGE_SIZE : Math.min(size, 50);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Integer ratingFilter = parseRating(rating);

        Page<Review> result = adminReviewService.listReviews(pageable, q, ratingFilter);

        model.addAttribute("reviews", result.getContent());
        model.addAttribute("page", result.getNumber());
        model.addAttribute("size", result.getSize());
        model.addAttribute("totalPages", result.getTotalPages());
        model.addAttribute("totalItems", result.getTotalElements());
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("rating", ratingFilter == null ? "" : ratingFilter.toString());
        model.addAttribute("hasNext", result.hasNext());
        model.addAttribute("hasPrev", result.hasPrevious());
        model.addAttribute("isFirst", result.isFirst());
        model.addAttribute("isLast", result.isLast());
        model.addAttribute("pageNumbers", computePageRange(result.getNumber(), result.getTotalPages()));
        return "admin/review-list";
    }

    @GetMapping("/{id}")
    public String reviewDetail(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) String rating,
                               Model model) {
        Review review = adminReviewService.getReviewById(id);

        Product product = null;
        if (review.getProductId() != null && !review.getProductId().isBlank()) {
            try {
                product = productRepository.findById(review.getProductId()).orElse(null);
            } catch (Exception ex) {
                product = null;
            }
        }

        model.addAttribute("review", review);
        model.addAttribute("product", product);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("rating", rating == null ? "" : rating);
        return "admin/review-detail";
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) String rating,
                               RedirectAttributes redirectAttributes) {
        try {
            adminReviewService.deleteReview(id);
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã xóa đánh giá vi phạm (ID: " + id + ")");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, rating);
    }

    @PostMapping("/{id}/hide")
    public String hideReview(@PathVariable String id,
                           @RequestParam(required = false) String reason,
                           @RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "10") int size,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String rating,
                           @AuthenticationPrincipal UserDetails userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            reviewService.hideReview(id,
                    userDetails != null ? userDetails.getUsername() : "ADMIN",
                    reason != null ? reason : "Vi phạm nội quy đánh giá");
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã ẩn đánh giá (ID: " + id + ")");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, rating);
    }

    @PostMapping("/{id}/restore")
    public String restoreReview(@PathVariable String id,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(required = false) String q,
                               @RequestParam(required = false) String rating,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            reviewService.restoreReview(id,
                    userDetails != null ? userDetails.getUsername() : "ADMIN");
            redirectAttributes.addFlashAttribute("flashSuccess",
                    "Đã khôi phục đánh giá (ID: " + id + ")");
        } catch (ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("flashError", ex.getMessage());
        }
        return buildRedirectUrl(page, size, q, rating);
    }

    private String buildRedirectUrl(int page, int size, String q, String rating) {
        StringBuilder url = new StringBuilder("/admin/reviews?")
                .append("page=").append(page)
                .append("&size=").append(size);
        if (q != null && !q.isBlank()) {
            url.append("&q=").append(q);
        }
        if (rating != null && !rating.isBlank()) {
            url.append("&rating=").append(rating);
        }
        return "redirect:" + url;
    }

    private static List<Integer> computePageRange(int current, int totalPages) {
        List<Integer> out = new ArrayList<>();
        if (totalPages <= 0) return out;
        int start = Math.max(0, current - 2);
        int end = Math.min(totalPages - 1, current + 2);
        for (int i = start; i <= end; i++) out.add(i);
        return out;
    }

    private static Integer parseRating(Integer raw) {
        if (raw == null) return null;
        if (raw < 1 || raw > 5) return null;
        return raw;
    }
}
