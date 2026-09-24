package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.dto.cart.CartItemValidation;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    
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
            String imageUrl = normalizeImageUrl(product.getThumbnailUrl());
            Cart.CartItem newItem = Cart.CartItem.builder()
                    .productId(productId)
                    .productName(product.getName())
                    .imageUrl(imageUrl)
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

    /**
     * TASK #14 — Partial checkout cleanup.
     * Xóa chỉ những CartItem có productId nằm trong {@code productIds}, giữ nguyên các item còn lại.
     * - productIds null hoặc rỗng → không xóa gì, trả Cart hiện tại.
     * - Tự save và cập nhật updatedAt.
     */
    @Override
    public Cart removeItems(String userId, Collection<String> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return getCartByUserId(userId);
        }
        Cart cart = getCartByUserId(userId);
        cart.getItems().removeIf(item -> productIds.contains(item.getProductId()));
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
    
    private Cart createEmptyCart(String userId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .items(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
        return cartRepository.save(cart);
    }

    /**
     * Normalizes an image URL to ensure it has the correct path prefix.
     * Handles null, empty, bare filenames, and full URLs uniformly.
     */
    private String normalizeImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        String trimmed = imageUrl.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("/")) {
            return trimmed;
        }
        // Bare filename — prepend /uploads/
        return "/uploads/" + trimmed;
    }

    @Override
    public Map<String, CartItemValidation> validateCartItems(Cart cart) {
        Map<String, CartItemValidation> invalid = new LinkedHashMap<>();
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return invalid;
        }

        for (Cart.CartItem item : cart.getItems()) {
            // 1) Product tồn tại?
            Optional<Product> productOpt = productRepository.findById(item.getProductId());
            if (productOpt.isEmpty()) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "PRODUCT_NOT_FOUND",
                        "Sản phẩm đã ngừng bán hoặc bị xóa khỏi hệ thống."));
                continue;
            }
            Product product = productOpt.get();

            // 2) Product phải ACTIVE
            ProductStatus status = product.getStatus();
            if (status == null || status != ProductStatus.ACTIVE) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "INACTIVE_PRODUCT",
                        String.format(
                                "Sản phẩm không còn khả dụng (trạng thái: %s). Vui lòng xóa khỏi giỏ hàng.",
                                status == null ? "Không xác định" : status)));
                continue;
            }

            // 3) Shop tồn tại & active & APPROVED
            if (product.getShopId() == null) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "SHOP_NOT_FOUND",
                        "Không xác định được cửa hàng bán sản phẩm này."));
                continue;
            }
            Optional<Shop> shopOpt = shopRepository.findById(product.getShopId());
            if (shopOpt.isEmpty()) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "SHOP_NOT_FOUND",
                        "Cửa hàng đã ngừng hoạt động hoặc bị xóa."));
                continue;
            }
            Shop shop = shopOpt.get();
            if (!shop.isActive()) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "SHOP_INACTIVE",
                        String.format("Cửa hàng '%s' hiện không hoạt động.", shop.getShopName())));
                continue;
            }
            ShopStatus shopStatus = shop.getStatus();
            if (shopStatus == ShopStatus.SUSPENDED) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "SHOP_SUSPENDED",
                        String.format("Cửa hàng '%s' đang bị tạm ngưng.", shop.getShopName())));
                continue;
            }
            if (shopStatus != ShopStatus.APPROVED) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "SHOP_NOT_APPROVED",
                        String.format("Cửa hàng '%s' chưa được xác minh để bán hàng.",
                                shop.getShopName())));
                continue;
            }

            // 4) Stock đủ?
            int stock = product.getStock();
            int requested = item.getQuantity();
            if (requested <= 0) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "INVALID_QUANTITY",
                        "Số lượng không hợp lệ."));
                continue;
            }
            if (stock <= 0) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "OUT_OF_STOCK",
                        "Sản phẩm đã hết hàng."));
                continue;
            }
            if (stock < requested) {
                invalid.put(item.getProductId(), CartItemValidation.invalid(
                        "INSUFFICIENT_STOCK",
                        String.format("Chỉ còn %d sản phẩm trong kho (giỏ hàng: %d).",
                                stock, requested)));
                continue;
            }
        }
        return invalid;
    }

    @Override
    public Cart removeInvalidItems(String userId) {
        Cart cart = getCartByUserId(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return cart;
        }
        Map<String, CartItemValidation> invalid = validateCartItems(cart);
        if (invalid.isEmpty()) {
            return cart;
        }
        cart.getItems().removeIf(item -> invalid.containsKey(item.getProductId()));
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeByShop(String userId, String shopId) {
        Cart cart = getCartByUserId(userId);
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            return cart;
        }
        // Nếu shopId null -> xóa các item không có shopId; ngược lại xóa đúng shopId.
        cart.getItems().removeIf(item -> {
            String itemShopId = item.getShopId();
            if (shopId == null) {
                return itemShopId == null;
            }
            return shopId.equals(itemShopId);
        });
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepository.save(cart);
    }
}
