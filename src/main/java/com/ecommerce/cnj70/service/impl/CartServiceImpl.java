package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    
    @Override
    public Cart getCartByUserId(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> createEmptyCart(userId));
    }
    
    @Override
    public Cart addToCart(String userId, String productId, int quantity) {
        Cart cart = getCartByUserId(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        
        Optional<Cart.CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();
        
        if (existingItem.isPresent()) {
            Cart.CartItem item = existingItem.get();
            int newQty = item.getQuantity() + quantity;
            int maxStock = item.getStock() != null ? item.getStock() : Integer.MAX_VALUE;
            if (newQty > maxStock) {
                newQty = maxStock;
            }
            item.setQuantity(newQty);
            item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(newQty)));
        } else {
            Cart.CartItem newItem = Cart.CartItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .imageUrl(product.getThumbnailUrl())
                    .price(product.getPrice())
                    .quantity(quantity)
                    .subtotal(product.getPrice().multiply(BigDecimal.valueOf(quantity)))
                    .shopId(product.getShopId())
                    .shopName(product.getShopName())
                    .stock(product.getStock())
                    .build();
            cart.getItems().add(newItem);
        }
        
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
    
    @Override
    public Cart updateCartItem(String userId, String productId, int quantity) {
        Cart cart = getCartByUserId(userId);
        
        if (quantity <= 0) {
            return removeFromCart(userId, productId);
        }
        
        cart.getItems().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    int maxStock = item.getStock() != null ? item.getStock() : Integer.MAX_VALUE;
                    int newQty = Math.min(quantity, maxStock);
                    item.setQuantity(newQty);
                    item.setSubtotal(item.getPrice().multiply(BigDecimal.valueOf(newQty)));
                });
        
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
    
    @Override
    public Cart removeFromCart(String userId, String productId) {
        Cart cart = getCartByUserId(userId);
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
    
    @Override
    public int countItems(String userId) {
        if (userId == null) return 0;
        return cartRepository.findByUserId(userId)
                .map(cart -> cart.getItems().stream().mapToInt(Cart.CartItem::getQuantity).sum())
                .orElse(0);
    }
    
    @Override
    public BigDecimal calculateTotal(Cart cart) {
        if (cart == null || cart.getItems() == null) return BigDecimal.ZERO;
        return cart.getItems().stream()
                .map(Cart.CartItem::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Override
    public void clearCart(String userId) {
        Cart cart = getCartByUserId(userId);
        cart.setItems(new ArrayList<>());
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }
    
    private Cart createEmptyCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
        return cartRepository.save(cart);
    }
}
