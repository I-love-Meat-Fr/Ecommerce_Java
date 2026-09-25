package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.dto.moderation.AutoModerationResult;
import com.ecommerce.cnj70.dto.moderation.ModerationDecision;
import com.ecommerce.cnj70.dto.moderation.ProductModerationContext;
import com.ecommerce.cnj70.dto.moderation.ProductModerationQueueItem;
import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.enums.ViolationSeverity;
import com.ecommerce.cnj70.enums.ViolationType;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.repository.ViolationRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.AutoModerationResultProvider;
import com.ecommerce.cnj70.service.ModeratorProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Phase 2A — Moderator Product Moderation Service Implementation.
 *
 * <p>Hard rules:</p>
 * <ul>
 *     <li>State transition enforced on EVERY action — no silent overwrite
 *         (Phase 2A §34 / §39)</li>
 *     <li>Reject / Escalate require non-blank reason (Phase 2A §30 / §32)</li>
 *     <li>Locked account cannot perform actions (Phase 2A §58)</li>
 *     <li>Every action writes a {@code ModerationHistory} document
 *         and an {@link AuditEvent} via the integration seam</li>
 *     <li>Auto Moderation result is read-only from the integration seam
 *         — never recomputed</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeratorProductServiceImpl implements ModeratorProductService {

    /** States that still need moderator attention (queue candidates). */
    private static final Set<ModerationStatus> QUEUE_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(ModerationStatus.PENDING_MANUAL,
                       ModerationStatus.AUTO_PASSED,
                       ModerationStatus.AUTO_REJECTED));

    private final ProductRepository productRepository;
    private final ModerationHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final MongoTemplate mongoTemplate;
    private final AutoModerationResultProvider autoModerationResultProvider;
    private final AuditEventWriter auditEventWriter;
    private final ViolationRepository violationRepository;

    // ======================== QUEUE ========================

    @Override
    public Page<ProductModerationQueueItem> listQueue(String q, ModerationStatus moderationStatus, Pageable pageable) {
        boolean hasQ = StringUtils.hasText(q);
        boolean hasStatus = moderationStatus != null;

        Page<Product> products;

        if (!hasQ && !hasStatus) {
            // No filter — all in-queue statuses
            products = productRepository.findByModerationStatusIn(QUEUE_STATUSES, pageable);
        } else if (hasQ && !hasStatus) {
            products = productRepository.findByNameContainingIgnoreCaseAndModerationStatusIn(
                    q.trim(), QUEUE_STATUSES, pageable);
        } else if (!hasQ && hasStatus) {
            // Filter by specific moderation status (capped to in-queue set for security)
            if (!QUEUE_STATUSES.contains(moderationStatus)) {
                // Asking for APPROVED/REJECTED/ESCALATED → empty page (not in queue)
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
            products = productRepository.findByModerationStatus(moderationStatus, pageable);
        } else {
            products = productRepository.findByNameContainingIgnoreCaseAndModerationStatusIn(
                    q.trim(), Collections.singleton(moderationStatus), pageable);
        }

        List<ProductModerationQueueItem> items = products.getContent().stream()
                .map(this::toQueueItem)
                .collect(Collectors.toList());

        return new PageImpl<>(items, pageable, products.getTotalElements());
    }

    private ProductModerationQueueItem toQueueItem(Product p) {
        // Resolve vendor info in batch — but here it's per-item. Acceptable for page sizes ≤ 50.
        User vendor = (p.getShopId() != null) ? null : userRepository.findById(p.getShopId()).orElse(null);
        // shopName comes embedded on Product already
        Optional<AutoModerationResult> autoResult = autoModerationResultProvider.getResult(p.getId());

        return ProductModerationQueueItem.builder()
                .productId(p.getId())
                .productName(p.getName())
                .shopId(p.getShopId())
                .shopName(p.getShopName())
                .vendorId(vendor != null ? vendor.getId() : null)
                .vendorName(vendor != null ? vendor.getFullName() : null)
                .categoryId(p.getCategoryId())
                .categoryName(p.getCategoryName())
                .price(p.getPrice())
                .productStatus(p.getStatus())
                .moderationStatus(p.getModerationStatus())
                .autoModerationResult(autoResult.orElse(null))
                .createdAt(p.getCreatedAt())
                .build();
    }

    // ======================== DETAIL ========================

    @Override
    public ProductModerationContext getContext(String productId) {
        if (!StringUtils.hasText(productId)) {
            throw new BadRequestException("Product ID không hợp lệ");
        }
        Product product = loadAndGuard(productId);

        // Shop context
        Shop shop = (product.getShopId() != null)
                ? shopRepository.findById(product.getShopId()).orElse(null)
                : null;

        // Vendor: look up by Shop.ownerId
        User vendor = (shop != null && shop.getOwnerId() != null)
                ? userRepository.findById(shop.getOwnerId()).orElse(null)
                : null;

        Optional<AutoModerationResult> autoResult = autoModerationResultProvider.getResult(product.getId());

        return ProductModerationContext.builder()
                .productId(product.getId())
                .productName(product.getName())
                .description(product.getDescription())
                .categoryId(product.getCategoryId())
                .categoryName(product.getCategoryName())
                .productStatus(product.getStatus())
                .moderationStatus(product.getModerationStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .shopId(product.getShopId())
                .shopName(product.getShopName())
                .shopStatus(shop != null ? shop.getStatus() : ShopStatus.PENDING)
                .vendorId(vendor != null ? vendor.getId() : null)
                .vendorEmail(vendor != null ? vendor.getEmail() : null)
                .vendorName(vendor != null ? vendor.getFullName() : null)
                .autoModerationResult(autoResult.orElse(null))
                .build();
    }

    // ======================== APPROVE ========================

    @Override
    @Transactional
    public ModerationDecision approve(String productId, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Product product = loadAndGuard(productId);
        ModerationStatus before = product.getModerationStatus();

        // Concurrency guard — only PENDING_MANUAL/AUTO_PASSED/AUTO_REJECTED can be approved
        if (before == null || !QUEUE_STATUSES.contains(before)) {
            throw new ConflictException(
                    "Sản phẩm đã ở trạng thái cuối hoặc không thể duyệt (status=" + before + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        // Phase 4 fix: use atomic MongoDB updateOne to avoid Spring Data save() inserting
        // a duplicate ObjectId document when native _id is String. The native _id type
        // is preserved here.
        Object nativeId = resolveNativeId("products", product.getId());
        org.bson.Document updateDoc = new org.bson.Document()
                .append("$set", new org.bson.Document()
                        .append("moderationStatus", ModerationStatus.APPROVED.name())
                        .append("status", ProductStatus.ACTIVE.name())
                        .append("moderationActorId", moderator.getId())
                        .append("moderationAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("moderationReason", null)
                        .append("updatedAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant())));
        org.bson.Document filter = new org.bson.Document("_id", nativeId);
        mongoTemplate.getCollection("products").updateOne(filter, updateDoc);
        // Refresh the in-memory product for downstream history/audit
        product.setModerationStatus(ModerationStatus.APPROVED);
        product.setStatus(ProductStatus.ACTIVE);
        product.setModerationActorId(moderator.getId());
        product.setModerationAt(now);
        product.setModerationReason(null);

        ModerationDecision decision = ModerationDecision.builder()
                .productId(productId)
                .action(ModerationAction.APPROVE)
                .newStatus(ModerationStatus.APPROVED)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(now)
                .message("Đã duyệt sản phẩm \"" + product.getName() + "\"")
                .build();

        recordHistory(product, decision, before, null);
        emitAudit(product, decision, before, clientIp);

        log.info("Moderator approve: productId={} moderatorId={} before={} after={}",
                productId, moderator.getId(), before, ModerationStatus.APPROVED);
        return decision;
    }

    // ======================== REJECT ========================

    @Override
    @Transactional
    public ModerationDecision reject(String productId, String reason, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Product product = loadAndGuard(productId);
        ModerationStatus before = product.getModerationStatus();

        validateReason(reason);

        if (before == null || !QUEUE_STATUSES.contains(before)) {
            throw new ConflictException(
                    "Sản phẩm đã ở trạng thái cuối hoặc không thể từ chối (status=" + before + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        Object nativeId = resolveNativeId("products", product.getId());
        org.bson.Document updateDoc = new org.bson.Document()
                .append("$set", new org.bson.Document()
                        .append("moderationStatus", ModerationStatus.REJECTED.name())
                        .append("status", ProductStatus.HIDDEN.name())
                        .append("moderationActorId", moderator.getId())
                        .append("moderationAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("moderationReason", reason)
                        .append("updatedAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant())));
        mongoTemplate.getCollection("products").updateOne(new org.bson.Document("_id", nativeId), updateDoc);
        product.setModerationStatus(ModerationStatus.REJECTED);
        product.setStatus(ProductStatus.HIDDEN);
        product.setModerationActorId(moderator.getId());
        product.setModerationAt(now);
        product.setModerationReason(reason);

        // Phase 2 §28 — Reject → Violation consistency.
        // Khi Moderator REJECT một Product, tạo Violation document để Admin
        // có history đầy đủ về vendor này vi phạm nội dung. Severity MEDIUM +
        // Type WARNING mặc định. KHÔNG tự đặt penalty rules — chỉ ghi nhận sự kiện.
        if (StringUtils.hasText(product.getShopId())) {
            try {
                Violation violation = Violation.builder()
                        .shopId(product.getShopId())
                        .productId(product.getId())
                        .productName(product.getName())
                        .type(ViolationType.WARNING)
                        .severity(ViolationSeverity.MEDIUM)
                        .reason("[Moderator REJECT Product] " + reason)
                        .createdBy(moderator.getUsername())
                        .build();
                violationRepository.save(violation);
            } catch (RuntimeException ex) {
                // Phase 2 — best-effort, không block reject flow
                log.warn("Failed to create Violation from Product reject: productId={} error={}",
                        productId, ex.getMessage());
            }
        }

        ModerationDecision decision = ModerationDecision.builder()
                .productId(productId)
                .action(ModerationAction.REJECT)
                .newStatus(ModerationStatus.REJECTED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(now)
                .message("Đã từ chối sản phẩm \"" + product.getName() + "\"")
                .build();

        recordHistory(product, decision, before, reason);
        emitAudit(product, decision, before, clientIp);

        log.info("Moderator reject: productId={} moderatorId={} reason.length={}",
                productId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== ESCALATE ========================

    @Override
    @Transactional
    public ModerationDecision escalate(String productId, String reason, CustomUserDetails moderator, String clientIp) {
        ModeratorGuard.verify(moderator);
        Product product = loadAndGuard(productId);
        ModerationStatus before = product.getModerationStatus();

        validateReason(reason);

        if (before == null || !QUEUE_STATUSES.contains(before)) {
            throw new ConflictException(
                    "Sản phẩm đã ở trạng thái cuối hoặc không thể leo thang (status=" + before + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        Object nativeId = resolveNativeId("products", product.getId());
        org.bson.Document updateDoc = new org.bson.Document()
                .append("$set", new org.bson.Document()
                        .append("moderationStatus", ModerationStatus.ESCALATED.name())
                        .append("status", ProductStatus.HIDDEN.name())
                        .append("moderationActorId", moderator.getId())
                        .append("moderationAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                        .append("moderationReason", reason)
                        .append("updatedAt", java.util.Date.from(now.atZone(java.time.ZoneId.systemDefault()).toInstant())));
        mongoTemplate.getCollection("products").updateOne(new org.bson.Document("_id", nativeId), updateDoc);
        product.setModerationStatus(ModerationStatus.ESCALATED);
        product.setStatus(ProductStatus.HIDDEN);
        product.setModerationActorId(moderator.getId());
        product.setModerationAt(now);
        product.setModerationReason(reason);

        ModerationDecision decision = ModerationDecision.builder()
                .productId(productId)
                .action(ModerationAction.ESCALATE)
                .newStatus(ModerationStatus.ESCALATED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(now)
                .message("Đã leo thang sản phẩm \"" + product.getName() + "\" lên Admin")
                .build();

        recordHistory(product, decision, before, reason);
        emitAudit(product, decision, before, clientIp);

        log.info("Moderator escalate: productId={} moderatorId={} reason.length={}",
                productId, moderator.getId(), reason.length());
        return decision;
    }

    // ======================== HELPERS ========================

    private Product loadAndGuard(String productId) {
        if (!StringUtils.hasText(productId)) {
            throw new BadRequestException("Product ID không hợp lệ");
        }
        // Phase 4 fix: support BOTH String _id and ObjectId _id.
        // Bug history: productRepository.findById(String) silently fails to match a
        // String _id stored in MongoDB because Spring Data's query translator
        // sends `{"id": "<hex>"}` (Java field name) instead of `{"_id": "<hex>"}`.
        // Workaround: query the collection directly via MongoTemplate. String
        // lookup FIRST (matches AdminProductServiceImpl pattern), then ObjectId.
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            org.bson.Document raw = mongoTemplate.getCollection("products")
                    .find(new org.bson.Document("_id", productId))
                    .first();
            if (raw == null && productId.length() == 24 && productId.matches("[0-9a-fA-F]+")) {
                org.bson.types.ObjectId oid = new org.bson.types.ObjectId(productId);
                raw = mongoTemplate.getCollection("products")
                        .find(new org.bson.Document("_id", oid))
                        .first();
            }
            if (raw != null) {
                if (!raw.containsKey("_class")) {
                    raw.put("_class", Product.class.getName());
                }
                product = mongoTemplate.getConverter().read(Product.class, raw);
                if (product.getId() == null) {
                    product.setId(raw.get("_id") instanceof org.bson.types.ObjectId
                            ? raw.get("_id").toString()
                            : (String) raw.get("_id"));
                }
            }
        }
        if (product == null) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId);
        }
        return product;
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Reason is required and must not be blank");
        }
    }

    /**
     * Phase 4 fix: resolve the native {@code _id} value of a MongoDB document so that
     * the subsequent {@code updateOne(_id, ...)} filter matches the actual stored type
     * (String or ObjectId). When Spring Data's {@code repository.save()} is given an
     * entity whose Java {@code @Id} field is a String, but the document in MongoDB was
     * written with a String {@code _id}, save() generates a new ObjectId insert and
     * leaves the original untouched — producing duplicate rows.
     */
    private Object resolveNativeId(String collectionName, String idHint) {
        if (idHint == null) {
            throw new BadRequestException("ID không hợp lệ");
        }
        // Try String first (matches our seed data convention).
        org.bson.Document raw = mongoTemplate.getCollection(collectionName)
                .find(new org.bson.Document("_id", idHint))
                .projection(new org.bson.Document("_id", 1))
                .first();
        if (raw != null) {
            return raw.get("_id");
        }
        // Fall back to ObjectId if it parses as 24 hex chars.
        if (idHint.length() == 24 && idHint.matches("[0-9a-fA-F]+")) {
            raw = mongoTemplate.getCollection(collectionName)
                    .find(new org.bson.Document("_id", new org.bson.types.ObjectId(idHint)))
                    .projection(new org.bson.Document("_id", 1))
                    .first();
            if (raw != null) {
                return raw.get("_id");
            }
        }
        throw new ResourceNotFoundException("Không tìm thấy document với ID: " + idHint);
    }

    private void recordHistory(Product product, ModerationDecision decision,
                                ModerationStatus before, String reason) {
        ModerationHistory history = ModerationHistory.builder()
                .resourceType("PRODUCT")
                .resourceId(product.getId())
                .resourceName(product.getName())
                .action(decision.getAction())
                .beforeStatus(before)
                .afterStatus(decision.getNewStatus())
                .reason(reason)
                .moderatorId(decision.getModeratorId())
                .moderatorEmail(decision.getModeratorEmail())
                .moderatorRole(UserRole.MODERATOR)
                .build();
        try {
            historyRepository.save(history);
        } catch (RuntimeException ex) {
            // History persistence failure must NOT mask the moderation action
            // (Phase 2A §41 — history is best-effort, primary action is DB product update).
            // We log it so the failure is visible to operators.
            log.error("Failed to persist ModerationHistory for productId={} action={}",
                    product.getId(), decision.getAction(), ex);
        }
    }

    private void emitAudit(Product product, ModerationDecision decision,
                            ModerationStatus before, String clientIp) {
        AuditEvent event = AuditEvent.builder()
                .actorId(decision.getModeratorId())
                .actorEmail(decision.getModeratorEmail())
                .role(UserRole.MODERATOR)
                .action("PRODUCT_" + decision.getAction().name())
                .resourceType("PRODUCT")
                .resourceId(product.getId())
                .reason(decision.getReason())
                .before(before != null ? before.name() : "NULL")
                .after(decision.getNewStatus().name())
                .ip(clientIp)
                .createdAt(decision.getDecidedAt())
                .build();
        try {
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.error("Failed to emit AuditEvent for productId={} action={}",
                    product.getId(), decision.getAction(), ex);
        }
    }

    /**
     * Phase 2A — Moderator account must be ACTIVE. Locked/unverified
     * moderators cannot perform any moderation action. Re-uses
     * Spring Security authentication state (CustomUserDetails.isEnabled
     * already encodes this; we add an explicit belt-and-braces check).
     */
    private static final class ModeratorGuard {
        static void verify(CustomUserDetails moderator) {
            if (moderator == null) {
                throw new UnauthorizedException("Yêu cầu đăng nhập Moderator");
            }
            if (!"MODERATOR".equals(moderator.getRole())) {
                // Defense-in-depth: even if a Customer somehow authenticates,
                // reject if role isn't MODERATOR.
                throw new UnauthorizedException("Tài khoản không có quyền Moderator");
            }
            if (!moderator.isEnabled() || !moderator.isAccountNonLocked()) {
                throw new UnauthorizedException("Tài khoản Moderator đã bị khóa hoặc chưa kích hoạt");
            }
        }
    }
}
