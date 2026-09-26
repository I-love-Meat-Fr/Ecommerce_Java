package com.ecommerce.cnj70.controller.web;

import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.dto.request.ReportReviewReq;
import com.ecommerce.cnj70.dto.request.ReviewReq;
import com.ecommerce.cnj70.dto.response.ReviewRes;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ReviewService;
import com.ecommerce.cnj70.util.FileUploadUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class ReviewController {
    
    private final ReviewService reviewService;

    /**
     * TASK #15/TASK #26 — Product Detail Page: trả review về client.
     * Áp dụng viewer-aware filtering để ẩn review HIDDEN với non-owner và
     * DELETED với tất cả mọi người.
     */
    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<List<ReviewRes>> getProductReviews(
            @PathVariable String productId,
            @AuthenticationPrincipal CustomUserDetails user) {
        String viewerId = (user != null) ? user.getId() : null;
        List<Review> reviews = reviewService.getVisibleReviewsByProductId(productId, viewerId);
        List<ReviewRes> response = reviews.stream()
                .map(this::toReviewRes)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/products/{productId}/reviews")
    public String createReview(@PathVariable String productId,
                             @AuthenticationPrincipal CustomUserDetails user,
                             @ModelAttribute @Valid ReviewReq request,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        try {
            reviewService.createReview(user.getId(), productId, request.getRating(), request.getComment());
            // Flash success thay vì truyền qua URL (gọn + không leak info qua query string).
            redirectAttributes.addFlashAttribute("reviewSuccess",
                    "Cảm ơn bạn đã đánh giá sản phẩm!");
            return "redirect:/products/" + productId;
        } catch (Exception e) {
            // Phân loại lỗi để UI render đúng context (Block-by-backend vs Validation vs khác).
            // Backend `reviewService.createReview` throw BadRequestException với message:
            //   - "Bạn chỉ có thể đánh giá sản phẩm sau khi đã nhận được hàng"
            //   - "Bạn đã đánh giá sản phẩm này rồi"
            //   - "Rating phải từ 1 đến 5 sao"
            //   - "Nội dung đánh giá không được để trống"
            String msg = e.getMessage() != null ? e.getMessage() : "";
            String errorCode;
            if (msg.contains("đã nhận được hàng")) {
                // Backend reject vì user chưa nhận hàng — UI đã ẩn form, nhưng nếu POST
                // trực tiếp thì vẫn redirect kèm errorCode để template hiển thị banner riêng.
                errorCode = "REVIEW_BLOCKED_NOT_RECEIVED";
            } else if (msg.contains("đã đánh giá")) {
                errorCode = "REVIEW_ALREADY_DONE";
            } else {
                errorCode = "REVIEW_VALIDATION";
            }
            redirectAttributes.addFlashAttribute("reviewError", msg);
            redirectAttributes.addFlashAttribute("reviewErrorCode", errorCode);
            // KHÔNG truyền ?error=... qua URL nữa — flash attribute sẽ được đọc
            // bởi `pdp-review-error-banner` trong template sau khi redirect.
            return "redirect:/products/" + productId;
        }
    }
    
    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewRes> getReview(@PathVariable String reviewId,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        Review review = reviewService.getReviewById(reviewId);

        // TASK #15 — Public API: review DELETED không ai xem được.
        // HIDDEN: chỉ owner xem được qua API này (Moderator/Admin xem qua /admin/reviews).
        boolean isOwner = user != null && user.getId().equals(review.getUserId());
        if (review.getModerationStatus() == com.ecommerce.cnj70.enums.ReviewModerationStatus.DELETED) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá");
        }
        if ((review.getModerationStatus() == com.ecommerce.cnj70.enums.ReviewModerationStatus.HIDDEN
                || review.isHidden())
                && !isOwner) {
            throw new ResourceNotFoundException("Không tìm thấy đánh giá");
        }

        return ResponseEntity.ok(toReviewRes(review));
    }
    
    @PostMapping("/reviews/{reviewId}/edit")
    public String updateReview(@PathVariable String reviewId,
                             @AuthenticationPrincipal CustomUserDetails user,
                             @ModelAttribute @Valid ReviewReq request,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        try {
            Review review = reviewService.updateReview(reviewId, user.getId(), request.getRating(), request.getComment());
            redirectAttributes.addFlashAttribute("reviewSuccess",
                    "Đã cập nhật đánh giá thành công!");
            return "redirect:/products/" + review.getProductId() + "#review-" + reviewId;
        } catch (Exception e) {
            // Cố gắng lấy productId để redirect an toàn; nếu review không tồn tại
            // thì fallback về trang chủ.
            String fallback = "/";
            try {
                Review r = reviewService.getReviewById(reviewId);
                fallback = "/products/" + r.getProductId();
            } catch (Exception ignored) {}
            redirectAttributes.addFlashAttribute("reviewError", e.getMessage());
            return "redirect:" + fallback + "#review-" + reviewId;
        }
    }
    
    @PostMapping("/reviews/{reviewId}/delete")
    public String deleteReview(@PathVariable String reviewId,
                              @AuthenticationPrincipal CustomUserDetails user,
                              RedirectAttributes redirectAttributes) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        String fallback = "/";
        try {
            Review review = reviewService.getReviewById(reviewId);
            fallback = "/products/" + review.getProductId();
            // Service đã enforce ownership check (throw BadRequestException nếu không phải owner)
            reviewService.deleteReview(reviewId, user.getId());
            redirectAttributes.addFlashAttribute("reviewSuccess", "Đã xóa đánh giá.");
        } catch (com.ecommerce.cnj70.exception.ResourceNotFoundException ex) {
            redirectAttributes.addFlashAttribute("reviewError", "Đánh giá không tồn tại hoặc đã bị xóa.");
        } catch (BadRequestException ex) {
            // Bao gồm cả "không có quyền" — UI sẽ hiển thị message thân thiện
            redirectAttributes.addFlashAttribute("reviewError", ex.getMessage());
        } catch (Exception e) {
            // Log + fallback graceful: vẫn redirect nhưng không hiển thị success
            redirectAttributes.addFlashAttribute("reviewError", "Có lỗi khi xóa đánh giá.");
        }
        
        return "redirect:" + fallback;
    }
    
    @GetMapping("/my-reviews")
    public String myReviews(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        // Trang /my-reviews hiển thị TẤT CẢ review của user, kể cả HIDDEN/REPORTED
        // (vì owner cần thấy để edit/restore). DELETED thì vẫn ẩn vì xóa là xóa.
        List<Review> reviews = reviewService.getReviewsByUserId(user.getId()).stream()
                .filter(r -> r.getModerationStatus()
                        != com.ecommerce.cnj70.enums.ReviewModerationStatus.DELETED)
                .collect(Collectors.toList());
        List<ReviewRes> response = reviews.stream()
                .map(this::toReviewRes)
                .collect(Collectors.toList());
        
        model.addAttribute("reviews", response);
        return "web/my-reviews";
    }

    // ============= TASK #26: Review Images API (owner-only) =============

    /**
     * Upload một ảnh cho Review.
     * <ul>
     *   <li>Chỉ owner Review mới upload được (service check).</li>
     *   <li>Tối đa 5 ảnh / Review.</li>
     *   <li>Validate size/MIME/extension qua FileUploadUtil.</li>
     * </ul>
     *
     * @return JSON {ok, imageUrl, message} hoặc {ok=false, errorCode, message}
     */
    @PostMapping("/api/reviews/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadReviewImage(
            @PathVariable String reviewId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> resp = new HashMap<>();
        if (user == null) {
            resp.put("ok", false);
            resp.put("errorCode", "AUTH_REQUIRED");
            resp.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            // Validate + lưu file
            String imageUrl = FileUploadUtil.saveFile(file);
            // Service sẽ check ownership + duplicate + max-count
            Review updated = reviewService.addReviewImage(reviewId, user.getId(), imageUrl);
            resp.put("ok", true);
            resp.put("imageUrl", imageUrl);
            resp.put("totalImages", updated.getImages() == null ? 0 : updated.getImages().size());
            resp.put("message", "Đã thêm ảnh vào đánh giá.");
            return ResponseEntity.ok(resp);
        } catch (BadRequestException ex) {
            // Phân loại: "không có quyền" vs validation vs giới hạn 5 ảnh
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            String code;
            if (msg.contains("quyền")) code = "NOT_OWNER";
            else if (msg.contains("tối đa")) code = "MAX_IMAGES_REACHED";
            else if (msg.contains("đã tồn tại")) code = "DUPLICATE";
            else code = "VALIDATION";
            resp.put("ok", false);
            resp.put("errorCode", code);
            resp.put("message", msg);
            return ResponseEntity.badRequest().body(resp);
        } catch (IOException ioex) {
            resp.put("ok", false);
            resp.put("errorCode", "IO_ERROR");
            resp.put("message", "Không thể lưu file ảnh");
            return ResponseEntity.status(500).body(resp);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("errorCode", "INTERNAL");
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    /**
     * Xóa một ảnh khỏi Review. Chỉ owner mới xóa được (service check).
     */
    @DeleteMapping("/api/reviews/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteReviewImage(
            @PathVariable String reviewId,
            @RequestParam("imageUrl") String imageUrl,
            @AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> resp = new HashMap<>();
        if (user == null) {
            resp.put("ok", false);
            resp.put("errorCode", "AUTH_REQUIRED");
            resp.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            Review updated = reviewService.removeReviewImage(reviewId, user.getId(), imageUrl);
            resp.put("ok", true);
            resp.put("totalImages", updated.getImages() == null ? 0 : updated.getImages().size());
            resp.put("message", "Đã xóa ảnh khỏi đánh giá.");
            return ResponseEntity.ok(resp);
        } catch (BadRequestException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "";
            String code;
            if (msg.contains("quyền")) code = "NOT_OWNER";
            else if (msg.contains("không tồn tại")) code = "IMAGE_NOT_FOUND";
            else code = "VALIDATION";
            resp.put("ok", false);
            resp.put("errorCode", code);
            resp.put("message", msg);
            return ResponseEntity.badRequest().body(resp);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("errorCode", "INTERNAL");
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    /**
     * TASK #26 — Lấy chi tiết 1 review (kèm ảnh) cho modal edit ảnh phía client.
     * Owner check: chỉ owner mới gọi được endpoint này (kể cả khi review HIDDEN).
     */
    @GetMapping("/api/reviews/{reviewId}/images")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getReviewImages(
            @PathVariable String reviewId,
            @AuthenticationPrincipal CustomUserDetails user) {
        Map<String, Object> resp = new HashMap<>();
        if (user == null) {
            resp.put("ok", false);
            resp.put("errorCode", "AUTH_REQUIRED");
            resp.put("message", "Vui lòng đăng nhập");
            return ResponseEntity.status(401).body(resp);
        }
        try {
            Review review = reviewService.getReviewById(reviewId);
            // Ownership check: chỉ owner mới list được images của review
            if (!user.getId().equals(review.getUserId())) {
                resp.put("ok", false);
                resp.put("errorCode", "NOT_OWNER");
                resp.put("message", "Bạn không có quyền xem ảnh của đánh giá này");
                return ResponseEntity.status(403).body(resp);
            }
            resp.put("ok", true);
            resp.put("images", review.getImages());
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("errorCode", "INTERNAL");
            resp.put("message", e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
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

    /**
     * TASK #21 — Check user có quyền review Product không (đã mua thành công).
     * Frontend dùng để hiển thị form review hoặc message "Bạn cần mua sản phẩm trước".
     */
    @GetMapping("/api/reviews/can-review")
    @ResponseBody
    public ResponseEntity<Boolean> checkCanReview(@AuthenticationPrincipal CustomUserDetails user,
                                                  @RequestParam String productId) {
        if (user == null) {
            return ResponseEntity.ok(false);
        }
        return ResponseEntity.ok(reviewService.canUserReviewProduct(user.getId(), productId));
    }

    /**
     * TASK #15 — Customer report một Review.
     * POST /api/reviews/{reviewId}/report
     */
    @PostMapping("/api/reviews/{reviewId}/report")
    @ResponseBody
    public ResponseEntity<String> reportReview(
            @PathVariable String reviewId,
            @RequestBody @Valid ReportReviewReq request,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).body("Vui lòng đăng nhập để report");
        }

        try {
            reviewService.reportReview(reviewId, user.getId(), request.getReason());
            return ResponseEntity.ok("Đã gửi report thành công. Cảm ơn bạn đã phản ánh!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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
                .images(review.getImages())
                .moderationStatus(review.getModerationStatus())
                .moderationReason(review.getModerationReason())
                .reportCount(review.getReportCount())
                .build();
    }
}
