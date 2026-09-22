package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminShopService {

    Page<Shop> listShops(Pageable pageable, String q);

    Page<Shop> listShops(Pageable pageable, String q, ShopStatus status);

    Shop getShopById(String id);

    void approveShop(String id);

    void activateShop(String id);

    void deactivateShop(String id, String reason, String adminUsername);

    void rejectShop(String id, String reason, String adminUsername);
}
