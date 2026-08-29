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

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminShopServiceImpl implements AdminShopService {

    private final ShopRepository shopRepository;

    @Override
    public Page<Shop> listShops(Pageable pageable, String q) {
        if (!StringUtils.hasText(q)) {
            return shopRepository.findAll(pageable);
        }
        return shopRepository.findByShopNameContainingIgnoreCase(q.trim(), pageable);
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
        shopRepository.save(shop);
        log.info("AdminShopService.activateShop: shop {} activated", id);
    }

    @Override
    public void deactivateShop(String id) {
        Shop shop = getShopById(id);

        if (!shop.isActive()) {
            throw new BusinessException("Shop đang ở trạng thái ngừng hoạt động, không thay đổi");
        }

        shop.setActive(false);
        shopRepository.save(shop);
        log.info("AdminShopService.deactivateShop: shop {} deactivated", id);
    }

    @Override
    public void rejectShop(String id) {
        Shop shop = getShopById(id);

        if (shop.getStatus() == ShopStatus.REJECTED) {
            log.info("AdminShopService.rejectShop: shop {} already rejected, skip", id);
            return;
        }

        shop.setStatus(ShopStatus.REJECTED);
        shopRepository.save(shop);
        log.info("AdminShopService.rejectShop: shop {} rejected (was {})", id, shop.getStatus());
    }
}
