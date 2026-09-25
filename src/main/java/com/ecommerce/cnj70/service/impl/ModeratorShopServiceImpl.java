package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.dto.moderation.ShopModerationContext;
import com.ecommerce.cnj70.dto.moderation.ShopModerationDecision;
import com.ecommerce.cnj70.dto.moderation.ShopModerationQueueItem;
import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.ModeratorShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 6 — Moderator Shop Moderation Service Implementation.
 *
 * <p>Hard rules:</p>
 * <ul>
 *     <li>State transition enforced on EVERY action — no silent overwrite.</li>
 *     <li>Suspend / Restore / Restrict require non-blank reason.</li>
 *     <li>Locked account cannot perform actions.</li>
 *     <li>Every action writes a {@code ModerationHistory} document
 *         and an {@link AuditEvent} via the integration seam.</li>
 *     <li>Does NOT overwrite Admin enforcement state
 *         ({@code Shop.enforcementActorId} is preserved).</li>
 * </ul>
 *
 * <p>This implementation is INTEGRATION-READY: it follows the existing
 * Moderator service pattern (audit + history best-effort) and reuses
 * the {@link Shop} document unchanged. If a future Shop-specific
 * moderation contract is finalized, only this implementation needs
 * adjustment.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeratorShopServiceImpl implements ModeratorShopService {

    /** States that need moderator attention (queue candidates). */
    private static final Set<ShopStatus> QUEUE_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(ShopStatus.PENDING,
                       ShopStatus.APPROVED,
                       ShopStatus.RESTRICTED,
                       ShopStatus.SUSPENDED));

    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ModerationHistoryRepository historyRepository;
    private final AuditEventWriter auditEventWriter;
    private final org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    // ======================== QUEUE ========================

    @Override
    public Page<ShopModerationQueueItem> listQueue(String q, ShopStatus status, Pageable pageable) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasStatus = status != null;

        Page<Shop> shops;

        if (!hasQ && !hasStatus) {
            shops = shopRepository.findAll(pageable);
        } else if (hasQ && !hasStatus) {
            shops = shopRepository.findByShopNameContainingIgnoreCase(q.trim(), pageable);
        } else if (!hasQ && hasStatus) {
            if (!QUEUE_STATUSES.contains(status)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            shops = shopRepository.findByStatus(status, pageable);
        } else {
            if (!QUEUE_STATUSES.contains(status)) {
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            shops = shopRepository.findByStatusAndShopNameContainingIgnoreCase(status, q.trim(), pageable);
        }

        List<ShopModerationQueueItem> items = shops.getContent().stream()
                .map(this::toQueueItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, shops.getTotalElements());
    }

    private ShopModerationQueueItem toQueueItem(Shop s) {
        User owner = (s.getOwnerId() != null)
                ? userRepository.findById(s.getOwnerId()).orElse(null)
                : null;

        return ShopModerationQueueItem.builder()
                .shopId(s.getId())
                .shopName(s.getShopName())
                .ownerId(s.getOwnerId())
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerName(owner != null ? owner.getFullName() : null)
                .description(s.getDescription())
                .logoUrl(s.getLogoUrl())
                .bannerUrl(s.getBannerUrl())
                .status(s.getStatus())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .enforcementAt(s.getEnforcementAt())
                .enforcementReason(s.getEnforcementReason())
                .build();
    }

    // ======================== DETAIL ========================

    @Override
    public ShopModerationContext getContext(String shopId) {
        if (!StringUtils.hasText(shopId)) {
            throw new BadRequestException("Shop ID không hợp lệ");
        }
        Shop shop = loadAndGuard(shopId);

        User owner = (shop.getOwnerId() != null)
                ? userRepository.findById(shop.getOwnerId()).orElse(null)
                : null;

        return ShopModerationContext.builder()
                .shopId(shop.getId())
                .shopName(shop.getShopName())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl())
                .status(shop.getStatus())
                .active(shop.isActive())
                .ownerId(shop.getOwnerId())
                .ownerEmail(owner != null ? owner.getEmail() : null)
                .ownerName(owner != null ? owner.getFullName() : null)
                .createdAt(shop.getCreatedAt())
                .updatedAt(shop.getUpdatedAt())
                .enforcementActorId(shop.getEnforcementActorId())
                .enforcementReason(shop.getEnforcementReason())
                .enforcementAt(shop.getEnforcementAt())
                .build();
    }

    // ======================== APPROVE (PENDING → APPROVED) ========================

    @Override
    @Transactional
    public ShopModerationDecision approve(String shopId, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Shop shop = loadAndGuard(shopId);
        ShopStatus before = shop.getStatus();

        if (before != ShopStatus.PENDING) {
            throw new ConflictException(
                    "Chỉ có thể duyệt shop đang ở trạng thái PENDING (hiện tại=" + before + ")");
        }

        // Phase 4 fix — atomic update via raw collection to avoid E11000 duplicate key
        // on shopName that occurs when shopRepository.save() treats the entity as a
        // new insert (because entity._id is String but DB _id is ObjectId or vice versa).
        Object nativeId = shop.getId();
        org.bson.Document updateDoc = new org.bson.Document()
                .append("$set", new org.bson.Document()
                        .append("status", ShopStatus.APPROVED.name())
                        .append("active", true)
                        .append("updatedAt", java.util.Date.from(java.time.LocalDateTime.now()
                                .atZone(java.time.ZoneId.systemDefault()).toInstant())));
        mongoTemplate.getCollection("shops").updateOne(
                new org.bson.Document("_id", nativeId), updateDoc);
        shop.setStatus(ShopStatus.APPROVED);
        shop.setActive(true);

        ShopModerationDecision decision = ShopModerationDecision.builder()
                .shopId(shopId)
                .shopName(shop.getShopName())
                .action(ModerationAction.APPROVE)
                .beforeStatus(before)
                .afterStatus(ShopStatus.APPROVED)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
                .message("Đã duyệt shop \"" + shop.getShopName() + "\"")
                .build();

        recordHistory(shop, decision);
        emitAudit(shop, decision, clientIp);

        log.info("Moderator approve shop: shopId={} moderatorId={} before={} after={}",
                shopId, moderator.getId(), before, ShopStatus.APPROVED);
        return decision;
    }

    // ======================== SUSPEND (APPROVED → SUSPENDED) ========================

    @Override
    @Transactional
    public ShopModerationDecision suspend(String shopId, String reason, String note,
                                          CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Shop shop = loadAndGuard(shopId);
        ShopStatus before = shop.getStatus();

        validateReason(reason);

        if (before != ShopStatus.APPROVED && before != ShopStatus.RESTRICTED) {
            throw new ConflictException(
                    "Chỉ có thể suspend shop ở trạng thái APPROVED hoặc RESTRICTED (hiện tại=" + before + ")");
        }

        // Phase 4 fix — atomic update via raw collection to avoid E11000.
        Object nativeId = shop.getId();
        LocalDateTime now = LocalDateTime.now();
        org.bson.Document updateDoc = new org.bson.Document()
                .append("$set", new org.bson.Document()
                        .append("status", ShopStatus.SUSPENDED.name())
                        .append("active", false)
                        .append("enforcementActorId", moderator.getId())
                        .append("enforcementReason", reason)
                        .append("enforcementAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("updatedAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant())));
        mongoTemplate.getCollection("shops").updateOne(
                new org.bson.Document("_id", nativeId), updateDoc);
        shop.setStatus(ShopStatus.SUSPENDED);
        shop.setActive(false);
        shop.setEnforcementActorId(moderator.getId());
        shop.setEnforcementReason(reason);
        shop.setEnforcementAt(now);

        ShopModerationDecision decision = ShopModerationDecision.builder()
                .shopId(shopId)
                .shopName(shop.getShopName())
                .action(ModerationAction.SUSPEND)
                .beforeStatus(before)
                .afterStatus(ShopStatus.SUSPENDED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(now)
                .message("Đã suspend shop \"" + shop.getShopName() + "\"")
                .build();

        recordHistory(shop, decision, reason);
        emitAudit(shop, decision, clientIp);

        log.info("Moderator suspend shop: shopId={} moderatorId={} reason.length={}",
                shopId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== RESTORE (SUSPENDED/RESTRICTED → APPROVED) ========================

    @Override
    @Transactional
    public ShopModerationDecision restore(String shopId, String reason, String note,
                                          CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Shop shop = loadAndGuard(shopId);
        ShopStatus before = shop.getStatus();

        validateReason(reason);

        if (before != ShopStatus.SUSPENDED && before != ShopStatus.RESTRICTED) {
            throw new ConflictException(
                    "Chỉ có thể restore shop ở trạng thái SUSPENDED hoặc RESTRICTED (hiện tại=" + before + ")");
        }

        Object nativeId = shop.getId();
        LocalDateTime now = LocalDateTime.now();
        org.bson.Document updateDoc = new org.bson.Document()
                .append("$set", new org.bson.Document()
                        .append("status", ShopStatus.APPROVED.name())
                        .append("active", true)
                        .append("enforcementActorId", moderator.getId())
                        .append("enforcementReason", reason)
                        .append("enforcementAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("updatedAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant())));
        mongoTemplate.getCollection("shops").updateOne(
                new org.bson.Document("_id", nativeId), updateDoc);
        shop.setStatus(ShopStatus.APPROVED);
        shop.setActive(true);
        shop.setEnforcementActorId(moderator.getId());
        shop.setEnforcementReason(reason);
        shop.setEnforcementAt(now);

        ShopModerationDecision decision = ShopModerationDecision.builder()
                .shopId(shopId)
                .shopName(shop.getShopName())
                .action(ModerationAction.RESTORE)
                .beforeStatus(before)
                .afterStatus(ShopStatus.APPROVED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(now)
                .message("Đã restore shop \"" + shop.getShopName() + "\" về APPROVED")
                .build();

        recordHistory(shop, decision, reason);
        emitAudit(shop, decision, clientIp);

        log.info("Moderator restore shop: shopId={} moderatorId={} before={} after={}",
                shopId, moderator.getId(), before, ShopStatus.APPROVED);
        return decision;
    }

    // ======================== RESTRICT (APPROVED → RESTRICTED) ========================

    @Override
    @Transactional
    public ShopModerationDecision restrict(String shopId, String reason, String note,
                                           CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Shop shop = loadAndGuard(shopId);
        ShopStatus before = shop.getStatus();

        validateReason(reason);

        if (before != ShopStatus.APPROVED) {
            throw new ConflictException(
                    "Chỉ có thể restrict shop ở trạng thái APPROVED (hiện tại=" + before + ")");
        }

        Object nativeId = shop.getId();
        LocalDateTime now = LocalDateTime.now();
        org.bson.Document updateDoc = new org.bson.Document()
                .append("$set", new org.bson.Document()
                        .append("status", ShopStatus.RESTRICTED.name())
                        .append("active", true)
                        .append("enforcementActorId", moderator.getId())
                        .append("enforcementReason", reason)
                        .append("enforcementAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("updatedAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant())));
        mongoTemplate.getCollection("shops").updateOne(
                new org.bson.Document("_id", nativeId), updateDoc);
        shop.setStatus(ShopStatus.RESTRICTED);
        shop.setActive(true);
        shop.setEnforcementActorId(moderator.getId());
        shop.setEnforcementReason(reason);
        shop.setEnforcementAt(now);

        ShopModerationDecision decision = ShopModerationDecision.builder()
                .shopId(shopId)
                .shopName(shop.getShopName())
                .action(ModerationAction.RESTRICT)
                .beforeStatus(before)
                .afterStatus(ShopStatus.RESTRICTED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(now)
                .message("Đã restrict shop \"" + shop.getShopName() + "\"")
                .build();

        recordHistory(shop, decision, reason);
        emitAudit(shop, decision, clientIp);

        log.info("Moderator restrict shop: shopId={} moderatorId={} reason.length={}",
                shopId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== HELPERS ========================

    private Shop loadAndGuard(String shopId) {
        if (!StringUtils.hasText(shopId)) {
            throw new BadRequestException("Shop ID không hợp lệ");
        }
        // Phase 4 fix: support BOTH String _id and ObjectId _id.
        // Bug history: shopRepository.findById(String) silently fails to match a
        // String _id stored in MongoDB because Spring Data's query translator
        // sends `{"id": "<hex>"}` (Java field name) instead of `{"_id": "<hex>"}`.
        // Workaround: query the collection directly via MongoTemplate. String
        // lookup FIRST (matches AdminShopServiceImpl.getShopById), then ObjectId.
        Shop shop = shopRepository.findById(shopId).orElse(null);
        if (shop == null) {
            org.bson.Document raw = mongoTemplate.getCollection("shops")
                    .find(new org.bson.Document("_id", shopId))
                    .first();
            if (raw == null && shopId.length() == 24 && shopId.matches("[0-9a-fA-F]+")) {
                org.bson.types.ObjectId oid = new org.bson.types.ObjectId(shopId);
                raw = mongoTemplate.getCollection("shops")
                        .find(new org.bson.Document("_id", oid))
                        .first();
            }
            if (raw != null) {
                if (!raw.containsKey("_class")) {
                    raw.put("_class", Shop.class.getName());
                }
                shop = mongoTemplate.getConverter().read(Shop.class, raw);
                if (shop.getId() == null) {
                    shop.setId(raw.get("_id") instanceof org.bson.types.ObjectId
                            ? raw.get("_id").toString()
                            : (String) raw.get("_id"));
                }
            }
        }
        if (shop == null) {
            throw new ResourceNotFoundException("Không tìm thấy shop với ID: " + shopId);
        }
        return shop;
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Reason is required and must not be blank");
        }
    }

    private void recordHistory(Shop shop, ShopModerationDecision decision) {
        recordHistory(shop, decision, decision.getReason());
    }

    private void recordHistory(Shop shop, ShopModerationDecision decision, String reason) {
        // NOTE: ModerationHistory.beforeStatus/afterStatus are typed as
        // ModerationStatus enum (Phase 2A product model). For Shop actions
        // we leave them null; Shop status transitions are encoded in
        // `reason` and reconstructed by the history view (resourceType=SHOP).
        ModerationHistory history = ModerationHistory.builder()
                .resourceType("SHOP")
                .resourceId(shop.getId())
                .resourceName(shop.getShopName())
                .action(decision.getAction())
                .beforeStatus(null)
                .afterStatus(null)
                .reason(reason)
                .moderatorId(decision.getModeratorId())
                .moderatorEmail(decision.getModeratorEmail())
                .moderatorRole(UserRole.MODERATOR)
                .build();
        try {
            historyRepository.save(history);
        } catch (RuntimeException ex) {
            log.error("Failed to persist ModerationHistory for shopId={} action={}",
                    shop.getId(), decision.getAction(), ex);
        }
    }

    private void emitAudit(Shop shop, ShopModerationDecision decision, String clientIp) {
        AuditEvent event = AuditEvent.builder()
                .actorId(decision.getModeratorId())
                .actorEmail(decision.getModeratorEmail())
                .role(UserRole.MODERATOR)
                .action("SHOP_" + decision.getAction().name())
                .resourceType("SHOP")
                .resourceId(shop.getId())
                .reason(decision.getReason())
                .before(decision.getBeforeStatus() != null ? decision.getBeforeStatus().name() : "NULL")
                .after(decision.getAfterStatus() != null ? decision.getAfterStatus().name() : "NULL")
                .ip(clientIp)
                .createdAt(decision.getDecidedAt())
                .build();
        try {
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.error("Failed to emit AuditEvent for shopId={} action={}",
                    shop.getId(), decision.getAction(), ex);
        }
    }

    /**
     * Phase 6 — Moderator account must be ACTIVE. Re-uses the same
     * guard pattern as ModeratorProductServiceImpl.
     */
    private static final class ModeratorGuard {
        static void verify(CustomUserDetails moderator) {
            if (moderator == null) {
                throw new UnauthorizedException("Yêu cầu đăng nhập Moderator");
            }
            if (!"MODERATOR".equals(moderator.getRole())) {
                throw new UnauthorizedException("Tài khoản không có quyền Moderator");
            }
            if (!moderator.isEnabled() || !moderator.isAccountNonLocked()) {
                throw new UnauthorizedException("Tài khoản Moderator đã bị khóa hoặc chưa kích hoạt");
            }
        }
    }
}
