package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminShopService {

    Page<Shop> listShops(Pageable pageable, String q);

    Page<Shop> listShops(Pageable pageable, String q, ShopStatus status);

    Shop getShopById(String id);

    // ===== approveShop =====
    void approveShop(String id);

    /** TASK #24: với AuditLog actor tracking */
    void approveShop(String id, String actorId, String actorUsername);

    // ===== activateShop =====
    void activateShop(String id);

    /** TASK #24: với AuditLog actor tracking */
    void activateShop(String id, String actorId, String actorUsername);

    // ===== deactivateShop =====
    /** Backward-compat: chỉ id. */
    void deactivateShop(String id);

    /**
     * TASK #24 + feature/vendors-module: deactivate kèm reason + adminUsername.
     * - Lưu deactivationReason, actionBy, actionAt lên Shop document (vendors-module).
     * - Ghi AuditLog SHOP_SUSPENDED với adminUsername làm actor (feature/admin).
     * - Nếu reason null, vẫn chấp nhận và set actionBy/actionAt cho audit trail.
     */
    void deactivateShop(String id, String reason, String adminUsername);

    // ===== rejectShop =====
    /** Backward-compat: chỉ id. */
    void rejectShop(String id);

    /**
     * TASK #24 + feature/vendors-module: reject kèm reason + adminUsername.
     * - Lưu rejectionReason, actionBy, actionAt lên Shop document (vendors-module).
     * - Ghi AuditLog SHOP_REJECTED với adminUsername làm actor (feature/admin).
     */
    void rejectShop(String id, String reason, String adminUsername);
}
