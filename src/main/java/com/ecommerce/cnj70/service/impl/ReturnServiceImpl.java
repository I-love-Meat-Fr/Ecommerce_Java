package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.ReturnRequest;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ReturnStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ReturnRequestRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 3A §23 — Return Service Implementation.
 *
 * <p>Scope giới hạn ở Phase 3A: Customer request Return + ownership-checked list/get.
 * Vendor approve/reject + Refund trigger thuộc Phase 3B.</p>
 *
 * <h3>Hard rules (Phase 3A)</h3>
 * <ul>
 *     <li>§26 — Không xóa Order/OrderItem/history. Return là lifecycle record bổ sung.</li>
 *     <li>§29-§30 — Không tự quyết refund amount. Chỉ lưu {@code declaredAmount} do Customer khai.</li>
 *     <li>§32 — Không tự phát minh workflow. Approve/Reject/Cancel thuộc Phase 3B.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRepository;
    private final OrderRepository orderRepository;
    private final ShopRepository shopRepository;

    @Override
    @Transactional
    public ReturnRequest requestReturn(String orderId,
                                        List<String> orderItemIds,
                                        String reason,
                                        List<String> evidence,
                                        CustomUserDetails user) {
        validateRole(user, UserRole.CUSTOMER);
        if (!StringUtils.hasText(orderId)) {
            throw new BadRequestException("orderId là bắt buộc");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("Lý do Return là bắt buộc");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Order"));
        if (!order.getUserId().equals(user.getId())) {
            throw new UnauthorizedException("Order không thuộc về Customer");
        }
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Shop"));

        // Phase 3A §11 — verify từng orderItemId (productId) thuộc Order.items.
        List<String> verifiedItemIds = new ArrayList<>();
        if (orderItemIds != null && !orderItemIds.isEmpty()) {
            Set<String> orderProductIds = order.getItems().stream()
                    .map(Order.OrderItem::getProductId)
                    .collect(Collectors.toSet());
            for (String itemId : orderItemIds) {
                if (itemId == null || itemId.isBlank()) {
                    throw new BadRequestException("orderItemIds chứa giá trị rỗng");
                }
                if (!orderProductIds.contains(itemId)) {
                    throw new BadRequestException(
                            "orderItemId '" + itemId + "' không thuộc Order " + order.getId());
                }
                verifiedItemIds.add(itemId);
            }
        }

        ReturnRequest rr = ReturnRequest.builder()
                .complaintId(null) // optional link — ComplaintId wired in Phase 3B workflow
                .orderId(order.getId())
                .orderItemIds(verifiedItemIds)
                .customerId(user.getId())
                .customerName(user.getFullName())
                .shopId(order.getShopId())
                .shopName(shop.getShopName())
                .vendorId(shop.getOwnerId())
                .reason(reason)
                .evidence(evidence != null ? evidence : new ArrayList<>())
                .status(ReturnStatus.REQUESTED)
                .build();
        ReturnRequest saved = returnRepository.save(rr);
        log.info("Return requested: id={} orderId={} customerId={}", saved.getId(), saved.getOrderId(), user.getId());
        return saved;
    }

    @Override
    public Page<ReturnRequest> listMyReturns(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.CUSTOMER);
        return returnRepository.findByCustomerId(user.getId(), pageable);
    }

    @Override
    public Page<ReturnRequest> listShopReturns(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.VENDOR);
        String shopId = user.getShopId();
        if (!StringUtils.hasText(shopId)) {
            return Page.empty(pageable);
        }
        return returnRepository.findByShopId(shopId, pageable);
    }

    @Override
    public ReturnRequest getById(String returnId, CustomUserDetails user) {
        if (!StringUtils.hasText(returnId)) {
            throw new BadRequestException("returnId không hợp lệ");
        }
        ReturnRequest rr = returnRepository.findById(returnId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Return: " + returnId));
        String role = user.getRole();
        if (UserRole.CUSTOMER.name().equals(role)) {
            if (!rr.getCustomerId().equals(user.getId())) {
                throw new UnauthorizedException("Return không thuộc Customer");
            }
        } else if (UserRole.VENDOR.name().equals(role)) {
            if (!rr.getShopId().equals(user.getShopId())) {
                throw new UnauthorizedException("Return không thuộc Shop của Vendor");
            }
        } else if (!UserRole.MODERATOR.name().equals(role)
                && !UserRole.ADMIN.name().equals(role)) {
            throw new UnauthorizedException("Role không hợp lệ");
        }
        return rr;
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
