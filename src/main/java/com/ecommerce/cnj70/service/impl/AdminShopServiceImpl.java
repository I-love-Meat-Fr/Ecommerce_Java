package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.AuditSeverity;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AdminShopService;
import com.ecommerce.cnj70.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * TASK #24 — AdminShopService với AuditLog integration.
 *
 * Sau merge feature/vendors-module: signature gộp reason + adminUsername,
 * vừa set fields lên Shop (deactivationReason/rejectionReason/actionBy/actionAt),
 * vừa ghi AuditLog với adminUsername làm actor.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminShopServiceImpl implements AdminShopService {

    private final ShopRepository shopRepository;
    private final AuditLogService auditLogService;

    @Override
    public Page<Shop> listShops(Pageable pageable, String q) {
        return listShops(pageable, q, null);
    }

    @Override
    public Page<Shop> listShops(Pageable pageable, String q, ShopStatus status) {
        boolean hasQ = StringUtils.hasText(q);
        if (status == null) {
            if (!hasQ) {
                return shopRepository.findAll(pageable);
            }
            return shopRepository.findByShopNameContainingIgnoreCase(q.trim(), pageable);
        }
        if (!hasQ) {
            return shopRepository.findByStatus(status, pageable);
        }
        return shopRepository.findByStatusAndShopNameContainingIgnoreCase(status, q.trim(), pageable);
    }

    @Override
    public Shop getShopById(String id) {
        return shopRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shop", "id", id));
    }

    // ===== approveShop =====

    @Override
    public void approveShop(String id) {
        approveShop(id, null, null);
    }

    @Override
    public void approveShop(String id, String actorId, String actorUsername) {
        Shop shop = getShopById(id);
        ShopStatus beforeStatus = shop.getStatus();

        if (beforeStatus == ShopStatus.APPROVED) {
            log.info("AdminShopService.approveShop: shop {} already approved, skip", id);
            return;
        }

        shop.setStatus(ShopStatus.APPROVED);
        shop.setRejectionReason(null);
        shopRepository.save(shop);

        // ===== TASK #24: AuditLog =====
        if (actorId != null || actorUsername != null) {
            auditLogService.log(
                    AuditAction.SHOP_APPROVED,
                    "SHOP",
                    id,
                    actorId,
                    actorUsername,
                    "ADMIN",
                    AuditSeverity.INFO,
                    "Admin duyệt shop: " + shop.getShopName() + " (từ " + beforeStatus + " sang APPROVED)",
                    Map.of("beforeStatus", beforeStatus.name(), "afterStatus", "APPROVED")
            );
        }

        log.info("AdminShopService.approveShop: shop {} approved (was {})", id, beforeStatus);
    }

    // ===== activateShop =====

    @Override
    public void activateShop(String id) {
        activateShop(id, null, null);
    }

    @Override
    public void activateShop(String id, String actorId, String actorUsername) {
        Shop shop = getShopById(id);

        if (shop.isActive()) {
            throw new BusinessException("Shop đang ở trạng thái hoạt động, không thay đổi");
        }

        shop.setActive(true);
        shop.setDeactivationReason(null);
        shopRepository.save(shop);

        if (actorId != null || actorUsername != null) {
            auditLogService.logInfo(
                    AuditAction.SHOP_APPROVED,
                    "SHOP",
                    id,
                    actorId,
                    actorUsername,
                    "ADMIN",
                    "Admin kích hoạt shop: " + shop.getShopName()
            );
        }

        log.info("AdminShopService.activateShop: shop {} activated", id);
    }

    // ===== deactivateShop =====

    @Override
    public void deactivateShop(String id) {
        deactivateShop(id, null, null);
    }

    @Override
    public void deactivateShop(String id, String reason, String adminUsername) {
        Shop shop = getShopById(id);

        if (!shop.isActive()) {
            throw new BusinessException("Shop đang ở trạng thái ngừng hoạt động, không thay đổi");
        }

        shop.setActive(false);
        shop.setDeactivationReason(StringUtils.hasText(reason) ? reason : null);
        shop.setActionBy(adminUsername);
        shop.setActionAt(LocalDateTime.now());
        shopRepository.save(shop);

        if (adminUsername != null || reason != null) {
            auditLogService.logWarning(
                    AuditAction.SHOP_SUSPENDED,
                    "SHOP",
                    id,
                    null,
                    adminUsername,
                    "ADMIN",
                    "Admin ngừng hoạt động shop: " + shop.getShopName() +
                            (reason != null ? " - Lý do: " + reason : "")
            );
        }

        log.info("AdminShopService.deactivateShop: shop {} deactivated by {} - reason: {}",
                id, adminUsername, reason);
    }

    // ===== rejectShop =====

    @Override
    public void rejectShop(String id) {
        rejectShop(id, null, null);
    }

    @Override
    public void rejectShop(String id, String reason, String adminUsername) {
        Shop shop = getShopById(id);
        ShopStatus beforeStatus = shop.getStatus();

        if (beforeStatus == ShopStatus.REJECTED) {
            log.info("AdminShopService.rejectShop: shop {} already rejected, skip", id);
            return;
        }

        shop.setStatus(ShopStatus.REJECTED);
        shop.setRejectionReason(StringUtils.hasText(reason) ? reason : null);
        shop.setActionBy(adminUsername);
        shop.setActionAt(LocalDateTime.now());
        shopRepository.save(shop);

        if (adminUsername != null || reason != null) {
            auditLogService.logWarning(
                    AuditAction.SHOP_REJECTED,
                    "SHOP",
                    id,
                    null,
                    adminUsername,
                    "ADMIN",
                    "Admin từ chối shop: " + shop.getShopName() +
                            (reason != null ? " - Lý do: " + reason : "")
            );
        }

        log.info("AdminShopService.rejectShop: shop {} rejected by {} - reason: {}",
                id, adminUsername, reason);
    }
}
