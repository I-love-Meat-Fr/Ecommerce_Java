package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.VoucherType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.VoucherRepository;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho VoucherServiceImpl — Phase 11 (Admin WEB Voucher Management).
 *
 * <p>Verify rule:</p>
 * <ul>
 *   <li>{@code createWebVoucher}: force type=WEB, shopId=null; validate discountValue>0,
 *       percent<=100, quantity>0, endDate>=startDate; ghi AuditEvent VOUCHER_CREATED.</li>
 *   <li>{@code updateWebVoucher}: giữ type=WEB, shopId=null; reject nếu SHOP;
 *       reject nếu quantity<used; ghi AuditEvent VOUCHER_UPDATED.</li>
 *   <li>{@code activateWebVoucher}: set active=true; reject SHOP; ghi AuditEvent VOUCHER_ACTIVATED.</li>
 *   <li>{@code deactivateWebVoucher}: set active=false; reject SHOP; ghi AuditEvent VOUCHER_DEACTIVATED.</li>
 *   <li>{@code deleteWebVoucher}: SOFT DELETE (set active=false); reject SHOP; ghi AuditEvent VOUCHER_DELETED.</li>
 *   <li>{@code getWebVouchers(pageable)}: gọi findByType(WEB, pageable) khi không filter.</li>
 *   <li>{@code getWebVouchers(pageable, q, active)}: gọi searchWebVouchers khi có q hoặc active filter.</li>
 * </ul>
 *
 * <p>WEB/SHOP isolation: {@code requireWebVoucher()} chặn SHOP voucher ở tất cả admin operations.
 * Hard delete KHÔNG được thực hiện theo yêu cầu Phase 11 (chỉ soft delete).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VoucherServiceImplTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private MongoCollection<Document> mongoCollection;
    @Mock private FindIterable<Document> findIterable;
    @Mock private com.mongodb.client.MongoCursor<Document> mongoCursor;
    @Mock private MongoConverter mongoConverter;
    @Mock private AuditEventWriter auditEventWriter;

    @InjectMocks private VoucherServiceImpl service;

    @BeforeEach
    void setUpRawQueryStub() {
        // Phase 5: getVoucherById uses raw mongoTemplate.getCollection("vouchers").find().first().
        // Default to "not found" so tests that don't override get null.
        org.mockito.Mockito.lenient()
                .when(mongoTemplate.getCollection("vouchers")).thenReturn(mongoCollection);
        org.mockito.Mockito.lenient()
                .when(mongoCollection.find(any(Document.class))).thenReturn(findIterable);
        org.mockito.Mockito.lenient()
                .when(findIterable.first()).thenReturn(null);
        org.mockito.Mockito.lenient()
                .when(mongoTemplate.getConverter()).thenReturn(mongoConverter);
    }

    /**
     * Stub raw mongoTemplate getCollection("vouchers").find().first() to return a
     * Mongo Document representing the given Voucher (Phase 5 raw query path).
     */
    private void stubRawGetVoucherById(Voucher v) {
        Document doc = new Document("_id", v.getId())
                .append("code", v.getCode())
                .append("name", v.getName())
                .append("type", v.getType() != null ? v.getType().name() : null)
                .append("discountType", v.getDiscountType() != null ? v.getDiscountType().name() : null)
                .append("discountValue", v.getDiscountValue())
                .append("maxDiscountAmount", v.getMaxDiscountAmount())
                .append("minOrderValue", v.getMinOrderValue())
                .append("quantity", v.getQuantity())
                .append("used", v.getUsed())
                .append("active", v.isActive())
                .append("startDate", v.getStartDate())
                .append("endDate", v.getEndDate())
                .append("createdBy", v.getCreatedBy())
                .append("shopId", v.getShopId())
                .append("shopName", v.getShopName());
        org.mockito.Mockito.lenient().when(findIterable.first()).thenReturn(doc);
        org.mockito.Mockito.lenient()
                .when(mongoConverter.read(eq(Voucher.class), eq(doc))).thenReturn(v);
    }

    // ===== Helpers =====

    private Voucher webVoucher() {
        return Voucher.builder()
                .id("v1")
                .code("WEBPROMO")
                .name("Web Promo")
                .type(VoucherType.WEB)
                .shopId(null)
                .shopName(null)
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.TEN)
                .maxDiscountAmount(null)
                .minOrderValue(BigDecimal.ZERO)
                .quantity(100)
                .used(0)
                .active(true)
                .createdBy("admin")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private Voucher shopVoucher() {
        Voucher v = webVoucher();
        v.setId("vs1");
        v.setCode("SHOPPROMO");
        v.setType(VoucherType.SHOP);
        v.setShopId("shop-1");
        v.setShopName("Test Shop");
        return v;
    }

    private VoucherFormReq validForm() {
        return VoucherFormReq.builder()
                .code("NEWVOUCHER")
                .name("New Voucher")
                .discountType(DiscountType.PERCENT)
                .discountValue(BigDecimal.valueOf(15))
                .maxDiscountAmount(BigDecimal.valueOf(50000))
                .minOrderValue(BigDecimal.valueOf(100000))
                .quantity(50)
                .productIds(List.of())
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();
    }

    // ===== createWebVoucher =====

    @Test
    void createWebVoucher_valid_setsTypeAndShopIdNull() {
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArgument(0));

        Voucher saved = service.createWebVoucher(validForm(), "admin");

        assertEquals(VoucherType.WEB, saved.getType());
        assertNull(saved.getShopId());
        assertEquals("admin", saved.getCreatedBy());
        verify(voucherRepository).save(any(Voucher.class));
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    void createWebVoucher_duplicateCode_throws409() {
        when(voucherRepository.existsByCode("NEWVOUCHER")).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.createWebVoucher(validForm(), "admin"));

        verify(voucherRepository, never()).save(any());
        verify(auditEventWriter, never()).write(any());
    }

    @Test
    void createWebVoucher_percentOver100_throws() {
        VoucherFormReq form = validForm();
        form.setDiscountValue(BigDecimal.valueOf(150));

        when(voucherRepository.existsByCode(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.createWebVoucher(form, "admin"));

        verify(voucherRepository, never()).save(any());
    }

    @Test
    void createWebVoucher_discountValueZero_throws() {
        VoucherFormReq form = validForm();
        form.setDiscountValue(BigDecimal.ZERO);

        when(voucherRepository.existsByCode(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.createWebVoucher(form, "admin"));

        verify(voucherRepository, never()).save(any());
    }

    @Test
    void createWebVoucher_quantityZero_throws() {
        VoucherFormReq form = validForm();
        form.setQuantity(0);

        when(voucherRepository.existsByCode(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.createWebVoucher(form, "admin"));

        verify(voucherRepository, never()).save(any());
    }

    @Test
    void createWebVoucher_endDateBeforeStartDate_throws() {
        VoucherFormReq form = validForm();
        LocalDateTime start = LocalDateTime.now();
        form.setStartDate(start);
        form.setEndDate(start.minusDays(1));

        when(voucherRepository.existsByCode(anyString())).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.createWebVoucher(form, "admin"));

        verify(voucherRepository, never()).save(any());
    }

    // ===== updateWebVoucher =====

    @Test
    void updateWebVoucher_valid_keepsWebType() {
        Voucher existing = webVoucher();
        stubRawGetVoucherById(existing);
        when(voucherRepository.existsByCode(anyString())).thenReturn(false);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArgument(0));

        VoucherFormReq form = validForm();
        form.setCode("WEBPROMO");
        form.setName("Updated Name");
        Voucher after = service.updateWebVoucher("v1", form);

        assertEquals(VoucherType.WEB, after.getType());
        assertNull(after.getShopId());
        assertEquals("Updated Name", after.getName());
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    void updateWebVoucher_shopVoucher_throws() {
        Voucher existing = shopVoucher();
        stubRawGetVoucherById(existing);

        assertThrows(BadRequestException.class,
                () -> service.updateWebVoucher("vs1", validForm()));

        verify(voucherRepository, never()).save(any());
        verify(auditEventWriter, never()).write(any());
    }

    @Test
    void updateWebVoucher_quantityBelowUsed_throws() {
        Voucher existing = webVoucher();
        existing.setUsed(30);
        stubRawGetVoucherById(existing);

        VoucherFormReq form = validForm();
        form.setCode("WEBPROMO");
        form.setQuantity(20); // < used(30)

        assertThrows(BadRequestException.class,
                () -> service.updateWebVoucher("v1", form));

        verify(voucherRepository, never()).save(any());
    }

    @Test
    void updateWebVoucher_endDateBeforeStartDate_throws() {
        Voucher existing = webVoucher();
        stubRawGetVoucherById(existing);

        VoucherFormReq form = validForm();
        form.setCode("WEBPROMO");
        LocalDateTime start = LocalDateTime.now();
        form.setStartDate(start);
        form.setEndDate(start.minusDays(1));

        assertThrows(BadRequestException.class,
                () -> service.updateWebVoucher("v1", form));

        verify(voucherRepository, never()).save(any());
    }

    // ===== activateWebVoucher =====

    @Test
    void activateWebVoucher_setsActiveTrue_andAudit() {
        Voucher existing = webVoucher();
        existing.setActive(false);
        stubRawGetVoucherById(existing);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArgument(0));

        Voucher after = service.activateWebVoucher("v1");

        assertTrue(after.isActive());
        verify(voucherRepository).save(any(Voucher.class));
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    void activateWebVoucher_shopVoucher_throws() {
        Voucher existing = shopVoucher();
        stubRawGetVoucherById(existing);

        assertThrows(BadRequestException.class,
                () -> service.activateWebVoucher("vs1"));

        verify(voucherRepository, never()).save(any());
        verify(auditEventWriter, never()).write(any());
    }

    // ===== deactivateWebVoucher =====

    @Test
    void deactivateWebVoucher_setsActiveFalse_andAudit() {
        Voucher existing = webVoucher();
        existing.setActive(true);
        stubRawGetVoucherById(existing);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArgument(0));

        Voucher after = service.deactivateWebVoucher("v1");

        assertFalse(after.isActive());
        verify(voucherRepository).save(any(Voucher.class));
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    void deactivateWebVoucher_shopVoucher_throws() {
        Voucher existing = shopVoucher();
        stubRawGetVoucherById(existing);

        assertThrows(BadRequestException.class,
                () -> service.deactivateWebVoucher("vs1"));

        verify(voucherRepository, never()).save(any());
        verify(auditEventWriter, never()).write(any());
    }

    // ===== deleteWebVoucher (Phase 11: soft delete only) =====

    @Test
    void deleteWebVoucher_softDelete_setsActiveFalse() {
        Voucher existing = webVoucher();
        existing.setActive(true);
        stubRawGetVoucherById(existing);
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(i -> i.getArgument(0));

        service.deleteWebVoucher("v1");

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(captor.capture());
        Voucher saved = captor.getValue();
        assertFalse(saved.isActive(), "Phase 11: delete = soft delete (active=false), không hard delete");
        verify(auditEventWriter).write(any(AuditEvent.class));
    }

    @Test
    void deleteWebVoucher_shopVoucher_throws() {
        Voucher existing = shopVoucher();
        stubRawGetVoucherById(existing);

        assertThrows(BadRequestException.class,
                () -> service.deleteWebVoucher("vs1"));

        verify(voucherRepository, never()).save(any());
        verify(voucherRepository, never()).delete(any());
        verify(auditEventWriter, never()).write(any());
    }

    // ===== getVoucherById / Not Found =====

    @Test
    void getVoucherById_notFound_throws() {
        // Phase 5: getVoucherById uses raw collection query.
        // The default stub in setUpRawQueryStub already returns null (not found).
        assertThrows(ResourceNotFoundException.class,
                () -> service.getVoucherById("nope"));
    }

    // ===== getWebVouchers (Phase 11: list with search/filter/pagination) =====

    @Test
    void getWebVouchers_noFilter_callsFindByType() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Voucher> page = new PageImpl<>(List.of(webVoucher()), pageable, 1);
        when(voucherRepository.findByType(VoucherType.WEB, pageable)).thenReturn(page);

        Page<Voucher> result = service.getWebVouchers(pageable, null, null);

        assertEquals(1, result.getTotalElements());
        verify(voucherRepository).findByType(VoucherType.WEB, pageable);
    }

    @Test
    void getWebVouchers_filterActiveOnly_callsFindByTypeAndActive() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Voucher> page = new PageImpl<>(List.of(webVoucher()), pageable, 1);
        when(voucherRepository.findByTypeAndActive(VoucherType.WEB, true, pageable)).thenReturn(page);

        Page<Voucher> result = service.getWebVouchers(pageable, null, Boolean.TRUE);

        assertEquals(1, result.getTotalElements());
        verify(voucherRepository).findByTypeAndActive(VoucherType.WEB, true, pageable);
    }

    @Test
    void getWebVouchers_withSearch_usesMongoTemplate() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Voucher> page = new PageImpl<>(List.of(webVoucher()), pageable, 1);
        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class),
                eqClass(Voucher.class))).thenReturn(List.of(webVoucher()));
        when(mongoTemplate.count(any(org.springframework.data.mongodb.core.query.Query.class),
                eqClass(Voucher.class))).thenReturn(1L);

        Page<Voucher> result = service.getWebVouchers(pageable, "WEB", null);

        assertEquals(1, result.getTotalElements());
        verify(mongoTemplate).find(any(org.springframework.data.mongodb.core.query.Query.class),
                eqClass(Voucher.class));
    }

    private static Class<Voucher> eqClass(Class<Voucher> c) {
        return org.mockito.ArgumentMatchers.eq(c);
    }
}
