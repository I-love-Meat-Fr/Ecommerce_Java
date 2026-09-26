package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.dto.checkout.CheckoutValidationRes;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ShippingStatus;

import java.util.List;

public interface OrderService {

    Order createOrder(String userId, CheckoutReq request);

    /**
     * Pre-submit validation: kiểm tra lại toàn bộ trạng thái (price, stock, shop, voucher)
     * dựa trên DB hiện tại. UI gọi endpoint này ngay trước khi gửi CheckoutReq để:
     * <ul>
     *   <li>Phát hiện price/stock/shop/voucher đã đổi từ lúc user mở trang</li>
     *   <li>Cập nhật UI với dữ liệu mới nhất từ server</li>
     *   <li>Chặn submit nếu có lỗi nghiêm trọng (stock=0, shop bị suspend, voucher hết hạn)</li>
     * </ul>
     *
     * <p>Method này KHÔNG tạo Order, KHÔNG trừ stock, KHÔNG tăng voucher used.
     * Chỉ đọc DB và trả về snapshot.</p>
     *
     * @param userId       user hiện tại
     * @param voucherCode  voucherCode từ session/CheckoutReq (null nếu không có voucher)
     * @return snapshot với valid/changed/errors/warnings và fresh data
     */
    CheckoutValidationRes validateCheckout(String userId, String voucherCode);

    Order getOrderById(String id);

    /**
     * Ownership-checked fetch. Returns the order only when it belongs to the given
     * customerId; throws otherwise (defense against IDOR).
     */
    Order getOrderByIdForCustomer(String orderId, String customerId);

    List<Order> getOrdersByUserId(String userId);

    List<Order> getOrdersByShopId(String shopId);

    void updateOrderStatus(String orderId, OrderStatus status);

    void cancelOrder(String orderId);

    /**
     * Cập nhật trạng thái vận chuyển cho phần của shop trong Order.
     * Mỗi shop có trạng thái vận chuyển riêng trong {@code Order.shippingByShop}.
     *
     * @param orderId        id của Order
     * @param shopId         shop cập nhật (key trong shippingByShop)
     * @param status         trạng thái vận chuyển mới
     * @param trackingNumber số vận đơn (optional)
     * @param carrier        đơn vị vận chuyển (optional)
     * @param note           ghi chú (optional)
     * @return Order sau khi cập nhật
     */
    Order updateShippingStatus(String orderId, String shopId, ShippingStatus status,
                               String trackingNumber, String carrier, String note);

    /**
     * TASK #14/#20/#21 — Kiểm tra Customer đã mua và ĐÃ NHẬN được Product chưa.
     * Điều kiện: Order có productId của user VÀ trạng thái DELIVERED.
     * Chỉ khi đã giao hàng thành công (DELIVERED) mới cho phép Review.
     *
     * @return true nếu Order đã DELIVERED và chứa productId
     */
    boolean hasUserReceivedProduct(String userId, String productId);

    /**
     * TASK #21 (legacy) — Kiểm tra Customer đã mua Product chưa (không tính CANCELLED).
     * Dùng để hiển thị trạng thái mua hàng (chưa giao = vẫn hiển thị "đã mua").
     * KHÔNG dùng cho Review validation — dùng hasUserReceivedProduct thay thế.
     *
     * @return true nếu Order không CANCELLED và chứa productId
     */
    boolean hasUserPurchasedProduct(String userId, String productId);
}
