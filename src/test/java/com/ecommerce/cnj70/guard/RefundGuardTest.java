package com.ecommerce.cnj70.guard;

import com.ecommerce.cnj70.document.RefundRequest;
import com.ecommerce.cnj70.document.ReturnRequest;
import com.ecommerce.cnj70.enums.RefundStatus;
import com.ecommerce.cnj70.enums.ReturnStatus;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.RefundRequestRepository;
import com.ecommerce.cnj70.repository.ReturnRequestRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.RefundProvider;
import com.ecommerce.cnj70.service.impl.RefundServiceImpl;
import com.ecommerce.cnj70.service.impl.ReturnServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phase 3A §31 — Refund/Return ownership isolation guard tests.
 *
 * <p>Đảm bảo Customer không xem được refund/return của Customer khác.
 * Rule <b>Done: Customer thấy trạng thái yêu cầu và kết quả</b> — nhưng chỉ
 * trong phạm vi của mình.</p>
 */
@DisplayName("Refund/Return Guard — Customer ownership isolation")
class RefundGuardTest {

    @Nested
    @DisplayName("Refund ownership")
    class RefundOwnership {

        @Test
        @DisplayName("Customer B không được xem Refund của Customer A")
        void customerB_cannotViewRefundOfCustomerA() {
            // Mock repositories
            RefundRequestRepository refundRepo = mock(RefundRequestRepository.class);
            OrderRepository orderRepo = mock(OrderRepository.class);
            RefundProvider refundProvider = mock(RefundProvider.class);

            RefundRequest refundA = RefundRequest.builder()
                    .id("refund-A")
                    .customerId("customer-A")
                    .shopId("shop-1")
                    .orderId("order-1")
                    .status(RefundStatus.SUCCEEDED)
                    .build();
            when(refundRepo.findById("refund-A")).thenReturn(Optional.of(refundA));

            RefundServiceImpl service = new RefundServiceImpl(refundRepo, orderRepo, refundProvider);

            // Customer B cố gắng xem refund của Customer A → phải throw UnauthorizedException
            CustomUserDetails customerB = buildCustomer("customer-B");
            assertThatThrownBy(() -> service.getById("refund-A", customerB))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Customer A được xem Refund của chính mình")
        void customerA_canViewOwnRefund() {
            RefundRequestRepository refundRepo = mock(RefundRequestRepository.class);
            OrderRepository orderRepo = mock(OrderRepository.class);
            RefundProvider refundProvider = mock(RefundProvider.class);

            RefundRequest refundA = RefundRequest.builder()
                    .id("refund-A")
                    .customerId("customer-A")
                    .shopId("shop-1")
                    .orderId("order-1")
                    .status(RefundStatus.SUCCEEDED)
                    .build();
            when(refundRepo.findById("refund-A")).thenReturn(Optional.of(refundA));
            when(refundRepo.save(any(RefundRequest.class))).thenAnswer(inv -> inv.getArgument(0));

            RefundServiceImpl service = new RefundServiceImpl(refundRepo, orderRepo, refundProvider);

            // Customer A xem refund của mình → thành công
            CustomUserDetails customerA = buildCustomer("customer-A");
            RefundRequest result = service.getById("refund-A", customerA);
            assert result != null;
            assert result.getId().equals("refund-A");
            assert result.getStatus() == RefundStatus.SUCCEEDED;
        }
    }

    @Nested
    @DisplayName("Return ownership")
    class ReturnOwnership {

        @Test
        @DisplayName("Customer B không được xem Return của Customer A")
        void customerB_cannotViewReturnOfCustomerA() {
            ReturnRequestRepository returnRepo = mock(ReturnRequestRepository.class);
            OrderRepository orderRepo = mock(OrderRepository.class);
            ShopRepository shopRepo = mock(ShopRepository.class);

            ReturnRequest returnA = ReturnRequest.builder()
                    .id("return-A")
                    .customerId("customer-A")
                    .shopId("shop-1")
                    .orderId("order-1")
                    .status(ReturnStatus.COMPLETED)
                    .build();
            when(returnRepo.findById("return-A")).thenReturn(Optional.of(returnA));

            ReturnServiceImpl service = new ReturnServiceImpl(returnRepo, orderRepo, shopRepo);

            // Customer B cố gắng xem return của Customer A → phải throw UnauthorizedException
            CustomUserDetails customerB = buildCustomer("customer-B");
            assertThatThrownBy(() -> service.getById("return-A", customerB))
                    .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        @DisplayName("Customer A được xem Return của chính mình")
        void customerA_canViewOwnReturn() {
            ReturnRequestRepository returnRepo = mock(ReturnRequestRepository.class);
            OrderRepository orderRepo = mock(OrderRepository.class);
            ShopRepository shopRepo = mock(ShopRepository.class);

            ReturnRequest returnA = ReturnRequest.builder()
                    .id("return-A")
                    .customerId("customer-A")
                    .shopId("shop-1")
                    .orderId("order-1")
                    .status(ReturnStatus.COMPLETED)
                    .build();
            when(returnRepo.findById("return-A")).thenReturn(Optional.of(returnA));

            ReturnServiceImpl service = new ReturnServiceImpl(returnRepo, orderRepo, shopRepo);

            // Customer A xem return của mình → thành công
            CustomUserDetails customerA = buildCustomer("customer-A");
            ReturnRequest result = service.getById("return-A", customerA);
            assert result != null;
            assert result.getId().equals("return-A");
            assert result.getStatus() == ReturnStatus.COMPLETED;
        }
    }

    // Helper để build CustomUserDetails giả lập cho Customer.
    private CustomUserDetails buildCustomer(String customerId) {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getId()).thenReturn(customerId);
        when(user.getRole()).thenReturn("CUSTOMER");
        when(user.isEnabled()).thenReturn(true);
        when(user.isAccountNonLocked()).thenReturn(true);
        return user;
    }
}
