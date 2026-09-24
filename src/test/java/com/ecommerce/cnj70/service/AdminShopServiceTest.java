package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.ecommerce.cnj70.service.impl.AdminShopServiceImpl;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

    @InjectMocks private AdminShopServiceImpl adminShopService;

    @BeforeEach
    void setUp() {
        lenient().when(shopRepository.save(any(Shop.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("approveShop: PENDING → APPROVED")
    void approveShop_pendingToApproved() {
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        adminShopService.approveShop("shop-1");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
    }

    @Test
    @DisplayName("approveShop: APPROVED → APPROVED (idempotent, không throw)")
    void approveShop_alreadyApproved_idempotent() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        adminShopService.approveShop("shop-1"); // không throw

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.APPROVED);
    }

    @Test
    @DisplayName("approveShop: shop không tồn tại → throw ResourceNotFoundException")
    void approveShop_notFound_throws() {
        when(shopRepository.findById("shop-x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminShopService.approveShop("shop-x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("activateShop: inactive → active")
    void activateShop_inactiveToActive() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        shop.setActive(false);
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        adminShopService.activateShop("shop-1");

        assertThat(shop.isActive()).isTrue();
    }

    @Test
    @DisplayName("activateShop: shop đã active → throw BusinessException")
    void activateShop_alreadyActive_throws() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> adminShopService.activateShop("shop-1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("đang ở trạng thái hoạt động");
    }

    @Test
    @DisplayName("deactivateShop: active → inactive (với lý do)")
    void deactivateShop_activeToInactive() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

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
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        assertThatThrownBy(() -> adminShopService.deactivateShop("shop-1", "test", "admin"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ngừng hoạt động");
    }

    @Test
    @DisplayName("rejectShop: APPROVED → REJECTED (với lý do)")
    void rejectShop_approvedToRejected() {
        Shop shop = TestFixtures.approvedActiveShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        adminShopService.rejectShop("shop-1", "Vi phạm chính sách", "admin@test");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(shop.getRejectionReason()).isEqualTo("Vi phạm chính sách");
        assertThat(shop.getActionBy()).isEqualTo("admin@test");
    }

    @Test
    @DisplayName("rejectShop: REJECTED → REJECTED (idempotent)")
    void rejectShop_alreadyRejected_idempotent() {
        Shop shop = TestFixtures.rejectedShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        adminShopService.rejectShop("shop-1", "test", "admin"); // không throw

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
    }

    @Test
    @DisplayName("rejectShop: lưu lý do và admin thực hiện")
    void currentBehavior_noReasonStored() {
        // AdminShopService đã lưu reason + actionBy vào Shop document
        Shop shop = TestFixtures.pendingShop("shop-1", "v-1");
        when(shopRepository.findById("shop-1")).thenReturn(Optional.of(shop));

        adminShopService.rejectShop("shop-1", "Sai thông tin CCCD", "admin@cnj70.com");

        assertThat(shop.getStatus()).isEqualTo(ShopStatus.REJECTED);
        assertThat(shop.getRejectionReason()).isEqualTo("Sai thông tin CCCD");
        assertThat(shop.getActionBy()).isEqualTo("admin@cnj70.com");
    }
}
