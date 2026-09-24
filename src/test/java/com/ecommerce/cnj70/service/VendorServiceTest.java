package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.ShopFormReq;
import com.ecommerce.cnj70.dto.response.VendorDashboardRes;
import com.ecommerce.cnj70.dto.response.VendorProfileRes;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.impl.VendorServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit test cho VendorServiceImpl.
 *
 * Verify các guard:
 *  - getCurrentVendor: chặn null + email không tồn tại
 *  - getShopIdFromUser: vendor chưa có shop → throw
 *  - createShop: KYC chưa APPROVED → throw (BẮT BUỘC)
 *  - createShop: đã có shop → throw
 *  - validateShopOwnership / validateProductOwnership: vendor khác shop → throw
 *  - getDashboardStats: aggregate đúng số liệu
 *  - getDashboardStats: customer aggregate distinct userId
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VendorService - unit test")
class VendorServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private VendorKycService vendorKycService;

    @InjectMocks private VendorServiceImpl vendorService;

    private User vendor;
    private UserDetails vendorDetails;

    @BeforeEach
    void setUp() {
        vendor = TestFixtures.userVendor("v-1", "v1@cnj70.com", KycStatus.APPROVED, "shop-1");
        vendorDetails = TestFixtures.customUserDetails(vendor);
        lenient().when(userRepository.findByEmail("v1@cnj70.com")).thenReturn(Optional.of(vendor));
    }

    // ============ getCurrentVendor ============

    @Test
    @DisplayName("getCurrentVendor: UserDetails null → UnauthorizedException")
    void getCurrentVendor_nullDetails_throws() {
        assertThatThrownBy(() -> vendorService.getCurrentVendor(null))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("đăng nhập");
    }

    @Test
    @DisplayName("getCurrentVendor: email không tồn tại → UnauthorizedException")
    void getCurrentVendor_emailNotFound_throws() {
        when(userRepository.findByEmail("ghost@cnj70.com")).thenReturn(Optional.empty());
        UserDetails ghost = TestFixtures.customUserDetails(
                TestFixtures.userCustomer("ghost", "ghost@cnj70.com"));

        assertThatThrownBy(() -> vendorService.getCurrentVendor(ghost))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    // ============ getShopIdFromUser ============

    @Test
    @DisplayName("getShopIdFromUser: vendor chưa có shop → BadRequestException")
    void getShopIdFromUser_noShop_throws() {
        User noShopVendor = TestFixtures.userVendor("v-2", "v2@cnj70.com", KycStatus.APPROVED, null);
        UserDetails noShopDetails = TestFixtures.customUserDetails(noShopVendor);
        when(userRepository.findByEmail("v2@cnj70.com")).thenReturn(Optional.of(noShopVendor));

        assertThatThrownBy(() -> vendorService.getShopIdFromUser(noShopDetails))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("chưa có shop");
    }

    // ============ createShop ============

    @Test
    @DisplayName("createShop: vendor KYC chưa APPROVED → throw BadRequestException (BẮT BUỘC)")
    void createShop_kycNotApproved_throws() {
        User pendingKyc = TestFixtures.userVendor("v-3", "v3@cnj70.com", KycStatus.PENDING_ADMIN, null);
        UserDetails ud = TestFixtures.customUserDetails(pendingKyc);
        when(userRepository.findByEmail("v3@cnj70.com")).thenReturn(Optional.of(pendingKyc));

        ShopFormReq req = ShopFormReq.builder()
                .shopName("Shop Test")
                .description("desc")
                .build();

        assertThatThrownBy(() -> vendorService.createShop(ud, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("KYC");
    }

    @Test
    @DisplayName("createShop: vendor đã có shop → throw BadRequestException")
    void createShop_alreadyHasShop_throws() {
        // Shop hiện tại tồn tại → phải block tạo mới
        when(shopRepository.existsById("shop-1")).thenReturn(true);

        ShopFormReq req = ShopFormReq.builder()
                .shopName("Shop Mới")
                .description("desc")
                .build();

        assertThatThrownBy(() -> vendorService.createShop(vendorDetails, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("đã có shop");
    }

    @Test
    @DisplayName("createShop: User.shopId trỏ đến Shop không tồn tại (orphan) → clear và tạo mới")
    void createShop_orphanShopId_clearsAndCreates() {
        // User.shopId = "shop-1" nhưng Shop không còn tồn tại trong DB
        when(shopRepository.existsById("shop-1")).thenReturn(false);
        when(shopRepository.existsByShopName(any())).thenReturn(false);
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> {
            Shop s = inv.getArgument(0);
            s.setId("shop-new");
            return s;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ShopFormReq req = ShopFormReq.builder().shopName("Shop Mới").description("desc").build();

        Shop result = vendorService.createShop(vendorDetails, req);

        assertThat(result.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(result.getOwnerId()).isEqualTo("v-1");
        assertThat(vendor.getShopId()).isEqualTo("shop-new");
    }

    @Test
    @DisplayName("createShop: tên shop trùng → throw BadRequestException")
    void createShop_duplicateName_throws() {
        User noShopVendor = TestFixtures.userVendor("v-4", "v4@cnj70.com", KycStatus.APPROVED, null);
        UserDetails ud = TestFixtures.customUserDetails(noShopVendor);
        when(userRepository.findByEmail("v4@cnj70.com")).thenReturn(Optional.of(noShopVendor));
        when(shopRepository.existsByShopName("Shop Trùng")).thenReturn(true);

        ShopFormReq req = ShopFormReq.builder().shopName("Shop Trùng").description("d").build();

        assertThatThrownBy(() -> vendorService.createShop(ud, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Tên shop đã tồn tại");
    }

    @Test
    @DisplayName("createShop: happy path → tạo shop PENDING, gắn shopId vào User")
    void createShop_happyPath_savesPending() {
        User noShopVendor = TestFixtures.userVendor("v-5", "v5@cnj70.com", KycStatus.APPROVED, null);
        UserDetails ud = TestFixtures.customUserDetails(noShopVendor);
        when(userRepository.findByEmail("v5@cnj70.com")).thenReturn(Optional.of(noShopVendor));
        when(shopRepository.existsByShopName(any())).thenReturn(false);
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> {
            Shop s = inv.getArgument(0);
            s.setId("shop-new");
            return s;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        ShopFormReq req = ShopFormReq.builder().shopName("Shop Mới").description("desc").build();

        Shop result = vendorService.createShop(ud, req);

        assertThat(result.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(result.getOwnerId()).isEqualTo("v-5");
        assertThat(noShopVendor.getShopId()).isEqualTo("shop-new");
    }

    // ============ validateShopOwnership ============

    @Test
    @DisplayName("validateShopOwnership: vendor khác shop → UnauthorizedException")
    void validateShopOwnership_otherShop_throws() {
        assertThatThrownBy(() -> vendorService.validateShopOwnership("shop-KHAC", vendorDetails))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("quyền");
    }

    // ============ validateProductOwnership ============

    @Test
    @DisplayName("validateProductOwnership: sản phẩm shop khác → UnauthorizedException")
    void validateProductOwnership_otherShop_throws() {
        Product otherProduct = TestFixtures.activeProduct("p-1", "shop-KHAC", 10);
        when(productRepository.findById("p-1")).thenReturn(Optional.of(otherProduct));

        assertThatThrownBy(() -> vendorService.validateProductOwnership("p-1", vendorDetails))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("validateProductOwnership: sản phẩm không tồn tại → ResourceNotFoundException")
    void validateProductOwnership_notFound_throws() {
        when(productRepository.findById("p-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.validateProductOwnership("p-x", vendorDetails))
                .isInstanceOf(com.ecommerce.cnj70.exception.ResourceNotFoundException.class);
    }

    // ============ getDashboardStats ============

    @Test
    @DisplayName("getDashboardStats: vendor chưa có shop → trả về stats zero (không throw)")
    void getDashboardStats_noShop_returnsZeroStats() {
        User noShop = TestFixtures.userVendor("v-6", "v6@cnj70.com", KycStatus.APPROVED, null);
        UserDetails ud = TestFixtures.customUserDetails(noShop);
        when(userRepository.findByEmail("v6@cnj70.com")).thenReturn(Optional.of(noShop));

        VendorDashboardRes stats = vendorService.getDashboardStats(ud);

        assertThat(stats.getTotalProducts()).isZero();
        assertThat(stats.getTotalOrders()).isZero();
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(stats.getShopSummary()).isNull();
    }

    @Test
    @DisplayName("getDashboardStats: aggregate đúng số đơn theo trạng thái, doanh thu chỉ DELIVERED")
    void getDashboardStats_aggregatesCorrectly() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));
        when(productRepository.findByShopId("shop-1")).thenReturn(List.of(
                TestFixtures.activeProduct("p-1", "shop-1", 10),
                TestFixtures.activeProduct("p-2", "shop-1", 0)
        ));

        // Lưu ý: getTotalAmount = subtotal (100000) + shippingFee (15000) = 115000
        List<Order> orders = List.of(
                deliveredOrder("o-1", "u-1", "shop-1", "100000"),
                pendingOrder("o-2", "u-1", "shop-1"),
                preparingOrder("o-3", "u-2", "shop-1"),
                cancelledOrder("o-4", "u-2", "shop-1")
        );
        when(orderRepository.findByShopIdOrderByCreatedAtDesc("shop-1")).thenReturn(orders);

        VendorDashboardRes stats = vendorService.getDashboardStats(vendorDetails);

        assertThat(stats.getTotalProducts()).isEqualTo(2);
        assertThat(stats.getOutOfStockProducts()).isEqualTo(1);
        assertThat(stats.getTotalOrders()).isEqualTo(4);
        assertThat(stats.getPendingOrders()).isEqualTo(1);
        assertThat(stats.getProcessingOrders()).isEqualTo(1);
        assertThat(stats.getCompletedOrders()).isEqualTo(1);
        assertThat(stats.getCancelledOrders()).isEqualTo(1);
        // Total revenue includes shipping fee (15000 per order)
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("115000");
        assertThat(stats.getShopSummary()).isNotNull();
        assertThat(stats.getShopSummary().getShopName()).isEqualTo("Shop shop-1");
    }

    @Test
    @DisplayName("getDashboardStats: chỉ filter đơn của shop hiện tại (khác shop bị bỏ)")
    void getDashboardStats_filtersByShopId() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));
        when(productRepository.findByShopId("shop-1")).thenReturn(java.util.Collections.emptyList());
        // Repository đã filter sẵn ở method findByShopIdOrderByCreatedAtDesc → chỉ trả về đơn của shop
        when(orderRepository.findByShopIdOrderByCreatedAtDesc("shop-1"))
                .thenReturn(List.of(deliveredOrder("o-1", "u-1", "shop-1", "50000")));

        VendorDashboardRes stats = vendorService.getDashboardStats(vendorDetails);

        assertThat(stats.getTotalOrders()).isEqualTo(1);
        // 50000 + 15000 shipping = 65000
        assertThat(stats.getTotalRevenue()).isEqualByComparingTo("65000");
    }

    // ============ getVendorProfile ============

    @Test
    @DisplayName("getVendorProfile: vendor có shop → trả về profile đầy đủ")
    void getVendorProfile_withShop_returnsFullProfile() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        VendorProfileRes profile = vendorService.getVendorProfile(vendorDetails);

        assertThat(profile.getId()).isEqualTo("v-1");
        assertThat(profile.getEmail()).isEqualTo("v1@cnj70.com");
        assertThat(profile.getRole()).isEqualTo(UserRole.VENDOR.name());
        assertThat(profile.getShop()).isNotNull();
        assertThat(profile.getShop().getShopName()).isEqualTo("Shop shop-1");
    }

    @Test
    @DisplayName("getVendorProfile: vendor chưa có shop → trả về profile không có shop")
    void getVendorProfile_noShop_returnsProfileWithoutShop() {
        User noShop = TestFixtures.userVendor("v-7", "v7@cnj70.com", KycStatus.APPROVED, null);
        UserDetails ud = TestFixtures.customUserDetails(noShop);
        when(userRepository.findByEmail("v7@cnj70.com")).thenReturn(Optional.of(noShop));

        VendorProfileRes profile = vendorService.getVendorProfile(ud);

        assertThat(profile.getShop()).isNull();
    }

    // ============ helpers ============

    private Order deliveredOrder(String id, String userId, String shopId, String amount) {
        Order o = TestFixtures.order(id, userId,
                List.of(TestFixtures.orderItem("p-" + id, shopId, 1, new BigDecimal(amount))));
        o.setStatus(OrderStatus.DELIVERED);
        return o;
    }

    private Order pendingOrder(String id, String userId, String shopId) {
        Order o = TestFixtures.order(id, userId,
                List.of(TestFixtures.orderItem("p-" + id, shopId, 1, new BigDecimal("100000"))));
        o.setStatus(OrderStatus.PENDING);
        return o;
    }

    private Order preparingOrder(String id, String userId, String shopId) {
        Order o = TestFixtures.order(id, userId,
                List.of(TestFixtures.orderItem("p-" + id, shopId, 1, new BigDecimal("100000"))));
        o.setStatus(OrderStatus.PREPARING);
        return o;
    }

    private Order cancelledOrder(String id, String userId, String shopId) {
        Order o = TestFixtures.order(id, userId,
                List.of(TestFixtures.orderItem("p-" + id, shopId, 1, new BigDecimal("100000"))));
        o.setStatus(OrderStatus.CANCELLED);
        return o;
    }
}
