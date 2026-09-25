package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ShopRepository;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test cho AdminShopServiceImpl.
 *
 * Verify rule:
 *  - approveShop: idempotent (approve lần 2 không throw)
 *  - activateShop: shop đã active → throw BusinessException
 *  - deactivateShop: shop đã inactive → throw BusinessException
 *  - rejectShop: idempotent
 *  - getShopById: không tồn tại → throw
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminShopService - unit test")
class AdminShopServiceTest {

    @Mock private ShopRepository shopRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;

    @InjectMocks private AdminShopServiceImpl adminShopService;

    /**
     * Stub raw getShopById(): mongoTemplate.getCollection("shops").find(filter).first() → Document
     * Phase 4 — pattern giống AdminUserServiceImplTest.stubRawFindOne.
     */
    @SuppressWarnings("unchecked")
    private void stubRawGetShopById(Shop shop) {
        Document raw = new Document("_id", shop.getId())
                .append("shopName", shop.getShopName() != null ? shop.getShopName() : "")
                .append("status", shop.getStatus() != null ? shop.getStatus().name() : null)
                .append("active", shop.isActive());

        FindIterable<Document> findIterable = mock(FindIterable.class);
        lenient().when(mongoTemplate.getCollection("shops")).thenReturn(mongoCollection);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoTemplate.getConverter()).thenReturn(
                mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        lenient().when(mongoTemplate.getConverter().read(eq(Shop.class), eq(raw))).thenReturn(shop);
    }

    @BeforeEach
    void setUp() {
        lenient().when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
        // Default cho tests "notFound": raw Document query trả null.
        // Stub MongoCollection trả null FindIterable.first() → throws ResourceNotFoundException.
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("shops")).thenReturn(mongoCollection);
    }

    @Test
    @DisplayName("approveShop: PENDING → APPROVED")
    void approveShop_pendingToApproved() {
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.approveShop("shop-1");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
    }

    @Test
    @DisplayName("approveShop: APPROVED → APPROVED (idempotent, không throw)")
    void approveShop_alreadyApproved_idempotent() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.approveShop("shop-1"); // không throw

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
    }

    @Test
    @DisplayName("approveShop: shop không tồn tại → throw ResourceNotFoundException")
    void approveShop_notFound_throws() {
        // Default raw query trả null (setUp) → throws ResourceNotFoundException

        assertThatThrownBy(() -> adminShopService.approveShop("shop-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("activateShop: inactive → active")
    void activateShop_inactiveToActive() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        shop.setActive(false);
        stubRawGetShopById(shop);

        adminShopService.activateShop("shop-1");

        assertThat(shop.isActive()).isTrue();
    }

    @Test
    @DisplayName("activateShop: shop đã active → throw BusinessException")
    void activateShop_alreadyActive_throws() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        assertThatThrownBy(() -> adminShopService.activateShop("shop-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang ở trạng thái hoạt động");
    }

    @Test
    @DisplayName("deactivateShop: active → inactive (với lý do)")
    void deactivateShop_activeToInactive() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.deactivateShop("shop-1", "Vi phạm điều khoản", "admin@test");

        assertThat(shop.isActive()).isFalse();
        assertThat(shop.getDeactivationReason()).isEqualTo("Vi phạm điều khoản");
        assertThat(shop.getActionBy()).isEqualTo("admin@test");
    }

    @Test
    @DisplayName("deactivateShop: shop đã inactive → throw BusinessException")
    void deactivateShop_alreadyInactive_throws() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        shop.setActive(false);
        stubRawGetShopById(shop);

        assertThatThrownBy(() -> adminShopService.deactivateShop("shop-1", "test", "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ngừng hoạt động");
    }

    @Test
    @DisplayName("rejectShop: APPROVED → REJECTED (với lý do)")
    void rejectShop_approvedToRejected() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.rejectShop("shop-1", "Vi phạm chính sách", "admin@test");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(shop.getRejectionReason()).isEqualTo("Vi phạm chính sách");
        assertThat(shop.getActionBy()).isEqualTo("admin@test");
    }

    @Test
    @DisplayName("rejectShop: REJECTED → REJECTED (idempotent)")
    void rejectShop_alreadyRejected_idempotent() {
        Shop shop = TestFixtures.rejectedShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.rejectShop("shop-1", "test", "admin"); // không throw

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
    }

    @Test
    @DisplayName("rejectShop: lưu lý do và admin thực hiện")
    void currentBehavior_noReasonStored() {
        // AdminShopService đã lưu reason + actionBy vào Shop document
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.rejectShop("shop-1", "Sai thông tin CCCD", "admin@cnj70.com");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(shop.getRejectionReason()).isEqualTo("Sai thông tin CCCD");
        assertThat(shop.getActionBy()).isEqualTo("admin@cnj70.com");
    }
}
