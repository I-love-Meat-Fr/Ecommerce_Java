package com.ecommerce.cnj70.fix.phase2;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho Phase 2 — Shop Fix Contract (§9).
 *
 * <p>Verify rules (§9.3, §9.5):</p>
 * <ul>
 *   <li>{@code approveShop}: persist via {@code shopRepository.save} (MongoDB write).</li>
 *   <li>{@code rejectShop}: persist via {@code shopRepository.save}; lưu reason + actor.</li>
 *   <li>{@code activateShop}: persist via {@code shopRepository.save}; idempotent guard.</li>
 *   <li>{@code deactivateShop}: persist via {@code shopRepository.save}; lưu reason + actor.</li>
 *   <li>{@code listShops}: filter by status (null = all, status = PENDING only).</li>
 *   <li>{@code getShopById}: invalid id → throw ResourceNotFoundException.</li>
 * </ul>
 *
 * <p>Existing {@code AdminShopServiceTest} đã cover happy paths của approve/reject/activate/deactivate.
 * Test này focus vào <strong>Persistence + Filter + Edge cases</strong> của Phase 2.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AdminShopService - Phase 2 fix verification")
class ShopFixTest {

    @Mock private ShopRepository shopRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;

    @InjectMocks private AdminShopServiceImpl adminShopService;

    @BeforeEach
    void setUp() {
        // Default: shopRepository.save echo back argument (allows inspection of mutated state)
        lenient().when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));

        // Default: raw Document query returns null → throws ResourceNotFoundException
        FindIterable<Document> emptyFind = mock(FindIterable.class);
        lenient().when(emptyFind.first()).thenReturn(null);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(emptyFind);
        lenient().when(mongoTemplate.getCollection("shops")).thenReturn(mongoCollection);
    }

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

    // ============ §9.3 — Persistence verification ============

    @Test
    @DisplayName("approveShop: PENDING → APPROVED + MongoDB save called")
    void approveShop_validId_persistedToMongoDB() {
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.approveShop("shop-1", "admin-1", "admin@cnj70.com");

        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(captor.capture());
        Shop saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(ShopStatus.APPROVED);
        assertThat(saved.getId()).isEqualTo("shop-1");
    }

    @Test
    @DisplayName("rejectShop: valid id + reason → REJECTED + reason persisted to MongoDB")
    void rejectShop_validIdAndReason_persistedToMongoDB() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.rejectShop("shop-1", "Vi phạm chính sách", "admin@cnj70.com");

        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(captor.capture());
        Shop saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(saved.getRejectionReason()).isEqualTo("Vi phạm chính sách");
        assertThat(saved.getActionBy()).isEqualTo("admin@cnj70.com");
        assertThat(saved.getActionAt()).isNotNull();
    }

    @Test
    @DisplayName("activateShop: inactive → active + MongoDB save called")
    void activateShop_validId_persistedToMongoDB() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        shop.setActive(false);
        stubRawGetShopById(shop);

        adminShopService.activateShop("shop-1", "admin-1", "admin@cnj70.com");

        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    @DisplayName("deactivateShop: active → inactive + reason persisted to MongoDB")
    void deactivateShop_validId_persistedToMongoDB() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.deactivateShop("shop-1", "Vi phạm điều khoản", "admin@cnj70.com");

        ArgumentCaptor<Shop> captor = ArgumentCaptor.forClass(Shop.class);
        verify(shopRepository).save(captor.capture());
        Shop saved = captor.getValue();

        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getDeactivationReason()).isEqualTo("Vi phạm điều khoản");
        assertThat(saved.getActionBy()).isEqualTo("admin@cnj70.com");
        assertThat(saved.getActionAt()).isNotNull();
    }

    // ============ §9.3 — Edge cases ============

    @Test
    @DisplayName("approveShop: invalid id → ResourceNotFoundException + NO save")
    void approveShop_invalidId_throwNotFound() {
        // Default raw query returns null → throws

        assertThatThrownBy(() -> adminShopService.approveShop("nonexistent", "admin-1", "admin@x.com"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("rejectShop: already REJECTED → idempotent (no save called again)")
    void rejectShop_alreadyRejected_idempotent() {
        Shop shop = TestFixtures.rejectedShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        adminShopService.rejectShop("shop-1", "new reason", "admin-1");

        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("activateShop: already active → BusinessException + NO save")
    void activateShop_alreadyActive_throws() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        stubRawGetShopById(shop);

        assertThatThrownBy(() -> adminShopService.activateShop("shop-1", "admin-1", "admin@x.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang ở trạng thái hoạt động");

        verify(shopRepository, never()).save(any(Shop.class));
    }

    @Test
    @DisplayName("deactivateShop: already inactive → BusinessException + NO save")
    void deactivateShop_alreadyInactive_throws() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        shop.setActive(false);
        stubRawGetShopById(shop);

        assertThatThrownBy(() -> adminShopService.deactivateShop("shop-1", "test", "admin-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ngừng hoạt động");

        verify(shopRepository, never()).save(any(Shop.class));
    }

    // ============ §9.5 — listShops filter by status ============

    @Test
    @DisplayName("listShops: status=PENDING → gọi findByStatus trên ShopRepository")
    void listShops_filterByStatus_callsFindByStatus() {
        Shop pendingShop = TestFixtures.pendingShop("shop-1", "v-1");
        Page<Shop> expectedPage = new PageImpl<>(List.of(pendingShop));
        when(shopRepository.findByStatus(eq(ShopStatus.PENDING), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Shop> result = adminShopService.listShops(
                PageRequest.of(0, 10), null, ShopStatus.PENDING);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ShopStatus.PENDING);
        verify(shopRepository).findByStatus(eq(ShopStatus.PENDING), any(Pageable.class));
    }

    @Test
    @DisplayName("listShops: status=null + q=null → gọi findAll (admin shop-list ALL)")
    void listShops_noFilter_callsFindAll() {
        Shop anyShop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        Page<Shop> expectedPage = new PageImpl<>(List.of(anyShop));
        when(shopRepository.findAll(any(Pageable.class))).thenReturn(expectedPage);

        Page<Shop> result = adminShopService.listShops(
                PageRequest.of(0, 10), null, null);

        assertThat(result.getContent()).hasSize(1);
        verify(shopRepository).findAll(any(Pageable.class));
        verify(shopRepository, never()).findByStatus(any(ShopStatus.class), any(Pageable.class));
    }

    @Test
    @DisplayName("listShops: status=APPROVED + q='shop' → gọi findByStatusAndShopNameContainingIgnoreCase")
    void listShops_statusAndQuery_callsCombinedFilter() {
        Shop approved = TestFixtures.approvedActiveShop("shop-1", "v-1");
        Page<Shop> expectedPage = new PageImpl<>(List.of(approved));
        when(shopRepository.findByStatusAndShopNameContainingIgnoreCase(
                eq(ShopStatus.APPROVED), eq("shop"), any(Pageable.class)))
                .thenReturn(expectedPage);

        Page<Shop> result = adminShopService.listShops(
                PageRequest.of(0, 10), "shop", ShopStatus.APPROVED);

        assertThat(result.getContent()).hasSize(1);
        verify(shopRepository).findByStatusAndShopNameContainingIgnoreCase(
                eq(ShopStatus.APPROVED), eq("shop"), any(Pageable.class));
    }
}
