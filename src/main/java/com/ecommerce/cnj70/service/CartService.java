package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Cart;

import java.math.BigDecimal;

public interface CartService {

    Cart getCartByUserId(String userId);

    Cart addToCart(String userId, String productId, int quantity);

    Cart updateCartItem(String userId, String productId, int quantity);

    Cart removeFromCart(String userId, String productId);

    void clearCart(String userId);

    int countItems(String userId);

    BigDecimal calculateTotal(Cart cart);
}