package com.ecommerce.cnj70.flow;

import com.ecommerce.cnj70.document.Cart;
import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.request.CheckoutReq;
import com.ecommerce.cnj70.dto.request.KycFormReq;
import com.ecommerce.cnj70.enums.KycStatus;
import com.ecommerce.cnj70.enums.OrderStatus;
import com.ecommerce.cnj70.enums.PaymentMethod;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.repository.CartRepository;
import com.ecommerce.cnj70.repository.KycProfileRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AdminKycService;
import com.ecommerce.cnj70.service.AdminShopService;
import com.ecommerce.cnj70.service.CartService;
import com.ecommerce.cnj70.service.OrderService;
import com.ecommerce.cnj70.service.ProductService;
import com.ecommerce.cnj70.service.VendorKycService;
import com.ecommerce.cnj70.service.VendorService;
import com.ecommerce.cnj70.service.impl.AdminKycServiceImpl;
import com.ecommerce.cnj70.service.impl.AdminShopServiceImpl;
import com.ecommerce.cnj70.service.impl.CartServiceImpl;
import com.ecommerce.cnj70.service.impl.OrderServiceImpl;
import com.ecommerce.cnj70.service.impl.ProductServiceImpl;
import com.ecommerce.cnj70.service.impl.StorageService;
import com.ecommerce.cnj70.service.impl.ThirdPartyKycVerifier;
import com.ecommerce.cnj70.service.impl.VendorKycServiceImpl;
import com.ecommerce.cnj70.service.impl.VendorServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * VendorFlowE2ETest - Verify toàn bộ flow end-to-end của vendor.
 *
 * Scenario:
 *  1. Vendor đăng ký (mock) - có KYC = NOT_SUBMITTED
 *  2. Vendor submit KYC → status = PENDING_THIRD_PARTY → 3rd-party approve → PENDING_ADMIN
 *  3. Admin approve KYC → status = APPROVED
 *  4. Vendor tạo shop → status = PENDING
 *  5. Admin approve shop → status = APPROVED, vendor được tạo product
 *  6. Vendor tạo product (ACTIVE)
 *  7. Admin moderate product (giữ ACTIVE, hoặc HIDDEN)
 *  8. Khách add to cart
 *  9. Khách checkout → order tạo, stock giảm
 *  10. Vendor ship → SHIPPING → DELIVERED
 *
 * Test verify: mỗi bước chuyển state đúng, không bị bypass guard.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Vendor Flow E2E - full lifecycle")
class VendorFlowE2ETest {

    @Mock private UserRepository userRepository;
    @Mock private ShopRepository shopRepository;
    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private KycProfileRepository kycProfileRepository;
    @Mock private ThirdPartyKycVerifier thirdPartyKycVerifier;
    @Mock private StorageService storageService;

    private VendorService vendorService;
    private VendorKycService kycService;
    private AdminKycService adminKycService;
    private AdminShopService adminShopService;
    private ProductService productService;
    private CartService cartService;
    private OrderService orderService;

