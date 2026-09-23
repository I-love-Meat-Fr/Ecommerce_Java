package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.service.AdminShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminShopServiceImpl implements AdminShopService {

    private final ShopRepository shopRepository;

    @Override
    public Page<Shop> listShops(Pageable pageable, String q) {
        return listShops(pageable, q, null);
    }

    @Override
    public Page<Shop> listShops(Pageable pageable, String q, ShopStatus status) {
        boolean hasQ = StringUtils.hasText(q);
        // Phase 10: Status filter (null/ALL means no status filter applied)
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

    @Override
    public void approveShop(String id) {
        Shop shop = getShopById(id);

        if (shop.getStatus() == ShopStatus.APPROVED) {
            log.info("AdminShopService.approveShop: shop {} already approved, skip", id);
            return;
        }

        shop.setStatus(ShopStatus.APPROVED);
        shop.setRejectionReason(null);
        shopRepository.save(shop);
        log.info("AdminShopService.approveShop: shop {} approved (was {})", id, shop.getStatus());
    }

    @Override
    public void activateShop(String id) {
        Shop shop = getShopById(id);

        if (shop.isActive()) {
            throw new BusinessException("Shop đang ở trạng thái hoạt động, không thay đổi");
        }

        shop.setActive(true);
        shop.setDeactivationReason(null);
        shopRepository.save(shop);
        log.info("AdminShopService.activateShop: shop {} activated", id);
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
        log.info("AdminShopService.deactivateShop: shop {} deactivated by {} - reason: {}",
                id, adminUsername, reason);
    }

    @Override
    public void rejectShop(String id, String reason, String adminUsername) {
        Shop shop = getShopById(id);

        if (shop.getStatus() == ShopStatus.REJECTED) {
            log.info("AdminShopService.rejectShop: shop {} already rejected, skip", id);
            return;
        }

        shop.setStatus(ShopStatus.REJECTED);
        shop.setRejectionReason(StringUtils.hasText(reason) ? reason : null);
        shop.setActionBy(adminUsername);
        shop.setActionAt(LocalDateTime.now());
        shopRepository.save(shop);
        log.info("AdminShopService.rejectShop: shop {} rejected by {} - reason: {}",
                id, adminUsername, reason);
    }
}
