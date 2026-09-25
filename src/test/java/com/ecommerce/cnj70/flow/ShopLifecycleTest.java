package com.ecommerce.cnj70.flow;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AdminShopService;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.impl.AdminShopServiceImpl;
import com.ecommerce.cnj70.support.TestFixtures;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ShopLifecycleTest - Verify lifecycle của Shop: PENDING → APPROVED → REJECTED/SUSPENDED.
 *
 * Hiện tại: Shop document KHÔNG có field reason → admin reject/deactivate không lưu lý do.
 * Test này verify behavior hiện tại + document gap.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Shop Lifecycle Test")
class ShopLifecycleTest {

    @Mock private ShopRepository shopRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;

    @InjectMocks private AdminShopServiceImpl adminShopService;

    @SuppressWarnings("unchecked")
    private void stubRawGetShopById(Shop shop) {
        Document raw = new Document("_id", shop.getId())
                .append("shopName", shop.getShopName() != null ? shop.getShopName() : "")
                .append("status", shop.getStatus() != null ? shop.getStatus().name() : null)
                .append("active", shop.isActive());

        FindIterable<Document> findIterable = mock(FindIterable.class);
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        lenient().when(mongoTemplate.getCollection("shops")).thenReturn(mongoCollection);
        lenient().when(mongoTemplate.getConverter()).thenReturn(
                mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        lenient().when(mongoTemplate.getConverter().read(eq(Shop.class), eq(raw))).thenReturn(shop);
    }

    @BeforeEach
    void setUp() {
        when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
        // Default: raw Document query trả null (cho tests notFound).
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("shops")).thenReturn(mongoCollection);
    }

    @Test
    @DisplayName("PENDING → APPROVED (admin duyệt)")
    void pendingToApproved() {
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.approveShop("shop-1");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(shop.isActive()).isTrue(); // mặc định active
    }

    @Test
    @DisplayName("PENDING → REJECTED (admin từ chối với lý do)")
    void pendingToRejected() {
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.rejectShop("shop-1", "Vi phạm điều khoản sử dụng", "admin@cnj70.com");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(shop.getRejectionReason()).isEqualTo("Vi phạm điều khoản sử dụng");
    }

    @Test
    @DisplayName("APPROVED active → APPROVED inactive (admin deactivate với lý do)")
    void activeToInactive() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.deactivateShop("shop-1", "Bán sản phẩm cấm", "admin@cnj70.com");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED); // status vẫn APPROVED
        assertThat(shop.isActive()).isFalse(); // active = false
        assertThat(shop.getDeactivationReason()).isEqualTo("Bán sản phẩm cấm");
    }

    @Test
    @DisplayName("APPROVED inactive → APPROVED active (admin activate)")
    void inactiveToActive() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        shop.setActive(false);
        stubRawGetShopById(shop);

        adminShopService.activateShop("shop-1");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(shop.isActive()).isTrue();
    }

    @Test
    @DisplayName("Idempotent: approve lần 2 → không throw")
    void approveTwice_idempotent() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.approveShop("shop-1"); // approved
        adminShopService.approveShop("shop-1"); // no-op

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
    }

    @Test
    @DisplayName("Deactivate shop đã inactive → throw BusinessException")
    void deactivateAlreadyInactive_throws() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        shop.setActive(false);
        stubRawGetShopById(shop);

        assertThatThrownBy(() -> adminShopService.deactivateShop("shop-1", "test", "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ngừng hoạt động");
    }

    @Test
    @DisplayName("Admin reject: lưu lý do + admin username")
    void adminRejectsButNoReasonSaved_currentBehavior() {
        // AdminShopService đã lưu lý do và admin username vào Shop document
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.rejectShop("shop-1", "Sai thông tin CCCD", "admin@cnj70.com");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(shop.getRejectionReason()).isEqualTo("Sai thông tin CCCD");
        assertThat(shop.getActionBy()).isEqualTo("admin@cnj70.com");
    }

    @Test
    @DisplayName("Activate shop đã active → throw BusinessException")
    void activateAlreadyActive_throws() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        assertThatThrownBy(() -> adminShopService.activateShop("shop-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("hoạt động");
    }

    @Test
    @DisplayName("ShopStatus enum: kiểm tra các trạng thái hiện có")
    void shopStatusEnum_noSuspendedState_currentBehavior() {
        // Hiện tại ShopStatus có 5 giá trị (Phase 3A mở rộng):
        //   PENDING, APPROVED, REJECTED (base)
        //   SUSPENDED (admin enforcement nặng) + RESTRICTED (admin enforcement nhẹ)
        // NOTE: deactivate → Shop.active=false + deactivationReason; không dùng SUSPENDED status
        ShopStatus[] statuses = ShopStatus.values();
        assertThat(statuses).containsExactly(
                ShopStatus.PENDING, ShopStatus.APPROVED, ShopStatus.REJECTED,
                ShopStatus.SUSPENDED, ShopStatus.RESTRICTED);
        assertThat(statuses).hasSize(5);
    }
}
