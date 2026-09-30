package com.ecommerce.cnj70.exception;

/**
 * Yêu cầu #2 (Verified Purchase) — ném ra khi Customer cố tạo Review nhưng
 * chưa mua + nhận hàng thành công sản phẩm đó.
 *
 * <p>Áp dụng cho cả {@code ReviewService.createReview} và {@code updateReview}.</p>
 *
 * <p>HTTP status tương ứng: 400 Bad Request (lỗi nghiệp vụ, không phải 404).</p>
 *
 * <p>Message chuẩn (theo yêu cầu): "Bạn chỉ có thể đánh giá sản phẩm sau khi
 * đã mua và nhận hàng thành công".</p>
 */
public class VerifiedPurchaseException extends BadRequestException {

    public static final String DEFAULT_MESSAGE =
            "Bạn chỉ có thể đánh giá sản phẩm sau khi đã mua và nhận hàng thành công";

    public VerifiedPurchaseException() {
        super(DEFAULT_MESSAGE);
    }

    public VerifiedPurchaseException(String message) {
        super(message);
    }

    public VerifiedPurchaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
