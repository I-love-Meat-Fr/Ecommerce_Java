package com.ecommerce.cnj70.dto.cart;

/**
 * Per-item validation result for cart items.
 * <p>
 * Mỗi CartItem được server kiểm tra lại theo thời gian thực (real-time) dựa trên:
 * <ul>
 *   <li>{@code Product.status} — phải là ACTIVE</li>
 *   <li>{@code Shop.active} + {@code Shop.status} — phải active & APPROVED</li>
 *   <li>{@code Product.stock} — phải >= quantity user đặt</li>
 *   <li>Sự tồn tại của Product/Shop trong DB</li>
 * </ul>
 * <p>
 * Kết quả được render lên Cart UI để:
 * <ul>
 *   <li>Báo lý do cho từng item không hợp lệ</li>
 *   <li>Vô hiệu hóa nút Checkout nếu có bất kỳ item nào không hợp lệ</li>
 *   <li>Cho phép user bulk-remove các item không hợp lệ</li>
 * </ul>
 */
public class CartItemValidation {

    /** Item hoàn toàn hợp lệ — không có vấn đề gì. */
    public static final CartItemValidation VALID =
            new CartItemValidation(true, null, null);

    private final boolean valid;
    /** Mã lý do ngắn gọn để template switch/bind class — vd: "INACTIVE_PRODUCT". */
    private final String reasonCode;
    /** Câu thông báo thân thiện với user (tiếng Việt) — hiển thị trên UI. */
    private final String reasonMessage;

    public CartItemValidation(boolean valid, String reasonCode, String reasonMessage) {
        this.valid = valid;
        this.reasonCode = reasonCode;
        this.reasonMessage = reasonMessage;
    }

    public boolean isValid() {
        return valid;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getReasonMessage() {
        return reasonMessage;
    }

    /**
     * Mã lý do — dùng để render badge màu tương ứng trên UI.
     * Các giá trị có thể:
     * <ul>
     *   <li>{@code PRODUCT_NOT_FOUND}</li>
     *   <li>{@code INACTIVE_PRODUCT} — status != ACTIVE (DRAFT/HIDDEN/OUT_OF_STOCK)</li>
     *   <li>{@code SHOP_NOT_FOUND}</li>
     *   <li>{@code SHOP_INACTIVE}</li>
     *   <li>{@code SHOP_NOT_APPROVED}</li>
     *   <li>{@code INSUFFICIENT_STOCK}</li>
     * </ul>
     */
    public static CartItemValidation invalid(String code, String message) {
        return new CartItemValidation(false, code, message);
    }
}
