package com.ecommerce.cnj70.service.automation;

import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

/**
 * Unit test cho {@link ProductModerationStateMachine}.
 *
 * <p>Cover:</p>
 * <ol>
 *   <li>Valid transitions (PENDING_AUTO → AUTO_PASSED / AUTO_REJECTED / PENDING_MANUAL).</li>
 *   <li>Invalid transitions (vd PENDING_AUTO → APPROVED) → throw BadRequest + ghi audit.</li>
 *   <li>Mutation của Product instance (status, moderationStatus, actorId, reason, at).</li>
 *   <li>Audit log ghi đúng action / severity.</li>
 *   <li>Enum canTransitionTo + allowedNextStates.</li>
 *   <li>State resolution từ ProductStatus fallback khi moderationStatus = null.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductModerationStateMachine - unit test")
class ProductModerationStateMachineTest {

    @Mock private AuditLogService auditLogService;
    private ProductModerationStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new ProductModerationStateMachine(auditLogService);
    }

    private Product productInState(ProductStatus status, ModerationStatus modStatus) {
        return Product.builder()
                .id("p-1")
                .name("Test product")
                .price(new java.math.BigDecimal("1000"))
                .shopId("s-1")
                .status(status)
                .moderationStatus(modStatus)
                .build();
    }

    // ===== Valid transitions =====

    @Test
    @DisplayName("PENDING_AUTO → AUTO_PASSED: set ACTIVE + AUTO_PASSED")
    void pendingAuto_to_autoPassed() {
        Product p = productInState(ProductStatus.PENDING_AUTO, ModerationStatus.PENDING_AUTO);

        stateMachine.transition(p, ProductModerationState.AUTO_PASSED, "SYSTEM",
                "All checks pass");

        assertThat(p.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(p.getModerationStatus()).isEqualTo(ModerationStatus.AUTO_PASSED);
        assertThat(p.getModerationActorId()).isEqualTo("SYSTEM");
        assertThat(p.getModerationReason()).isEqualTo("All checks pass");
        assertThat(p.getModerationAt()).isNotNull();

        verify(auditLogService, times(1)).log(
                eq(AuditAction.PRODUCT_AUTO_PASS),
                eq("PRODUCT"),
                eq("p-1"),
                anyString(), anyString(), anyString(),
                eq(AuditSeverity.INFO),
                anyString(),
                any());
    }

    @Test
    @DisplayName("PENDING_AUTO → AUTO_REJECTED: set REJECTED_AUTO + severity WARNING")
    void pendingAuto_to_autoRejected() {
        Product p = productInState(ProductStatus.PENDING_AUTO, ModerationStatus.PENDING_AUTO);

        stateMachine.transition(p, ProductModerationState.AUTO_REJECTED, "SYSTEM",
                "Blacklist keyword hit");

        assertThat(p.getStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);
        assertThat(p.getModerationStatus()).isEqualTo(ModerationStatus.AUTO_REJECTED);
        assertThat(p.getModerationReason()).isEqualTo("Blacklist keyword hit");

        verify(auditLogService, times(1)).log(
                eq(AuditAction.PRODUCT_AUTO_REJECT),
                eq("PRODUCT"),
                eq("p-1"),
                anyString(), anyString(), anyString(),
                eq(AuditSeverity.WARNING),
                anyString(),
                any());
    }

    @Test
    @DisplayName("PENDING_AUTO → PENDING_MANUAL: set MANUAL_REVIEW")
    void pendingAuto_to_pendingManual() {
        Product p = productInState(ProductStatus.PENDING_AUTO, ModerationStatus.PENDING_AUTO);

        stateMachine.transition(p, ProductModerationState.PENDING_MANUAL, "SYSTEM",
                "Price anomaly detected");

        assertThat(p.getStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
        assertThat(p.getModerationStatus()).isEqualTo(ModerationStatus.PENDING_MANUAL);

        verify(auditLogService, times(1)).log(
                eq(AuditAction.PRODUCT_MANUAL_REVIEW),
                eq("PRODUCT"),
                eq("p-1"),
                anyString(), anyString(), anyString(),
                eq(AuditSeverity.INFO),
                anyString(),
                any());
    }

    @Test
    @DisplayName("PENDING_MANUAL → APPROVED: Moderator approve")
    void pendingManual_to_approved() {
        Product p = productInState(ProductStatus.MANUAL_REVIEW, ModerationStatus.PENDING_MANUAL);

        stateMachine.transition(p, ProductModerationState.APPROVED, "moderator-1",
                "Looks OK after review");

        assertThat(p.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(p.getModerationStatus()).isEqualTo(ModerationStatus.APPROVED);
        assertThat(p.getModerationActorId()).isEqualTo("moderator-1");
    }

    @Test
    @DisplayName("PENDING_MANUAL → ESCALATED: Moderator escalate Admin")
    void pendingManual_to_escalated() {
        Product p = productInState(ProductStatus.MANUAL_REVIEW, ModerationStatus.PENDING_MANUAL);

        stateMachine.transition(p, ProductModerationState.ESCALATED, "moderator-1",
                "Cần Admin xem xét");

        assertThat(p.getStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
        assertThat(p.getModerationStatus()).isEqualTo(ModerationStatus.ESCALATED);
    }

    // ===== Invalid transitions =====

    @Test
    @DisplayName("PENDING_AUTO → APPROVED: KHÔNG hợp lệ → throw BadRequest + audit")
    void pendingAuto_to_approved_throws() {
        Product p = productInState(ProductStatus.PENDING_AUTO, ModerationStatus.PENDING_AUTO);

        assertThatThrownBy(() -> stateMachine.transition(p,
                ProductModerationState.APPROVED, "vendor-x", "Tự approve"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể chuyển");

        // Product KHÔNG bị mutate khi transition invalid
        assertThat(p.getStatus()).isEqualTo(ProductStatus.PENDING_AUTO);
        assertThat(p.getModerationStatus()).isEqualTo(ModerationStatus.PENDING_AUTO);

        // Audit ghi CRITICAL để admin điều tra
        verify(auditLogService, times(1)).log(
                eq(AuditAction.PRODUCT_AUTO_REJECT),
                eq("PRODUCT"),
                eq("p-1"),
                anyString(), anyString(), anyString(),
                eq(AuditSeverity.CRITICAL),
                anyString(),
                any());
    }

    @Test
    @DisplayName("APPROVED là terminal: không thể đi đâu tiếp")
    void approved_isTerminal() {
        Product p = productInState(ProductStatus.ACTIVE, ModerationStatus.APPROVED);

        for (ProductModerationState target : ProductModerationState.values()) {
            assertThatThrownBy(() -> stateMachine.transition(p, target, "x", "y"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Test
    @DisplayName("REJECTED là terminal: không thể đi đâu tiếp")
    void rejected_isTerminal() {
        Product p = productInState(ProductStatus.REJECTED_AUTO, ModerationStatus.REJECTED);

        for (ProductModerationState target : ProductModerationState.values()) {
            if (target == ProductModerationState.REJECTED) continue; // self-transition vẫn invalid
            assertThatThrownBy(() -> stateMachine.transition(p, target, "x", "y"))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    // ===== Audit metadata =====

    @Test
    @DisplayName("Audit log metadata chứa from/to states")
    void auditLog_metadataContainsFromTo() {
        Product p = productInState(ProductStatus.PENDING_AUTO, ModerationStatus.PENDING_AUTO);

        stateMachine.transition(p, ProductModerationState.AUTO_PASSED, "SYSTEM", "ok");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> metaCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);

        verify(auditLogService, times(1)).log(
                any(AuditAction.class),
                eq("PRODUCT"),
                eq("p-1"),
                anyString(), anyString(), anyString(),
                any(AuditSeverity.class),
                anyString(),
                metaCaptor.capture());

        assertThat(metaCaptor.getValue()).containsEntry("from", "PENDING_AUTO");
        assertThat(metaCaptor.getValue()).containsEntry("to", "AUTO_PASSED");
    }

    @Test
    @DisplayName("AuditLogService throw → transition vẫn thành công (non-fatal)")
    void auditLogService_throws_butTransitionSucceeds() {
        doThrow(new RuntimeException("audit system down"))
                .when(auditLogService).log(any(), anyString(), anyString(),
                        anyString(), anyString(), anyString(), any(), anyString(), any());

        Product p = productInState(ProductStatus.PENDING_AUTO, ModerationStatus.PENDING_AUTO);

        // Không throw — chỉ log warning.
        stateMachine.transition(p, ProductModerationState.AUTO_PASSED, "SYSTEM", "ok");

        assertThat(p.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(p.getModerationStatus()).isEqualTo(ModerationStatus.AUTO_PASSED);
    }

    // ===== Defensive =====

    @Test
    @DisplayName("Null product → IllegalArgumentException")
    void nullProduct_throws() {
        assertThatThrownBy(() -> stateMachine.transition(null,
                ProductModerationState.AUTO_PASSED, "SYSTEM", "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Null target → IllegalArgumentException")
    void nullTarget_throws() {
        Product p = productInState(ProductStatus.PENDING_AUTO, ModerationStatus.PENDING_AUTO);
        assertThatThrownBy(() -> stateMachine.transition(p, null, "SYSTEM", "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Enum self-check =====

    @Test
    @DisplayName("ProductModerationState.canTransitionTo / allowedNextStates")
    void enum_selfCheck() {
        assertThat(ProductModerationState.PENDING_AUTO.canTransitionTo(ProductModerationState.AUTO_PASSED)).isTrue();
        assertThat(ProductModerationState.PENDING_AUTO.canTransitionTo(ProductModerationState.APPROVED)).isFalse();
        assertThat(ProductModerationState.PENDING_AUTO.allowedNextStates())
                .containsExactlyInAnyOrder(
                        ProductModerationState.AUTO_PASSED,
                        ProductModerationState.AUTO_REJECTED,
                        ProductModerationState.PENDING_MANUAL);

        assertThat(ProductModerationState.APPROVED.allowedNextStates()).isEmpty();
        assertThat(ProductModerationState.REJECTED.allowedNextStates()).isEmpty();
    }

    @Test
    @DisplayName("ProductModerationState.toProductStatus mapping")
    void enum_toProductStatus() {
        assertThat(ProductModerationState.PENDING_AUTO.toProductStatus()).isEqualTo(ProductStatus.PENDING_AUTO);
        assertThat(ProductModerationState.AUTO_PASSED.toProductStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(ProductModerationState.APPROVED.toProductStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(ProductModerationState.AUTO_REJECTED.toProductStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);
        assertThat(ProductModerationState.REJECTED.toProductStatus()).isEqualTo(ProductStatus.REJECTED_AUTO);
        assertThat(ProductModerationState.PENDING_MANUAL.toProductStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
        assertThat(ProductModerationState.ESCALATED.toProductStatus()).isEqualTo(ProductStatus.MANUAL_REVIEW);
    }

    // ===== State resolution fallback =====

    @Test
    @DisplayName("State resolution: moderationStatus null → fallback theo ProductStatus")
    void stateResolution_fallback() {
        // Product có ProductStatus.ACTIVE nhưng chưa có moderationStatus (legacy data).
        Product p = productInState(ProductStatus.ACTIVE, null);

        // → resolved state = APPROVED (terminal) → mọi transition đều invalid.
        assertThatThrownBy(() -> stateMachine.transition(p,
                ProductModerationState.PENDING_MANUAL, "SYSTEM", "x"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("State resolution: HIDDEN status → fallback REJECTED (terminal)")
    void stateResolution_hiddenMapsToRejected() {
        Product p = productInState(ProductStatus.HIDDEN, null);

        assertThatThrownBy(() -> stateMachine.transition(p,
                ProductModerationState.APPROVED, "admin", "unhide"))
                .isInstanceOf(BadRequestException.class);
    }
}
