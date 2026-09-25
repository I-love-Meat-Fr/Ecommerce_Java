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
import org.springframework.data.mongodb.core.MongoTemplate;
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
    /**
     * Phase 4 — MongoTemplate dùng cho raw Document query để tránh
     * {@code shopRepository.findById(String)} không match với {@code _id} String
     * (giống bug đã fix cho AdminUser ở Phase 3 và nhiều chỗ khác trong project).
     */
    private final MongoTemplate mongoTemplate;

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
        if (!org.springframework.util.StringUtils.hasText(id)) {
            throw new ResourceNotFoundException("Shop", "id", id);
        }
        // Phase 4 hotfix — Hỗ trợ CẢ 2 kiểu _id (String lẫn ObjectId).
        // Một số shop trong DB có _id là ObjectId (do import qua Compass/script),
        // Spring Data findById(String) sẽ không match → trả null → 404.
        // Phase 2 critical fix: KHÔNG ép `_id` raw.put("_id", id) vì sẽ làm
        // MongoDB save() insert document mới (với _id String) thay vì update
        // document cũ (với _id ObjectId) → E11000 duplicate key trên shopName.
        // Giữ nguyên native type của _id (ObjectId hoặc String) là cách duy nhất
        // để shopRepository.save(shop) hoạt động đúng.
        org.bson.Document raw = mongoTemplate.getCollection("shops")
                .find(new org.bson.Document("_id", id))
                .first();
        if (raw == null && id.length() == 24 && id.matches("[0-9a-fA-F]+")) {
            org.bson.types.ObjectId oid = new org.bson.types.ObjectId(id);
            raw = mongoTemplate.getCollection("shops")
                    .find(new org.bson.Document("_id", oid))
                    .first();
        }
        if (raw == null) {
            throw new ResourceNotFoundException("Shop", "id", id);
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", Shop.class.getName());
        }
        Shop shop = mongoTemplate.getConverter().read(Shop.class, raw);
        if (shop.getId() == null) {
            // Preserve native type: nếu _id là ObjectId, set String hex; nếu String,
            // giữ String. shop.getId() tự nhận diện qua converter.
            shop.setId(raw.get("_id") instanceof org.bson.types.ObjectId
                    ? raw.get("_id").toString()
                    : (String) raw.get("_id"));
        }
        return shop;
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

        // Phase 2 critical fix: tránh E11000 duplicate key trên shopName do
        // shopRepository.save() insert document mới khi entity.id là String
        // nhưng DB lưu _id là ObjectId. Dùng atomic update trực tiếp trên
        // collection (bypass entity mapping + _class discriminator).
        Object nativeId = resolveIdForQuery(id, shop);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("status", ShopStatus.APPROVED.name())
                        .append("rejectionReason", null)
                        .append("updatedAt", java.util.Date.from(java.time.LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("shops")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        if (matched == 0) {
            log.warn("AdminShopService.approveShop: shop {} not found by atomic update, falling back", id);
            shop.setStatus(ShopStatus.APPROVED);
            shop.setRejectionReason(null);
            shopRepository.save(shop);
        }

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

        // Phase 2 critical fix: atomic update tránh E11000 (xem approveShop).
        Object nativeId = resolveIdForQuery(id, shop);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("active", true)
                        .append("deactivationReason", null)
                        .append("updatedAt", java.util.Date.from(java.time.LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("shops")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        if (matched == 0) {
            log.warn("AdminShopService.activateShop: shop {} not found by atomic update, falling back", id);
            shop.setActive(true);
            shop.setDeactivationReason(null);
            shopRepository.save(shop);
        }

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
<<<<<<< HEAD
    public void deactivateShop(String id, String actorId, String actorUsername) {
=======
    public void deactivateShop(String id, String reason, String adminUsername) {
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
        Shop shop = getShopById(id);

        if (!shop.isActive()) {
            throw new BusinessException("Shop đang ở trạng thái ngừng hoạt động, không thay đổi");
        }

<<<<<<< HEAD
        shop.setActive(false);
        // reason không nằm trong interface signature → clear lý do cũ, không set mới
        shop.setDeactivationReason(null);
        shop.setActionBy(actorUsername);
        shop.setActionAt(LocalDateTime.now());
        shopRepository.save(shop);
=======
        // Phase 2 critical fix: atomic update tránh E11000 (xem approveShop).
        Object nativeId = resolveIdForQuery(id, shop);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("active", false)
                        .append("deactivationReason", StringUtils.hasText(reason) ? reason : null)
                        .append("actionBy", adminUsername)
                        .append("actionAt", java.util.Date.from(LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("updatedAt", java.util.Date.from(LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("shops")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        if (matched == 0) {
            log.warn("AdminShopService.deactivateShop: shop {} not found by atomic update, falling back", id);
            shop.setActive(false);
            shop.setDeactivationReason(StringUtils.hasText(reason) ? reason : null);
            shop.setActionBy(adminUsername);
            shop.setActionAt(LocalDateTime.now());
            shopRepository.save(shop);
        }
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace

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

<<<<<<< HEAD
        log.info("AdminShopService.deactivateShop: shop {} deactivated", id);
    }

    @Override
    public void rejectShop(String id) {
        rejectShop(id, null, null, null);
    }

    @Override
    public void rejectShop(String id, String reason, String actorUsername) {
        // Convenience overload: bridge test signature (id, reason, actorUsername).
        // Production code dùng 4-arg overload với actorId đầy đủ.
        rejectShop(id, null, actorUsername, reason);
    }
=======
        log.info("AdminShopService.deactivateShop: shop {} deactivated by {} - reason: {}",
                id, adminUsername, reason);
    }

    // ===== rejectShop =====
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace

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

<<<<<<< HEAD
        shop.setStatus(ShopStatus.REJECTED);
        shop.setRejectionReason(StringUtils.hasText(reason) ? reason : null);
        shop.setActionBy(actorUsername);
        shop.setActionAt(LocalDateTime.now());
        shopRepository.save(shop);
=======
        // Phase 2 critical fix: atomic update tránh E11000 (xem approveShop).
        Object nativeId = resolveIdForQuery(id, shop);
        org.bson.Document update = new org.bson.Document("$set",
                new org.bson.Document("status", ShopStatus.REJECTED.name())
                        .append("rejectionReason", StringUtils.hasText(reason) ? reason : null)
                        .append("actionBy", adminUsername)
                        .append("actionAt", java.util.Date.from(LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("updatedAt", java.util.Date.from(LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        long matched = mongoTemplate.getCollection("shops")
                .updateOne(new org.bson.Document("_id", nativeId), update)
                .getMatchedCount();
        if (matched == 0) {
            log.warn("AdminShopService.rejectShop: shop {} not found by atomic update, falling back", id);
            shop.setStatus(ShopStatus.REJECTED);
            shop.setRejectionReason(StringUtils.hasText(reason) ? reason : null);
            shop.setActionBy(adminUsername);
            shop.setActionAt(LocalDateTime.now());
            shopRepository.save(shop);
        }
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace

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

<<<<<<< HEAD
        log.info("AdminShopService.rejectShop: shop {} rejected (was {})", id, beforeStatus);
=======
        log.info("AdminShopService.rejectShop: shop {} rejected by {} - reason: {}",
                id, adminUsername, reason);
    }

    /**
     * Phase 2 critical fix: trả về _id dạng native (ObjectId hoặc String) để
     * update query match đúng document trong DB, tránh E11000 duplicate key
     * do save() insert document mới khi entity._id là String hex.
     *
     * @param id  ID String từ URL (hex 24 char hoặc String)
     * @param shop  Shop entity đã load từ getShopById (chứa _id native type)
     * @return Object — ObjectId nếu DB lưu ObjectId, String nếu lưu String
     */
    private Object resolveIdForQuery(String id, Shop shop) {
        // Ưu tiên lấy native type từ entity; fallback theo id pattern.
        if (shop.getId() != null && shop.getId().length() == 24 && id.matches("[0-9a-fA-F]+")) {
            // Có thể là ObjectId — kiểm tra bằng cách query thử
            org.bson.Document raw = mongoTemplate.getCollection("shops")
                    .find(new org.bson.Document("_id", new org.bson.types.ObjectId(id)))
                    .first();
            if (raw != null) {
                return new org.bson.types.ObjectId(id);
            }
        }
        return id;
>>>>>>> 105fc32050ccebc9b92d94f41bbd9d97b7536ace
    }
}
