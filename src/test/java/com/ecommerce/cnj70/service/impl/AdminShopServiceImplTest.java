package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho AdminShopServiceImpl — Phase 5 (Shop Approval & Status Management).
 *
 * Verify rule:
 *  - approveShop: PENDING → APPROVED, xóa rejectionReason, ghi AuditLog SHOP_APPROVED.
 *  - rejectShop: → REJECTED, set rejectionReason + actionBy + actionAt, ghi AuditLog SHOP_REJECTED.
 *  - activateShop: inactive → active, idempotent (idempotent nếu đã active → throw BusinessException).
 *  - getShopById: shop không tồn tại → throw ResourceNotFoundException.
 *
 * <p>Lưu ý:
 *  - {@link AdminShopServiceImpl} inject {@link AuditLogService} (không phải {@code AuditLogRepository}),
 *    nên các verify phải đi qua {@code auditLogService.log(...)}, không phải {@code auditLogRepository.save(...)}.
 *  - Signature 3 tham số: {@code approveShop(id, actorId, actorUsername)},
 *    {@code activateShop(id, actorId, actorUsername)}, {@code rejectShop(id, reason, adminUsername)}.
 *  - KHÔNG thêm cascade Shop → Product (đó là Phase D).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminShopServiceImplTest {

    @Mock private ShopRepository shopRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;

    @InjectMocks private AdminShopServiceImpl service;

    @BeforeEach
    void setUp() {
        lenient().when(shopRepository.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));
        // Default: raw Document query trả null (cho shopNotFound_throws).
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
        lenient().when(findIterable.first()).thenReturn(raw);
        lenient().when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        lenient().when(mongoTemplate.getConverter()).thenReturn(
                mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        lenient().when(mongoTemplate.getConverter().read(eq(Shop.class), eq(raw))).thenReturn(shop);
    }

    private Shop newShop() {
        Shop s = new Shop();
        s.setId("s1");
        s.setShopName("Test Shop");
        s.setStatus(ShopStatus.PENDING);
        s.setActive(true);
        return s;
    }

    @Test
    void approveShop_pendingToApproved() {
        Shop s = newShop();
        stubRawGetShopById(s);
        when(shopRepository.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));

        // Signature thật: approveShop(id, actorId, actorUsername)
        service.approveShop("s1", null, "admin");

        assertEquals(ShopStatus.APPROVED, s.getStatus());
        verify(shopRepository).save(any(Shop.class));
        verify(auditLogService).log(
                any(), anyString(), anyString(), any(), any(), anyString(),
                any(), anyString(), any());
    }

    @Test
    void rejectShop_pendingToRejected_writesReason() {
        Shop s = newShop();
        stubRawGetShopById(s);
        when(shopRepository.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));

        service.rejectShop("s1", "Giấy tờ không hợp lệ", "admin");

        assertEquals(ShopStatus.REJECTED, s.getStatus());
        assertNotNull(s.getRejectionReason());
        assertEquals("Giấy tờ không hợp lệ", s.getRejectionReason());
        assertEquals("admin", s.getActionBy());
        verify(shopRepository).save(any(Shop.class));
        verify(auditLogService).logWarning(
                any(), anyString(), anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void activateShop_idempotent() {
        Shop s = newShop();
        s.setActive(false);
        stubRawGetShopById(s);
        when(shopRepository.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));

        // Signature thật: activateShop(id, actorId, actorUsername)
        service.activateShop("s1", null, "admin");

        assertTrue(s.isActive());
        verify(shopRepository).save(any(Shop.class));
        verify(auditLogService).logInfo(
                any(), anyString(), anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void activateShop_alreadyActive() {
        Shop s = newShop();
        s.setActive(true);
        stubRawGetShopById(s);

        assertThrows(BusinessException.class,
                () -> service.activateShop("s1", null, "admin"));

        // Shop đã active rồi → KHÔNG save, KHÔNG ghi AuditLog.
        verify(shopRepository, never()).save(any());
        verify(auditLogService, never()).logInfo(
                any(), anyString(), anyString(), any(), any(), anyString(), anyString());
    }

    @Test
    void shopNotFound_throws() {
        // Default raw query trả null (setUp) → throws ResourceNotFoundException.

        assertThrows(ResourceNotFoundException.class,
                () -> service.approveShop("nope", null, "admin"));

        verify(shopRepository, never()).save(any());
    }
}
