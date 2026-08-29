package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Shop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminShopService {

    Page<Shop> listShops(Pageable pageable, String q);

    Shop getShopById(String id);

    void approveShop(String id);

    void activateShop(String id);

    void deactivateShop(String id);

    void rejectShop(String id);
}
