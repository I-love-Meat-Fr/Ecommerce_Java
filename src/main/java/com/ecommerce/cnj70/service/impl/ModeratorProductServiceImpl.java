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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));

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

        product.setModerationStatus(ModerationStatus.APPROVED);
        product.setStatus(ProductStatus.ACTIVE); // mapping rule (Phase 2A §10)
        product.setModerationActorId(moderator.getId());
        product.setModerationAt(LocalDateTime.now());
        product.setModerationReason(null);
        productRepository.save(product);

        ModerationDecision decision = ModerationDecision.builder()
                .productId(productId)
                .action(ModerationAction.APPROVE)
                .newStatus(ModerationStatus.APPROVED)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
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

        product.setModerationStatus(ModerationStatus.REJECTED);
        product.setStatus(ProductStatus.HIDDEN); // mapping rule (Phase 2A §10)
        product.setModerationActorId(moderator.getId());
        product.setModerationAt(LocalDateTime.now());
        product.setModerationReason(reason);
        productRepository.save(product);

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
                .decidedAt(LocalDateTime.now())
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

        product.setModerationStatus(ModerationStatus.ESCALATED);
        product.setStatus(ProductStatus.HIDDEN); // mapping rule (Phase 2A §10) — admin will enforce
        product.setModerationActorId(moderator.getId());
        product.setModerationAt(LocalDateTime.now());
        product.setModerationReason(reason);
        productRepository.save(product);

        ModerationDecision decision = ModerationDecision.builder()
                .productId(productId)
                .action(ModerationAction.ESCALATE)
                .newStatus(ModerationStatus.ESCALATED)
                .reason(reason)
                .moderatorId(moderator.getId())
                .moderatorEmail(moderator.getUsername())
                .decidedAt(LocalDateTime.now())
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
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + productId));
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Reason is required and must not be blank");
        }
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
