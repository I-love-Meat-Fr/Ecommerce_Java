package com.ecommerce.cnj70.dto.request;

import com.ecommerce.cnj70.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * CheckoutReq — Data contract từ Checkout sang Order.
 *
 * Được CartController / OrderController gửi xuống OrderService.createOrder().
 *
 * Quy ước (Cart → Checkout → Order data contract):
 *  - userId        : lấy từ session, KHÔNG tin dữ liệu frontend
 *  - items         : danh sách productId + quantity user muốn checkout
 *                    - null hoặc rỗng = checkout toàn bộ Cart
 *                    - không rỗng = partial checkout (TASK #14)
 *  - voucherCode   : mã voucher đã áp dụng ở Cart (nếu có)
 *                    - null/blank = không có voucher
 *  - discount      : số tiền giảm do voucher, server tự tính lại khi tạo Order
 *  - shippingAddress / phone / paymentMethod : thông tin giao hàng + thanh toán
 *
 * OrderService.createOrder() sẽ:
 *  1. Lấy Cart theo userId từ DB
 *  2. Lọc items theo danh sách productId (nếu có)
 *  3. Validate từng Product còn tồn tại + đủ stock
 *  4. Tính lại subtotal/discount/finalTotal từ server
 *  5. Tạo Order, cập nhật stock, trừ Cart items đã mua
 *  6. Voucher (nếu có): gọi VoucherService.incrementUsed() SAU khi Order tạo thành công
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutReq {

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    private String phone;

    private PaymentMethod paymentMethod;

    /**
     * Danh sách item user chọn để checkout. Null/rỗng = checkout toàn bộ Cart.
     */
    private List<CheckoutItemReq> items;

    /**
     * Voucher code đã áp dụng ở bước Cart.
     * Để null/blank nếu user không dùng voucher.
     */
    private String voucherCode;

    /**
     * Số tiền giảm do voucher (gợi ý từ Cart).
     * Server sẽ tính lại từ VoucherService để chống client gian lận.
     * Có thể null — server sẽ tự tính.
     */
    private BigDecimal discount;

    /**
     * Idempotency key do client sinh (UUID v4) — gửi kèm POST /checkout để
     * chống double-click / double-submit / network retry tạo 2 Order trùng.
     *
     * <p>Flow:</p>
     * <ul>
     *   <li>JS trong {@code checkout.html} generate UUID ngay khi user mở trang,
     *       lưu vào {@code sessionStorage}, gán vào hidden field của form.</li>
     *   <li>Server wrap {@code OrderService.createOrder} với
     *       {@code CheckoutIdempotencyService.tryReserve} — race-condition
     *       được giải quyết bằng compound unique index trên MongoDB.</li>
     *   <li>Nếu cùng key + body → trả Order cũ (idempotent replay).</li>
     *   <li>Nếu cùng key + body khác → 409 Conflict.</li>
     *   <li>Nếu null/blank → server fallback sang key dựa trên
     *       (userId, cartSnapshot, phút hiện tại) — defense-in-depth.</li>
     * </ul>
     *
     * <p>KHÔNG validate {@code @NotBlank} — cho phép null để client cũ / JS
     * disabled vẫn hoạt động (server sẽ tự sinh fallback).</p>
     */
    private String idempotencyKey;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutItemReq {
        private String productId;
        private int quantity;
    }
}