    private User vendor;
    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {
        // Khởi tạo các service
        vendorService = new VendorServiceImpl(userRepository, shopRepository, productRepository,
                orderRepository, kycService);

        kycService = new VendorKycServiceImpl(userRepository, kycProfileRepository,
                thirdPartyKycVerifier, storageService, shopRepository);

        adminKycService = new AdminKycServiceImpl(kycProfileRepository, userRepository);

        adminShopService = new AdminShopServiceImpl(shopRepository, mock(com.ecommerce.cnj70.service.AuditLogService.class));

        productService = new ProductServiceImpl(productRepository,
                mock(com.ecommerce.cnj70.repository.CategoryRepository.class),
                mock(com.ecommerce.cnj70.repository.ModerationHistoryRepository.class),
                mock(com.ecommerce.cnj70.service.AutoModerationResultProvider.class),
                mock(com.ecommerce.cnj70.service.AuditEventWriter.class));

        cartService = new CartServiceImpl(cartRepository, productRepository);

        orderService = new OrderServiceImpl(orderRepository, userRepository, productRepository,
                cartRepository, shopRepository, cartService,
                mock(com.ecommerce.cnj70.service.VoucherService.class));

        // Common stubs
        vendor = TestFixtures.userVendor("v-1", "v1@cnj70.com", KycStatus.NOT_SUBMITTED, null);
        admin = TestFixtures.userAdmin();
        customer = TestFixtures.userCustomer();

        when(userRepository.findByEmail("v1@cnj70.com")).thenReturn(Optional.of(vendor));
        when(userRepository.findByEmail("admin@cnj70.com")).thenReturn(Optional.of(admin));
        when(userRepository.findByEmail("customer1@cnj70.com")).thenReturn(Optional.of(customer));
        when(userRepository.findById("v-1")).thenReturn(Optional.of(vendor));
        when(userRepository.findById("u-customer-1")).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> {
            Shop s = inv.getArgument(0);
            if (s.getId() == null) s.setId("shop-" + System.nanoTime());
            return s;
        });
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            if (p.getId() == null) p.setId("p-" + System.nanoTime());
            return p;
        });
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(kycProfileRepository.save(any(KycProfile.class))).thenAnswer(inv -> {
            KycProfile p = inv.getArgument(0);
            if (p.getId() == null) p.setId("kyc-" + System.nanoTime());
            return p;
        });
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("E2E: full happy path - KYC submit → approve → tạo shop → approve → tạo product → checkout → deliver")
    void e2e_fullHappyPath() {
        // ===== BƯỚC 1: Vendor submit KYC =====
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.empty());

        KycProfile kycAfterSubmit = kycService.submitKyc(TestFixtures.customUserDetails(vendor), validKycForm());
        assertThat(kycAfterSubmit.getStatus()).isEqualTo(KycStatus.PENDING_THIRD_PARTY);
        verify(thirdPartyKycVerifier, atLeastOnce()).verifyAndUpdate(any(), any());

        // ===== BƯỚC 2: 3rd-party verify xong → chuyển status để admin duyệt =====
        kycAfterSubmit.setStatus(KycStatus.PENDING_ADMIN);
        when(kycProfileRepository.findById(kycAfterSubmit.getId())).thenReturn(Optional.of(kycAfterSubmit));
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(kycAfterSubmit));

        // ===== BƯỚC 3: Admin approve KYC =====
        KycProfile kycAfterApprove = adminKycService.approve(
                kycAfterSubmit.getId(), TestFixtures.customUserDetails(admin), "OK");
        assertThat(kycAfterApprove.getStatus()).isEqualTo(KycStatus.APPROVED);
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.APPROVED);

        // ===== BƯỚC 4: Vendor tạo shop =====
        when(shopRepository.existsByShopName("Shop E2E")).thenReturn(false);
        Shop createdShop = vendorService.createShop(TestFixtures.customUserDetails(vendor),
                com.ecommerce.cnj70.dto.request.ShopFormReq.builder()
                        .shopName("Shop E2E").description("desc").build());
        assertThat(createdShop.getStatus()).isEqualTo(ShopStatus.PENDING);
        assertThat(vendor.getShopId()).isEqualTo(createdShop.getId());

        // ===== BƯỚC 5: Admin approve shop =====
        when(shopRepository.findById(createdShop.getId())).thenReturn(Optional.of(createdShop));
        adminShopService.approveShop(createdShop.getId());
        assertThat(createdShop.getStatus()).isEqualTo(ShopStatus.APPROVED);

        // ===== BƯỚC 6: Vendor tạo product =====
        Product createdProduct = productService.createProduct(
                com.ecommerce.cnj70.dto.request.ProductFormReq.builder()
                        .name("Sản phẩm E2E")
                        .price(new BigDecimal("500000"))
                        .stock(10)
                        .build(),
                vendor.getShopId(), createdShop.getShopName());
        assertThat(createdProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(createdProduct.getStock()).isEqualTo(10);

        // ===== BƯỚC 7: Admin moderate (giữ ACTIVE) =====
        when(productRepository.findById(createdProduct.getId())).thenReturn(Optional.of(createdProduct));
        productService.updateProductStatus(createdProduct.getId(), ProductStatus.ACTIVE);
        assertThat(createdProduct.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        // ===== BƯỚC 8: Khách add to cart =====
        when(productRepository.findById(createdProduct.getId())).thenReturn(Optional.of(createdProduct));
        when(cartRepository.findByUserId("u-customer-1")).thenReturn(Optional.of(
                TestFixtures.emptyCart("u-customer-1")));
        Cart cart = cartService.addToCart("u-customer-1", createdProduct.getId(), 2);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(2);

        // ===== BƯỚC 9: Khách checkout =====
        when(cartRepository.findByUserId("u-customer-1")).thenReturn(Optional.of(cart));
        Order order = orderService.createOrder("u-customer-1",
                CheckoutReq.builder()
                        .shippingAddress("123 ABC, Q1, HCM")
                        .phone("0900000000")
                        .paymentMethod(PaymentMethod.COD)
                        .build());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(createdProduct.getStock()).isEqualTo(8); // 10 - 2

        // ===== BƯỚC 10: Vendor ship → DELIVERED =====
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        orderService.updateOrderStatus(order.getId(), OrderStatus.PREPARING);
        orderService.updateOrderStatus(order.getId(), OrderStatus.SHIPPING);
        orderService.updateOrderStatus(order.getId(), OrderStatus.DELIVERED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();

        // Verify final state
        assertThat(createdProduct.getStock()).isEqualTo(8);
        assertThat(createdShop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.APPROVED);
    }

    @Test
    @DisplayName("E2E FAIL: vendor chưa KYC → không tạo được shop")
    void e2e_fail_noKycCannotCreateShop() {
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.empty());

        // Vendor vẫn ở trạng thái NOT_SUBMITTED
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.NOT_SUBMITTED);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        vendorService.createShop(TestFixtures.customUserDetails(vendor),
                                com.ecommerce.cnj70.dto.request.ShopFormReq.builder()
                                        .shopName("Shop Fail").build()))
                .isInstanceOf(com.ecommerce.cnj70.exception.BadRequestException.class)
                .hasMessageContaining("KYC");
    }

    @Test
    @DisplayName("E2E FAIL: vendor KYC bị admin reject → không tạo được shop")
    void e2e_fail_adminRejectKyc() {
        KycProfile rejected = TestFixtures.kycProfile("kyc-1", "v-1", KycStatus.PENDING_ADMIN);
        when(kycProfileRepository.findById("kyc-1")).thenReturn(Optional.of(rejected));
        when(kycProfileRepository.findByUserId("v-1")).thenReturn(Optional.of(rejected));

        KycProfile afterReject = adminKycService.reject("kyc-1",
                TestFixtures.customUserDetails(admin), "Sai CCCD");
        assertThat(afterReject.getStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);
        assertThat(vendor.getKycStatus()).isEqualTo(KycStatus.ADMIN_REJECTED);

        // Vendor vẫn không tạo được shop vì kycStatus != APPROVED
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        vendorService.createShop(TestFixtures.customUserDetails(vendor),
                                com.ecommerce.cnj70.dto.request.ShopFormReq.builder()
                                        .shopName("Shop").build()))
                .isInstanceOf(com.ecommerce.cnj70.exception.BadRequestException.class)
                .hasMessageContaining("KYC");
    }

    @Test
    @DisplayName("E2E FAIL: shop PENDING → vendor chưa được tạo product theo rule (currently bypassed)")
    void e2e_fail_pendingShop_currentlyBypassed() {
        // GAP hiện tại: ProductServiceImpl.createProduct không check Shop.status
        // → Vendor có thể tạo product khi shop đang PENDING (chờ admin duyệt)
        // Test này document gap
        Shop pendingShop = TestFixtures.pendingShop("shop-1", "v-1");
        when(shopRepository.existsByShopName(any())).thenReturn(false);
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> {
            Shop s = inv.getArgument(0);
            s.setId("shop-1");
            return s;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // Vendor tạo shop
        vendor.setKycStatus(KycStatus.APPROVED);
        Shop created = vendorService.createShop(TestFixtures.customUserDetails(vendor),
                com.ecommerce.cnj70.dto.request.ShopFormReq.builder()
                        .shopName("Shop Pending").build());
        assertThat(created.getStatus()).isEqualTo(ShopStatus.PENDING);

        // Hiện tại: vẫn tạo được product (gap)
        Product product = productService.createProduct(
                com.ecommerce.cnj70.dto.request.ProductFormReq.builder()
                        .name("SP").price(new BigDecimal("100000")).stock(5).build(),
                created.getId(), created.getShopName());
        assertThat(product).isNotNull(); // bypass guard

        // → Fix: ProductServiceImpl nên check Shop.status == APPROVED
    }

    @Test
    @DisplayName("E2E FAIL: product OUT_OF_STOCK → khách không checkout được")
    void e2e_fail_outOfStockProduct_cannotCheckout() {
        User customer = TestFixtures.userCustomer();
        Product outOfStock = TestFixtures.activeProduct("p-1", "shop-1", 0); // stock = 0
        Cart cart = TestFixtures.cartWith("u-1", new ArrayList<>(List.of(
                TestFixtures.cartItem("p-1", "shop-1", 1, 0, new BigDecimal("100000")))));

        when(userRepository.findById("u-1")).thenReturn(Optional.of(customer));
        when(cartRepository.findByUserId("u-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById("p-1")).thenReturn(Optional.of(outOfStock));
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(
                TestFixtures.shop("shop-1", "owner-1", ShopStatus.APPROVED, true)));

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        orderService.createOrder("u-1",
                                CheckoutReq.builder().shippingAddress("X").build()))
                .isInstanceOf(com.ecommerce.cnj70.exception.BadRequestException.class)
                .hasMessageContaining("không đủ hàng");
    }

    // ============ helper ============

    private KycFormReq validKycForm() {
        return KycFormReq.builder()
                .ownerFullName("Nguyễn Văn E2E")
                .idNumber("012345678901")
                .bankAccount("1234567890")
                .bankName("Vietcombank")
                .build();
    }

    private static <T> T mock(Class<T> classToMock) {
        return org.mockito.Mockito.mock(classToMock);
    }
}
