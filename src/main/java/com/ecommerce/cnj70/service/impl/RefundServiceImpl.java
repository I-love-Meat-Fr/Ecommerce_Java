package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.RefundRequest;
import com.ecommerce.cnj70.enums.RefundStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.RefundRequestRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.RefundProvider;
import com.ecommerce.cnj70.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Phase 3A §28 — Refund Service Implementation.
 *
 * <p>Persists {@link RefundRequest} rồi delegate sang {@link RefundProvider}.
 * KHÔNG tự quyết amount; KHÔNG tự cộng balance (Phase 3A §29).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRequestRepository refundRepository;
    private final OrderRepository orderRepository;
    private final RefundProvider refundProvider;

    @Override
    @Transactional
    public RefundRequest requestRefund(String complaintId, String returnId, String orderId,
                                        BigDecimal declaredAmount, CustomUserDetails user) {
        if (user == null) {
            throw new UnauthorizedException("Yêu cầu đăng nhập");
        }
        if (!StringUtils.hasText(orderId)) {
            throw new BadRequestException("orderId là bắt buộc");
        }
        // Phase 3A §31 — Refund phải gắn với ít nhất 1 business event (Complaint/Return/Order).
        if (!StringUtils.hasText(complaintId)
                && !StringUtils.hasText(returnId)
                && !StringUtils.hasText(orderId)) {
            throw new BadRequestException("Refund phải gắn với Complaint, Return hoặc Order");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Order"));

        // Ownership — Customer chỉ được refund Order của mình.
        if (UserRole.CUSTOMER.name().equals(user.getRole())
                && !order.getUserId().equals(user.getId())) {
            throw new UnauthorizedException("Order không thuộc về Customer");
        }
        // Vendor chỉ refund Order của Shop mình.
        if (UserRole.VENDOR.name().equals(user.getRole())
                && !order.getShopId().equals(user.getShopId())) {
            throw new UnauthorizedException("Order không thuộc Shop của Vendor");
        }

        // Phase 3A §30 — declaredAmount có thể null; Provider sẽ quyết.
        BigDecimal amountToUse = declaredAmount;
        if (amountToUse == null) {
            amountToUse = refundProvider.deriveDeclaredAmount(orderId);
        }

        RefundRequest rr = RefundRequest.builder()
                .complaintId(complaintId)
                .returnId(returnId)
                .orderId(orderId)
                .customerId(order.getUserId())
                .shopId(order.getShopId())
                .vendorId(null)
                .declaredAmount(amountToUse)
                .status(RefundStatus.REQUESTED)
                .requestedById(user.getId())
                .requestedByEmail(user.getEmail())
                .requestedByRole(user.getRole())
                .build();
        RefundRequest saved = refundRepository.save(rr);
        log.info("Refund requested: id={} orderId={} amount={} byRole={}",
                saved.getId(), saved.getOrderId(), saved.getDeclaredAmount(), user.getRole());

        // Delegate to provider — log-only by default; production provider replaces this.
        RefundRequest afterSubmit = refundProvider.submit(saved);
        return refundRepository.save(afterSubmit);
    }

    @Override
    public Page<RefundRequest> listMyRefunds(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.CUSTOMER);
        return refundRepository.findByCustomerId(user.getId(), pageable);
    }

    @Override
    public Page<RefundRequest> listShopRefunds(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.VENDOR);
        String shopId = user.getShopId();
        if (!StringUtils.hasText(shopId)) {
            return Page.empty(pageable);
        }
        return refundRepository.findByShopId(shopId, pageable);
    }

    @Override
    public RefundRequest getById(String refundId, CustomUserDetails user) {
        if (!StringUtils.hasText(refundId)) {
            throw new BadRequestException("refundId không hợp lệ");
        }
        RefundRequest rr = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Refund: " + refundId));
        String role = user.getRole();
        if (UserRole.CUSTOMER.name().equals(role)) {
            if (!rr.getCustomerId().equals(user.getId())) {
                throw new UnauthorizedException("Refund không thuộc Customer");
            }
        } else if (UserRole.VENDOR.name().equals(role)) {
            if (!rr.getShopId().equals(user.getShopId())) {
                throw new UnauthorizedException("Refund không thuộc Shop của Vendor");
            }
        } else if (!UserRole.MODERATOR.name().equals(role)
                && !UserRole.ADMIN.name().equals(role)) {
            throw new UnauthorizedException("Role không hợp lệ");
        }
        return rr;
    }

    @Override
    public RefundRequest pollStatus(String refundId, CustomUserDetails user) {
        RefundRequest rr = getById(refundId, user);
        if (rr.getStatus() == RefundStatus.SUCCEEDED
                || rr.getStatus() == RefundStatus.FAILED
                || rr.getStatus() == RefundStatus.CANCELLED) {
            // Terminal — không poll.
            return rr;
        }
        RefundRequest polled = refundProvider.pollStatus(rr);
        return refundRepository.save(polled);
    }

    private void validateRole(CustomUserDetails user, UserRole required) {
        if (user == null) {
            throw new UnauthorizedException("Yêu cầu đăng nhập");
        }
        if (!required.name().equals(user.getRole())) {
            throw new UnauthorizedException("Yêu cầu role " + required + ", hiện tại: " + user.getRole());
        }
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Tài khoản đã bị vô hiệu hóa");
        }
        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedException("Tài khoản đã bị khóa");
        }
    }
}
