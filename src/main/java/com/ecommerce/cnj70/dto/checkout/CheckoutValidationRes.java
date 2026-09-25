package com.ecommerce.cnj70.dto.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Response trả về cho Checkout UI khi gọi {@code POST /api/checkout/validate}.
 *
 * <p>Mục đích: trước khi user bấm "Đặt hàng", server kiểm tra lại toàn bộ
 * dữ liệu (price, stock, shop, voucher) dựa trên DB hiện tại. UI sử dụng
 * response này để:</p>
 *
 * <ul>
 *   <li>Hiển thị cảnh báo nếu giá/stock/shop/voucher đã thay đổi</li>
 *   <li>Auto-refresh UI bằng dữ liệu mới (price, summary, voucher)</li>
 *   <li>Chặn submit nếu có bất kỳ lỗi nghiêm trọng nào (stock=0, shop suspended, voucher hết hạn)</li>
 *   <li>Cho phép submit nếu chỉ có cảnh báo "giá đã tăng" — vì user vẫn có thể mua ở giá mới</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutValidationRes {

    /**
     * true = có thể submit CheckoutReq; false = có lỗi nghiêm trọng, không được submit.
     */
    private boolean valid;

    /**
     * true = phát hiện thay đổi (price/stock/shop status/voucher), UI cần refresh.
     */
    private boolean changed;

    /**
     * Per-item snapshot từ DB hiện tại — UI dùng để vẽ lại giá/line items.
     */
    @Builder.Default
    private List<ValidatedItem> items = new ArrayList<>();

    /**
     * Lỗi nghiêm trọng — chặn submit (stock=0, shop suspended, product removed…).
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Cảnh báo — KHÔNG chặn submit, nhưng UI nên highlight (giá tăng, voucher sắp hết lượt).
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Tổng kết tiền dựa trên giá mới nhất từ DB.
     */
    private Summary summary;

    /**
     * Trạng thái voucher hiện tại (nếu có).
     */
    private VoucherState voucher;

    /**
     * Per-item validation snapshot.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidatedItem {
        private String productId;
        private String productName;
        private String imageUrl;
        private String shopId;
        private String shopName;

        /** Giá user đang thấy trong Cart (snapshot cũ). */
        private BigDecimal oldPrice;
        /** Giá hiện tại trong DB. */
        private BigDecimal currentPrice;
        /** true = oldPrice != currentPrice. */
        private boolean priceChanged;

        /** Số lượng user muốn mua (= cartItem.quantity). */
        private int requestedQuantity;
        /** Stock hiện tại trong DB. */
        private int currentStock;
        /** true = stock >= requestedQuantity. */
        private boolean stockOk;

        /** Trạng thái Product — ACTIVE / HIDDEN / OUT_OF_STOCK / null nếu product đã xóa. */
        private String productStatus;
        /** true = shop còn active & APPROVED. */
        private boolean shopActive;
        /** Tên shop hiện tại (có thể đã đổi so với cart snapshot). */
        private String shopCurrentName;

        /** Subtotal theo currentPrice × requestedQuantity. */
        private BigDecimal subtotal;

        /** true = item này hoàn toàn OK để checkout. */
        private boolean valid;

        /** Lý do không hợp lệ (vd: "OUT_OF_STOCK", "SHOP_SUSPENDED"). */
        private String reasonCode;
        /** Câu thông báo tiếng Việt — UI hiển thị cho user. */
        private String reasonMessage;
    }

    /**
     * Tổng tiền — tất cả đều do server tính dựa trên currentPrice.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private BigDecimal discount;
        private BigDecimal finalTotal;
        /** Số tiền giảm so với tổng cũ (nếu có thay đổi). Dương = user tiết kiệm hơn. */
        private BigDecimal deltaFromPrevious;
    }

    /**
     * Trạng thái voucher đang được áp dụng (từ session hoặc từ CheckoutReq.voucherCode).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoucherState {
        private String code;
        private String voucherId;
        private String voucherName;
        private boolean valid;
        /** BigDecimal.ZERO nếu invalid. */
        private BigDecimal discount;
        /** Lý do không hợp lệ (vd: "EXPIRED", "EXHAUSTED", "MIN_ORDER_NOT_MET"). */
        private String reasonCode;
        /** Câu thông báo tiếng Việt. */
        private String reasonMessage;
    }
}
