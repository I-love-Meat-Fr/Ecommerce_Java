package com.ecommerce.cnj70.fix.phase1;

import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.dto.request.VoucherFormReq;
import com.ecommerce.cnj70.enums.DiscountType;
import com.ecommerce.cnj70.enums.VoucherType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.VoucherRepository;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.impl.VoucherServiceImpl;
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
import org.mockito.Mockito;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho VoucherServiceImpl — Phase 1 (Voucher WEB Fix Contract §11.8).
 *
 * <p>Verify rules:</p>
 * <ul>
 *   <li>{@code createWebVoucher}: persisted to MongoDB with type=WEB forced, shopId=null.</li>
 *   <li>{@code createWebVoucher}: form missing type → service assigns type=WEB.</li>
 *   <li>{@code getWebVouchers(pageable, q, active)}: only returns type=WEB.</li>
 *   <li>{@code activateWebVoucher / deactivateWebVoucher}: status changes correctly.</li>
 *   <li>{@code deleteWebVoucher}: soft-delete (active=false).</li>
 *   <li>{@code createWebVoucher}: endDate in past / quantity zero → throw validation.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VoucherService WEB - Phase 1 fix verification")
class VoucherWebFixTest {

    @Mock private VoucherRepository voucherRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private AuditEventWriter auditEventWriter;

    @InjectMocks private VoucherServiceImpl voucherService;

    private VoucherFormReq validForm;

    @SuppressWarnings("unchecked")
    private void stubRawGetVoucherById(Voucher voucher) {
        Document raw = new Document("_id", voucher.getId())
                .append("code", voucher.getCode() != null ? voucher.getCode() : "")
                .append("type", voucher.getType() != null ? voucher.getType().name() : "WEB")
                .append("active", voucher.isActive())
                .append("quantity", voucher.getQuantity())
                .append("used", voucher.getUsed());

        MongoCollection<Document> coll = Mockito.mock(MongoCollection.class);
        FindIterable<Document> findIter = Mockito.mock(FindIterable.class);
        Mockito.lenient().when(mongoTemplate.getCollection("vouchers")).thenReturn(coll);
        Mockito.lenient().when(coll.find(any(Bson.class))).thenReturn(findIter);
        Mockito.lenient().when(findIter.first()).thenReturn(raw);
        MongoConverter converter = Mockito.mock(MongoConverter.class);
        Mockito.lenient().when(mongoTemplate.getConverter()).thenReturn(converter);
        Mockito.lenient().when(converter.read(eq(Voucher.class), eq(raw))).thenReturn(voucher);
    }

    @SuppressWarnings("unchecked")
    private void stubRawGetVoucherById_NotFound() {
        MongoCollection<Document> coll = Mockito.mock(MongoCollection.class);
        FindIterable<Document> findIter = Mockito.mock(FindIterable.class);
        Mockito.lenient().when(mongoTemplate.getCollection("vouchers")).thenReturn(coll);
        Mockito.lenient().when(coll.find(any(Bson.class))).thenReturn(findIter);
        Mockito.lenient().when(findIter.first()).thenReturn(null);
    }

    @BeforeEach
    void setUp() {
        validForm = VoucherFormReq.builder()
                .code("TESTWEB10")
                .name("Phase 1 WEB Voucher")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10"))
                .quantity(100)
                .minOrderValue(new BigDecimal("0"))
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .build();

        // Default save: echo + assign id
        when(voucherRepository.save(any(Voucher.class))).thenAnswer(inv -> {
            Voucher v = inv.getArgument(0);
            if (v.getId() == null) {
                v.setId("v-stub-id");
            }
            return v;
        });
    }

    // ============ §11.8 - createVoucher_webType_persistedToMongoDB ============

    @Test
    @DisplayName("createWebVoucher: valid form → persisted với type=WEB forced, shopId=null")
    void createVoucher_webType_persistedToMongoDB() {
        when(voucherRepository.existsByCode("TESTWEB10")).thenReturn(false);

        Voucher saved = voucherService.createWebVoucher(validForm, "admin-1");

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(captor.capture());

        Voucher persisted = captor.getValue();
        assertThat(persisted.getType()).isEqualTo(VoucherType.WEB);
        assertThat(persisted.getShopId()).isNull();
        assertThat(persisted.getShopName()).isNull();
        assertThat(persisted.getCode()).isEqualTo("TESTWEB10");
        assertThat(persisted.isActive()).isTrue();
        assertThat(persisted.getUsed()).isEqualTo(0);
        assertThat(saved.getType()).isEqualTo(VoucherType.WEB);
    }

