package com.ecommerce.cnj70.support;

import com.ecommerce.cnj70.document.Cart;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * TestFixtures — Centralized factory cho unit test (mockito).
 *
 * <p>Tất cả test trong package {@code com.ecommerce.cnj70} dùng chung factory này để
 * tạo domain object với dữ liệu nhất quán. Tránh việc mỗi test phải tự khởi tạo
 * {@code User.builder()...} dài dòng.</p>
 *
 * <p>Các method public tương ứng với use-case test:</p>
 * <ul>
 *   <li>{@link #userVendor}, {@link #userAdmin}, {@link #userCustomer}</li>
 *   <li>{@link #customUserDetails} — wrap User thành Spring Security principal</li>
 *   <li>{@link #emptyCart}, {@link #cartWith}, {@link #cartItem}</li>
 *   <li>{@link #activeProduct}, {@link #hiddenProduct}, {@link #draftProduct}, {@link #product}</li>
 *   <li>{@link #shop}, {@link #pendingShop}</li>
 *   <li>{@link #kycProfile}</li>
 *   <li>{@link #order}, {@link #orderItem}</li>
 * </ul>
 */
public final class TestFixtures {

    private TestFixtures() {
        // Utility class — không cho phép khởi tạo.
    }

    // ============================================================
    // User fixtures
    // ============================================================

    /**
     * Tạo vendor user với id, email, kycStatus, shopId tuỳ ý.
     *
     * @param id        user id (ví dụ "v-1")
     * @param email     email (ví dụ "v1@cnj70.com")
     * @param kycStatus trạng thái KYC (NOT_SUBMITTED / PENDING_ADMIN / APPROVED ...)
     * @param shopId    id shop đã gắn với vendor, null nếu chưa tạo shop
     */
    public static User userVendor(String id, String email, KycStatus kycStatus, String shopId) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded-password")
                .fullName("Vendor " + id)
                .phone("0900000000")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .kycStatus(kycStatus == null ? KycStatus.NOT_SUBMITTED : kycStatus)
                .shopId(shopId)
                .build();
    }

    /**
     * Admin user mặc định — email "admin@cnj70.com", id "u-admin-1".
     */
    public static User userAdmin() {
        return User.builder()
                .id("u-admin-1")
                .email("admin@cnj70.com")
                .password("encoded-password")
                .fullName("Admin CNJ70")
                .phone("0900000001")
                .role(UserRole.ADMIN)
                .status(AccountStatus.ACTIVE)
                .kycStatus(KycStatus.NOT_SUBMITTED)
                .build();
    }

    /**
     * Customer user mặc định — email "customer1@cnj70.com", id "u-customer-1".
     */
    public static User userCustomer() {
        return userCustomer("u-customer-1", "customer1@cnj70.com");
    }

    /**
     * Customer user với id/email tuỳ biên.
     */
    public static User userCustomer(String id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .password("encoded-password")
                .fullName("Customer " + id)
                .phone("0900000002")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .kycStatus(KycStatus.NOT_SUBMITTED)
                .build();
    }

    /**
     * Wrap {@link User} thành {@link CustomUserDetails} để truyền vào service như auth principal.
     */
    public static CustomUserDetails customUserDetails(User user) {
        return CustomUserDetails.fromUser(user);
    }

    // ============================================================
    // Cart fixtures
    // ============================================================

    /**
     * Cart rỗng (chưa có item nào).
     */
    public static Cart emptyCart(String userId) {
        return Cart.builder()
                .id("cart-" + userId)
                .userId(userId)
                .items(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Cart với danh sách item cho trước.
     */
    public static Cart cartWith(String userId, List<Cart.CartItem> items) {
        return Cart.builder()
                .id("cart-" + userId)
                .userId(userId)
                .items(items == null ? new ArrayList<>() : new ArrayList<>(items))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * CartItem với productId, shopId, quantity, stock snapshot, price.
     */
    public static Cart.CartItem cartItem(String productId, String shopId, int quantity,
                                        int stock, BigDecimal price) {
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
        return Cart.CartItem.builder()
                .productId(productId)
                .productName("Product " + productId)
                .imageUrl(null)
                .price(price)
                .quantity(quantity)
                .subtotal(subtotal)
                .shopId(shopId)
                .shopName("Shop " + shopId)
                .stock(stock)
                .build();
    }

    // ============================================================
    // Product fixtures
    // ============================================================

    /**
     * Product ACTIVE — đang bán, hiển thị trên storefront.
     */
    public static Product activeProduct(String id, String shopId, int stock) {
        return product(id, shopId, stock, ProductStatus.ACTIVE);
    }

    /**
     * Product HIDDEN — bị ẩn khỏi storefront (do admin hide hoặc vi phạm).
     */
    public static Product hiddenProduct(String id, String shopId, int stock) {
        return product(id, shopId, stock, ProductStatus.HIDDEN);
    }

    /**
     * Product DRAFT — vendor mới tạo, chưa submit duyệt.
     */
    public static Product draftProduct(String id, String shopId, int stock) {
        return product(id, shopId, stock, ProductStatus.DRAFT);
    }

    /**
     * Product với status tuỳ biên.
     */
    public static Product product(String id, String shopId, int stock, ProductStatus status) {
        return Product.builder()
                .id(id)
                .shopId(shopId)
                .shopName("Shop " + shopId)
                .name("Product " + id)
                .description("Mô tả sản phẩm " + id)
                .price(new BigDecimal("100000"))
                .stock(stock)
                .categoryId("cat-1")
                .categoryName("Default Category")
                .status(status)
                .build();
    }

    // ============================================================
    // Shop fixtures
    // ============================================================

    /**
     * Shop với id, ownerId, status, active tuỳ biên.
     */
    public static Shop shop(String id, String ownerId, ShopStatus status, boolean active) {
        return Shop.builder()
                .id(id)
                .ownerId(ownerId)
                .shopName("Shop " + id)
                .description("Mô tả shop " + id)
                .status(status)
                .active(active)
                .kycStatus(KycStatus.APPROVED)
                .build();
    }

    /**
     * Shop PENDING — mới tạo, đang chờ admin duyệt.
     */
    public static Shop pendingShop(String id, String ownerId) {
        return shop(id, ownerId, ShopStatus.PENDING, true);
    }

    // ============================================================
    // KycProfile fixtures
    // ============================================================

    /**
     * KycProfile với id, userId, status tuỳ biên.
     */
    public static KycProfile kycProfile(String id, String userId, KycStatus status) {
        return KycProfile.builder()
                .id(id)
                .userId(userId)
                .status(status == null ? KycStatus.NOT_SUBMITTED : status)
                .ownerFullName("Nguyễn Văn Test")
                .idNumber("012345678901")
                .bankAccount("1234567890")
                .bankName("Vietcombank")
                .submittedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    // ============================================================
    // Order fixtures
    // ============================================================

    /**
     * Order với id, userId, items tuỳ biên.
     * Status mặc định = PENDING.
     */
    public static Order order(String id, String userId, List<Order.OrderItem> items) {
        return Order.builder()
                .id(id)
                .userId(userId)
                .userName("Customer " + userId)
                .userEmail(userId + "@cnj70.com")
                .userPhone("0900000002")
                .shippingAddress("123 Test Street, District 1, HCMC")
                .items(items == null ? new ArrayList<>() : new ArrayList<>(items))
                .subtotal(BigDecimal.ZERO)
                .shippingFee(new BigDecimal("15000"))
                .totalAmount(new BigDecimal("15000"))
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * OrderItem với productId, shopId, quantity, price.
     * Subtotal được tính = price * quantity.
     */
    public static Order.OrderItem orderItem(String productId, String shopId,
                                            int quantity, BigDecimal price) {
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
        return Order.OrderItem.builder()
                .productId(productId)
                .productName("Product " + productId)
                .shopId(shopId)
                .shopName("Shop " + shopId)
                .price(price)
                .quantity(quantity)
                .subtotal(subtotal)
                .build();
    }
}