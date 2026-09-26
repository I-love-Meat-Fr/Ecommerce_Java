package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.dto.cart.CartItemValidation;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

public interface CartService {

    Cart getCartByUserId(String userId);

    Cart addToCart(String userId, String productId, int quantity);

    Cart updateCartItem(String userId, String productId, int quantity);

    Cart removeFromCart(String userId, String productId);

    /**
     * Persist applied voucher code on Cart (MongoDB).
     * @param userId  current user
     * @param code    voucher code (non-null, non-blank), hoặc null để clear
     * @return Cart đã được save với appliedVoucherCode
     */
    Cart setAppliedVoucherCode(String userId, String code);

    /**
     * Lấy applied voucher code (có thể null) từ Cart hiện tại của user.
     */
    String getAppliedVoucherCode(String userId);

    /**
     * Remove một tập productIds khỏi Cart.
     * Dùng cho TASK #14 — partial checkout: chỉ xóa items user đã mua,
     * giữ lại các item còn lại trong Cart.
     *
     * @param userId      user hiện tại
     * @param productIds  tập productId cần xóa (null hoặc rỗng = không xóa gì)
     * @return Cart sau khi xóa
     */
    Cart removeItems(String userId, Collection<String> productIds);

    void clearCart(String userId);

    int countItems(String userId);

    BigDecimal calculateTotal(Cart cart);

    /**
     * Kiểm tra real-time trạng thái của từng item trong Cart dựa trên Product/Shop hiện tại.
     * <p>
     * Trả về map {@code productId -> CartItemValidation} (chỉ chứa các item KHÔNG hợp lệ).
     * Item hợp lệ sẽ không xuất hiện trong map (dùng {@link CartItemValidation#VALID}
     * làm sentinel để thêm vào kết quả nếu cần).
     *
     * <p><b>Quy tắc không hợp lệ:</b>
     * <ul>
     *   <li>Product bị xóa khỏi DB</li>
     *   <li>Product.status != ACTIVE (DRAFT/HIDDEN/OUT_OF_STOCK)</li>
     *   <li>Shop bị xóa / không active / chưa APPROVED / SUSPENDED</li>
     *   <li>Product.stock < cartItem.quantity</li>
     * </ul>
     *
     * @param cart Cart hiện tại của user
     * @return Map productId → lý do không hợp lệ (chỉ chứa các item invalid)
     */
    Map<String, CartItemValidation> validateCartItems(Cart cart);

    /**
     * Xóa tất cả CartItem không còn hợp lệ (dựa trên {@link #validateCartItems(Cart)}).
     * Dùng khi user bấm "Xóa sản phẩm không hợp lệ" trên Cart UI.
     *
     * @param userId user hiện tại
     * @return Cart sau khi đã loại bỏ invalid items
     */
    Cart removeInvalidItems(String userId);

    /**
     * Xóa tất cả CartItem thuộc về một shop cụ thể.
     * Dùng khi user bấm "Xóa tất cả sản phẩm của shop này" trên header shop-group.
     *
     * @param userId  user hiện tại
     * @param shopId  shopId cần xóa (null = các item không có shopId)
     * @return Cart sau khi đã loại bỏ items của shop
     */
    Cart removeByShop(String userId, String shopId);
}