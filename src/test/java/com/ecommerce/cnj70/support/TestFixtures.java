package com.ecommerce.cnj70.support;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.security.CustomUserDetails;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test fixtures: tạo sẵn các builder cho Document và DTO phổ biến.
 * Mục đích: các test case chỉ cần gọi helper, không hardcode field nhiều lần.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ========== USER ==========

    public static User userVendor() {
        return User.builder()
                .id("user-vendor-1")
                .email("vendor1@cnj70.com")
                .password("$2a$10$encoded_password")
                .fullName("Nguyễn Văn A")
                .phone("0901234567")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .kycStatus(KycStatus.NOT_SUBMITTED)
                .shopId(null)
                .build();
    }

    public static User userVendor(String id, String email, KycStatus kycStatus, String shopId) {
        return User.builder()
                .id(id)
                .email(email)
                .password("$2a$10$encoded_password")
                .fullName("Vendor " + id)
                .phone("0900000000")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .kycStatus(kycStatus)
                .shopId(shopId)
                .build();
    }

    public static User userCustomer() {
        return User.builder()
                .id("user-customer-1")
                .email("customer1@cnj70.com")
                .password("$2a$10$encoded_password")
                .fullName("Khách Hàng A")
                .phone("0912345678")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    public static User userCustomer(String id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("$2a$10$encoded_password")
                .fullName("Customer " + id)
                .phone("0911111111")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    public static User userAdmin() {
        return User.builder()
                .id("user-admin-1")
                .email("admin@cnj70.com")
                .password("$2a$10$encoded_password")
                .fullName("Admin")
                .role(UserRole.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();
    }

    public static CustomUserDetails customUserDetails(User user) {
        return new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getFullName(),
                user.getRole() != null ? user.getRole().name() : "CUSTOMER",
                user.getStatus() != null ? user.getStatus().name() : "ACTIVE",
                user.getShopId(),
                user.getAvatarUrl()
        );
    }

    // ========== SHOP ==========

    public static Shop shop(String id, String ownerId, ShopStatus status, boolean active) {
        return Shop.builder()
                .id(id)
                .ownerId(ownerId)
                .shopName("Shop " + id)
                .description("Mô tả shop " + id)
                .logoUrl(null)
                .bannerUrl(null)
                .status(status)
                .active(active)
                .build();
    }

    public static Shop approvedActiveShop(String id, String ownerId) {
        return shop(id, ownerId, ShopStatus.APPROVED, true);
    }

    public static Shop pendingShop(String id, String ownerId) {
        return shop(id, ownerId, ShopStatus.PENDING, true);
    }

    public static Shop rejectedShop(String id, String ownerId) {
        return shop(id, ownerId, ShopStatus.REJECTED, true);
    }

    // ========== KYC PROFILE ==========

    public static KycProfile kycProfile(String id, String userId, KycStatus status) {
        return KycProfile.builder()
                .id(id)
                .userId(userId)
                .status(status)
                .ownerFullName("Nguyễn Văn Test")
                .idNumber("012345678901")
                .businessName("Công ty Test")
                .taxCode("0123456789")
                .bankAccount("1234567890")
                .bankName("Vietcombank")
                .bankBranch("Chi nhánh HCM")
                .submitCount(1)
                .build();
    }

    // ========== PRODUCT ==========

    public static Product product(String id, String shopId, int stock, ProductStatus status) {
        return Product.builder()
                .id(id)
                .shopId(shopId)
                .shopName("Shop " + shopId)
                .name("Sản phẩm " + id)
                .price(new BigDecimal("100000"))
                .stock(stock)
                .status(status)
                .sold(0)
                .rating(0.0)
                .reviewCount(0)
                .imageUrls(new ArrayList<>())
                .specifications(new ArrayList<>())
                .variants(new ArrayList<>())
                .build();
    }

    public static Product activeProduct(String id, String shopId, int stock) {
        return product(id, shopId, stock, ProductStatus.ACTIVE);
    }

    public static Product hiddenProduct(String id, String shopId, int stock) {
        return product(id, shopId, stock, ProductStatus.HIDDEN);
    }

    public static Product draftProduct(String id, String shopId, int stock) {
        return product(id, shopId, stock, ProductStatus.DRAFT);
    }

    // ========== CATEGORY ==========

    public static Category category(String id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .active(true)
                .sortOrder(1)
                .build();
    }

    // ========== CART ==========

    public static Cart.CartItem cartItem(String productId, String shopId, int qty, int stock, BigDecimal price) {
        return Cart.CartItem.builder()
                .productId(productId)
                .productName("Sản phẩm " + productId)
                .price(price)
                .quantity(qty)
                .subtotal(price.multiply(BigDecimal.valueOf(qty)))
                .shopId(shopId)
                .shopName("Shop " + shopId)
                .stock(stock)
                .build();
    }

    public static Cart emptyCart(String userId) {
        return Cart.builder()
                .id("cart-" + userId)
                .userId(userId)
                .items(new ArrayList<>())
                .build();
    }

    public static Cart cartWith(String userId, List<Cart.CartItem> items) {
        return Cart.builder()
                .id("cart-" + userId)
                .userId(userId)
                .items(new ArrayList<>(items))
                .build();
    }

    // ========== ORDER ==========

    public static Order.OrderItem orderItem(String productId, String shopId, int qty, BigDecimal price) {
        return Order.OrderItem.builder()
                .productId(productId)
                .productName("Sản phẩm " + productId)
                .price(price)
                .quantity(qty)
                .subtotal(price.multiply(BigDecimal.valueOf(qty)))
                .shopId(shopId)
                .build();
    }

    public static Order order(String id, String userId, List<Order.OrderItem> items) {
        BigDecimal subtotal = items.stream()
                .map(Order.OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shippingFee = new BigDecimal("15000");
        return Order.builder()
                .id(id)
                .userId(userId)
                .userName("Customer " + userId)
                .userEmail("customer@cnj70.com")
                .userPhone("0900000000")
                .shippingAddress("123 Nguyễn Văn A, Q1, TP.HCM")
                .items(new ArrayList<>(items))
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .totalAmount(subtotal.add(shippingFee))
                .status(OrderStatus.PENDING)
                .paid(false)
                .build();
    }
}
