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

    /** TASK #24: với AuditLog actor tracking */
    void approveShop(String id, String actorId, String actorUsername);

    void activateShop(String id);

    void activateShop(String id, String actorId, String actorUsername);

    void deactivateShop(String id);

    void deactivateShop(String id, String actorId, String actorUsername);

    void rejectShop(String id);

    /** TASK #24: với AuditLog actor tracking + reason */
    void rejectShop(String id, String actorId, String actorUsername, String reason);

    /**
     * Convenience overload cho test/dev: truyền {@code (id, reason, actorUsername)}
     * thay vì {@code (id, actorId, actorUsername, reason)} — actorId = null.
     * Không thay đổi business logic; chỉ bridge signature mismatch với tests cũ.
     */
    void rejectShop(String id, String reason, String actorUsername);
}
