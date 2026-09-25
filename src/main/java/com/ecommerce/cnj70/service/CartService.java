package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Cart;

import java.math.BigDecimal;
import java.util.Collection;

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
}