    @Test
    @DisplayName("createWebVoucher: form missing type → service vẫn gán type=WEB")
    void createVoucher_formMissingTypeType_serviceAssignsWEB() {
        when(voucherRepository.existsByCode("TESTWEB10")).thenReturn(false);

        // Form KHÔNG có field type — DTO không có type, service phải force WEB
        VoucherFormReq formNoType = VoucherFormReq.builder()
                .code("TESTWEB10")
                .name("No type field")
                .discountType(DiscountType.AMOUNT)
                .discountValue(new BigDecimal("50000"))
                .quantity(50)
                .build();

        Voucher saved = voucherService.createWebVoucher(formNoType, "admin-1");

        assertThat(saved.getType()).isEqualTo(VoucherType.WEB);
        assertThat(saved.getShopId()).isNull();
    }

    @Test
    @DisplayName("createWebVoucher: duplicate code → throw ConflictException")
    void createVoucher_duplicateCode_throws() {
        when(voucherRepository.existsByCode("TESTWEB10")).thenReturn(true);

        assertThatThrownBy(() -> voucherService.createWebVoucher(validForm, "admin-1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("đã tồn tại");

        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    // ============ §11.8 - listVouchers_adminOnlyReturnsWEB ============

    @Test
    @DisplayName("getWebVouchers: chỉ trả về type=WEB")
    void listVouchers_adminOnlyReturnsWEB() {
        Voucher webVoucher = Voucher.builder()
                .id("v-web")
                .code("WEB1")
                .type(VoucherType.WEB)
                .active(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Voucher> page = new PageImpl<>(List.of(webVoucher), pageable, 1);
        when(voucherRepository.findByType(VoucherType.WEB, pageable)).thenReturn(page);

        Page<Voucher> result = voucherService.getWebVouchers(pageable, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(VoucherType.WEB);
        verify(voucherRepository).findByType(VoucherType.WEB, pageable);
    }

    @Test
    @DisplayName("getWebVouchers: filter active=true → repository.findByTypeAndActive")
    void listVouchers_adminFilterActive() {
        Voucher webVoucher = Voucher.builder()
                .id("v-web-1")
                .code("WEB1")
                .type(VoucherType.WEB)
                .active(true)
                .build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Voucher> page = new PageImpl<>(List.of(webVoucher), pageable, 1);
        when(voucherRepository.findByTypeAndActive(VoucherType.WEB, true, pageable)).thenReturn(page);

        Page<Voucher> result = voucherService.getWebVouchers(pageable, null, true);

        assertThat(result.getContent()).hasSize(1);
        verify(voucherRepository).findByTypeAndActive(VoucherType.WEB, true, pageable);
    }

    // ============ §11.8 - activateVoucher_changesStatus ============

    @Test
    @DisplayName("activateWebVoucher: WEB voucher inactive → active=true")
    void activateVoucher_changesStatus() {
        Voucher voucher = Voucher.builder()
                .id("v-1")
                .code("WEB1")
                .type(VoucherType.WEB)
                .quantity(100)
                .used(0)
                .active(false)
                .build();
        stubRawGetVoucherById(voucher);

        Voucher result = voucherService.activateWebVoucher("v-1");

        assertThat(result.isActive()).isTrue();
        verify(voucherRepository).save(any(Voucher.class));
    }

    @Test
    @DisplayName("activateWebVoucher: SHOP voucher → throw (admin không được activate SHOP)")
    void activateWebVoucher_shopVoucher_throws() {
        Voucher shopVoucher = Voucher.builder()
                .id("v-shop")
                .code("SHOP1")
                .type(VoucherType.SHOP)
                .quantity(100)
                .used(0)
                .active(false)
                .build();
        stubRawGetVoucherById(shopVoucher);

        assertThatThrownBy(() -> voucherService.activateWebVoucher("v-shop"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("WEB");
    }

    // ============ §11.8 - deactivateVoucher_changesStatus ============

    @Test
    @DisplayName("deactivateWebVoucher: WEB voucher active → active=false")
    void deactivateVoucher_changesStatus() {
        Voucher voucher = Voucher.builder()
                .id("v-1")
                .code("WEB1")
                .type(VoucherType.WEB)
                .quantity(100)
                .used(0)
                .active(true)
                .build();
        stubRawGetVoucherById(voucher);

        Voucher result = voucherService.deactivateWebVoucher("v-1");

        assertThat(result.isActive()).isFalse();
        verify(voucherRepository).save(any(Voucher.class));
    }

    // ============ §11.8 - deleteVoucher_removesFromMongoDB ============

    @Test
    @DisplayName("deleteWebVoucher: WEB voucher → soft delete (active=false)")
    void deleteVoucher_removesFromMongoDB() {
        Voucher voucher = Voucher.builder()
                .id("v-1")
                .code("WEB1")
                .type(VoucherType.WEB)
                .quantity(100)
                .used(0)
                .active(true)
                .build();
        stubRawGetVoucherById(voucher);

        voucherService.deleteWebVoucher("v-1");

        ArgumentCaptor<Voucher> captor = ArgumentCaptor.forClass(Voucher.class);
        verify(voucherRepository).save(captor.capture());

        Voucher saved = captor.getValue();
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getType()).isEqualTo(VoucherType.WEB);
    }

    @Test
    @DisplayName("deleteWebVoucher: not found → throw ResourceNotFoundException")
    void deleteVoucher_notFound_throws() {
        stubRawGetVoucherById_NotFound();

        assertThatThrownBy(() -> voucherService.deleteWebVoucher("v-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ============ §11.8 - createVoucher_endDateInPast_throwValidation ============

    @Test
    @DisplayName("createWebVoucher: endDate < startDate → throw BadRequestException")
    void createVoucher_endDateInPast_throwValidation() {
        when(voucherRepository.existsByCode("PASTWEB")).thenReturn(false);

        VoucherFormReq badForm = VoucherFormReq.builder()
                .code("PASTWEB")
                .name("Past end date")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10"))
                .quantity(10)
                .startDate(LocalDateTime.now().plusDays(30))
                .endDate(LocalDateTime.now().plusDays(10))  // before startDate
                .build();

        assertThatThrownBy(() -> voucherService.createWebVoucher(badForm, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("kết thúc");

        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    // ============ §11.8 - createVoucher_quantityZero_throwValidation ============

    @Test
    @DisplayName("createWebVoucher: quantity = 0 → throw BadRequestException")
    void createVoucher_quantityZero_throwValidation() {
        when(voucherRepository.existsByCode("ZEROQTY")).thenReturn(false);

        VoucherFormReq badForm = VoucherFormReq.builder()
                .code("ZEROQTY")
                .name("Zero quantity")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("10"))
                .quantity(0)
                .build();

        assertThatThrownBy(() -> voucherService.createWebVoucher(badForm, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("lượt");

        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    @DisplayName("createWebVoucher: discountValue = 0 → throw BadRequestException")
    void createVoucher_discountValueZero_throwValidation() {
        when(voucherRepository.existsByCode("ZEROVALUE")).thenReturn(false);

        VoucherFormReq badForm = VoucherFormReq.builder()
                .code("ZEROVALUE")
                .name("Zero discount")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("0"))
                .quantity(10)
                .build();

        assertThatThrownBy(() -> voucherService.createWebVoucher(badForm, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("h");

        verify(voucherRepository, never()).save(any(Voucher.class));
    }

    @Test
    @DisplayName("createWebVoucher: PERCENT > 100 → throw BadRequestException")
    void createVoucher_percentOver100_throwValidation() {
        when(voucherRepository.existsByCode("OVER100")).thenReturn(false);

        VoucherFormReq badForm = VoucherFormReq.builder()
                .code("OVER100")
                .name("Over 100")
                .discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal("150"))
                .quantity(10)
                .build();

        assertThatThrownBy(() -> voucherService.createWebVoucher(badForm, "admin-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("100");

        verify(voucherRepository, never()).save(any(Voucher.class));
    }
}
