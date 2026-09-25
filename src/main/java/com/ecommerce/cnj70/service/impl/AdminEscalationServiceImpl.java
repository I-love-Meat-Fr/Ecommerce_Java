package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.document.ModerationHistory;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.ReportCase;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ModerationAction;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.enums.ProductStatus;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ShopStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.enums.ViolationAction;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.EscalationRepository;
import com.ecommerce.cnj70.repository.ModerationHistoryRepository;
import com.ecommerce.cnj70.repository.ProductRepository;
import com.ecommerce.cnj70.repository.ReportCaseRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminEscalationService;
import com.ecommerce.cnj70.service.AuditEventWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
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
 * Phase 3A — Admin Escalation + Enforcement Service Implementation.
 *
 * <p>See {@link AdminEscalationService} for the contract.</p>
 *
 * <h3>State transitions enforced</h3>
 * <ul>
 *     <li>{@code ViolationAction.PRODUCT_HIDDEN} / {@code PRODUCT_REJECTED}
 *         → {@code Product.status = HIDDEN}</li>
 *     <li>{@code ViolationAction.SHOP_RESTRICTED}
 *         → {@code Shop.status = RESTRICTED}</li>
 *     <li>{@code ViolationAction.SHOP_SUSPENDED}
 *         → {@code Shop.status = SUSPENDED}</li>
 *     <li>{@code ViolationAction.VENDOR_BANNED}
 *         → {@code User.status = LOCKED} (only if User.role = VENDOR)</li>
 *     <li>{@code ViolationAction.WARNING}
 *         → no resource state change; record-only</li>
 *     <li>{@code ViolationAction.FUNDS_FROZEN}
 *         → OUT OF SCOPE (Finance / Phase 4C); rejected with BadRequest</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEscalationServiceImpl implements AdminEscalationService {

    /** Default queue universe for Admin — pending + in-review escalations. */
    private static final Set<Escalation.Status> QUEUE_STATUSES = Collections.unmodifiableSet(
            EnumSet.of(Escalation.Status.PENDING, Escalation.Status.IN_REVIEW));

    private final EscalationRepository escalationRepository;
    private final ReportCaseRepository reportCaseRepository;
    private final ProductRepository productRepository;
    private final ShopRepository shopRepository;
    private final UserRepository userRepository;
    private final ModerationHistoryRepository historyRepository;
    private final AuditEventWriter auditEventWriter;
    private final MongoTemplate mongoTemplate;

    // ======================== QUEUE ========================

    @Override
    public Page<Escalation> listEscalations(Escalation.Status statusFilter,
                                            EscalationSeverity severityFilter,
                                            ReportCaseResourceType resourceTypeFilter,
                                            Pageable pageable) {
        if (statusFilter != null && statusFilter.isTerminal()) {
            // RESOLVED is terminal — not in queue universe. Short-circuit to avoid
            // issuing a DB query that would necessarily return nothing useful.
            return Page.empty(pageable);
        }
        Set<Escalation.Status> effectiveStatuses = (statusFilter != null
                && QUEUE_STATUSES.contains(statusFilter))
                ? Collections.singleton(statusFilter)
                : QUEUE_STATUSES;

        boolean hasSeverity = severityFilter != null;
        boolean hasResource = resourceTypeFilter != null;

        Page<Escalation> page;
        if (hasSeverity && hasResource) {
            page = escalationRepository.findByStatusInAndSeverityAndResourceType(
                    effectiveStatuses, severityFilter, resourceTypeFilter, pageable);
        } else if (hasSeverity) {
            page = escalationRepository.findByStatusInAndSeverity(
                    effectiveStatuses, severityFilter, pageable);
        } else if (hasResource) {
            page = escalationRepository.findByStatusInAndResourceType(
                    effectiveStatuses, resourceTypeFilter, pageable);
        } else {
            page = escalationRepository.findByStatusIn(effectiveStatuses, pageable);
        }
        return page;
    }

    // ======================== DETAIL ========================

    @Override
    public Escalation getDetail(String escalationId) {
        return loadAndGuard(escalationId);
    }

    // ======================== CLAIM ========================

    @Override
    @Transactional
    public Escalation claim(String escalationId, CustomUserDetails admin, String clientIp) {
        AdminGuard.verify(admin);
        Escalation esc = loadAndGuard(escalationId);

        if (esc.getStatus() != null && esc.getStatus().isTerminal()) {
            throw new ConflictException("Escalation đã ở trạng thái cuối (status=" + esc.getStatus() + ")");
        }

        Escalation.Status before = esc.getStatus();
        esc.setStatus(Escalation.Status.IN_REVIEW);
        esc.setAssignedAdminId(admin.getId());
        esc.setAssignedAdminEmail(admin.getUsername());
        Escalation saved = escalationRepository.save(esc);

        AuditEvent event = AuditEvent.builder()
                .actorId(admin.getId())
                .actorEmail(admin.getUsername())
                .role(UserRole.ADMIN)
                .action("ESCALATION_CLAIM")
                .resourceType("ESCALATION")
                .resourceId(saved.getId())
                .before(before != null ? before.name() : "NULL")
                .after(Escalation.Status.IN_REVIEW.name())
                .ip(clientIp)
                .createdAt(LocalDateTime.now())
                .build();
        safeEmitAudit(event);
        log.info("Admin claim escalation: id={} adminId={} before={} after=IN_REVIEW",
                saved.getId(), admin.getId(), before);
        return saved;
    }

    // ======================== ENFORCE ========================

    @Override
    @Transactional
    public Escalation enforce(String escalationId, ViolationAction action,
                              String reason, String note,
                              CustomUserDetails admin, String clientIp) {
        AdminGuard.verify(admin);
        validateReason(reason);
        Escalation esc = loadAndGuard(escalationId);
        if (esc.getStatus() != null && esc.getStatus().isTerminal()) {
            throw new ConflictException("Escalation đã ở trạng thái cuối, không thể enforce");
        }

        // Pre-condition: if no Admin has claimed yet, claim first.
        if (esc.getStatus() == Escalation.Status.PENDING) {
            esc.setStatus(Escalation.Status.IN_REVIEW);
            esc.setAssignedAdminId(admin.getId());
            esc.setAssignedAdminEmail(admin.getUsername());
        }

        // Resolve the resource and perform the actual state transition.
        ResourceTransition transition = performTransition(esc, action, reason, admin);

        // Persist updated escalation
        esc.setStatus(Escalation.Status.RESOLVED);
        esc.setDecisionAdminId(admin.getId());
        esc.setDecisionAdminEmail(admin.getUsername());
        esc.setDecisionAction(action);
        esc.setDecisionNote(note);
        esc.setResolvedAt(LocalDateTime.now());
        Escalation saved = escalationRepository.save(esc);

        // Phase 3B — Audit: Enforcement action on resource
        AuditEvent event = AuditEvent.builder()
                .actorId(admin.getId())
                .actorEmail(admin.getUsername())
                .role(UserRole.ADMIN)
                .action(action.name())
                .resourceType(transition.resourceType)
                .resourceId(transition.resourceId)
                .reason(reason)
                .severity(esc.getSeverity() != null ? esc.getSeverity().name() : null)
                .before(transition.before)
                .after(transition.after)
                .ip(clientIp)
                .createdAt(LocalDateTime.now())
                .build();
        safeEmitAudit(event);

        // Phase 3B — Audit: Escalation resolved
        AuditEvent escalationResolvedEvent = AuditEvent.builder()
                .actorId(admin.getId())
                .actorEmail(admin.getUsername())
                .role(UserRole.ADMIN)
                .action("ESCALATION_RESOLVED")
                .resourceType("ESCALATION")
                .resourceId(saved.getId())
                .reason("Enforcement applied: " + action.name())
                .severity(esc.getSeverity() != null ? esc.getSeverity().name() : null)
                .before(Escalation.Status.IN_REVIEW.name())
                .after(Escalation.Status.RESOLVED.name())
                .ip(clientIp)
                .createdAt(LocalDateTime.now())
                .build();
        safeEmitAudit(escalationResolvedEvent);

        // Moderation history (separate from audit log; reused Phase 2A repo)
        recordHistory(saved, action, transition, admin, reason);

        log.info("Admin enforce escalation: id={} action={} resourceType={} resourceId={} adminId={}",
                saved.getId(), action, transition.resourceType, transition.resourceId, admin.getId());
        return saved;
    }

    // ======================== DISMISS ========================

    @Override
    @Transactional
    public Escalation dismiss(String escalationId, String reason, String note,
                              CustomUserDetails admin, String clientIp) {
        AdminGuard.verify(admin);
        validateReason(reason);
        Escalation esc = loadAndGuard(escalationId);
        if (esc.getStatus() != null && esc.getStatus().isTerminal()) {
            throw new ConflictException("Escalation đã ở trạng thái cuối, không thể dismiss");
        }

        if (esc.getStatus() == Escalation.Status.PENDING) {
            esc.setStatus(Escalation.Status.IN_REVIEW);
            esc.setAssignedAdminId(admin.getId());
            esc.setAssignedAdminEmail(admin.getUsername());
        }
        esc.setStatus(Escalation.Status.RESOLVED);
        esc.setDecisionAdminId(admin.getId());
        esc.setDecisionAdminEmail(admin.getUsername());
        // decisionAction stays null for dismiss (no enforcement was applied)
        esc.setDecisionNote(note);
        esc.setResolvedAt(LocalDateTime.now());
        Escalation saved = escalationRepository.save(esc);

        // Phase 3B — Audit: Escalation dismissed
        AuditEvent event = AuditEvent.builder()
                .actorId(admin.getId())
                .actorEmail(admin.getUsername())
                .role(UserRole.ADMIN)
                .action("ESCALATION_DISMISSED")
                .resourceType("ESCALATION")
                .resourceId(saved.getId())
                .reason(reason)
                .severity(esc.getSeverity() != null ? esc.getSeverity().name() : null)
                .before(Escalation.Status.IN_REVIEW.name())
                .after(Escalation.Status.RESOLVED.name())
                .ip(clientIp)
                .createdAt(LocalDateTime.now())
                .build();
        safeEmitAudit(event);

        // Phase 3B — Audit: Escalation resolved (dismiss counts as resolution)
        AuditEvent resolvedEvent = AuditEvent.builder()
                .actorId(admin.getId())
                .actorEmail(admin.getUsername())
                .role(UserRole.ADMIN)
                .action("ESCALATION_RESOLVED")
                .resourceType("ESCALATION")
                .resourceId(saved.getId())
                .reason("Dismissed: " + reason)
                .severity(esc.getSeverity() != null ? esc.getSeverity().name() : null)
                .before(Escalation.Status.IN_REVIEW.name())
                .after(Escalation.Status.RESOLVED.name())
                .ip(clientIp)
                .createdAt(LocalDateTime.now())
                .build();
        safeEmitAudit(resolvedEvent);

        recordDismissHistory(saved, admin, reason);
        log.info("Admin dismiss escalation: id={} adminId={} reason.length={}",
                saved.getId(), admin.getId(), reason.length());
        return saved;
    }

    // ======================== TRANSITIONS ========================

    private ResourceTransition performTransition(Escalation esc, ViolationAction action,
                                                  String reason, CustomUserDetails admin) {
        LocalDateTime now = LocalDateTime.now();
        switch (action) {
            case WARNING:
                // Record-only; no resource state change.
                return new ResourceTransition("ESCALATION", esc.getId(),
                        "N/A", "N/A");

            case PRODUCT_HIDDEN:
            case PRODUCT_REJECTED: {
                Product product = requireProduct(esc);
                String before = product.getStatus() != null ? product.getStatus().name() : "NULL";
                product.setStatus(ProductStatus.HIDDEN);
                product.setEnforcementActorId(admin.getId());
                product.setEnforcementReason(reason);
                product.setEnforcementAt(now);
                Product saved = productRepository.save(product);
                return new ResourceTransition("PRODUCT", saved.getId(),
                        before, ProductStatus.HIDDEN.name());
            }

            case SHOP_RESTRICTED: {
                Shop shop = requireShop(esc);
                String before = shop.getStatus() != null ? shop.getStatus().name() : "NULL";
                shop.setStatus(ShopStatus.RESTRICTED);
                shop.setActive(false);
                shop.setEnforcementActorId(admin.getId());
                shop.setEnforcementReason(reason);
                shop.setEnforcementAt(now);
                Shop saved = shopRepository.save(shop);
                return new ResourceTransition("SHOP", saved.getId(),
                        before, ShopStatus.RESTRICTED.name());
            }

            case SHOP_SUSPENDED: {
                Shop shop = requireShop(esc);
                String before = shop.getStatus() != null ? shop.getStatus().name() : "NULL";
                shop.setStatus(ShopStatus.SUSPENDED);
                shop.setActive(false);
                shop.setEnforcementActorId(admin.getId());
                shop.setEnforcementReason(reason);
                shop.setEnforcementAt(now);
                Shop saved = shopRepository.save(shop);
                return new ResourceTransition("SHOP", saved.getId(),
                        before, ShopStatus.SUSPENDED.name());
            }

            case VENDOR_BANNED: {
                User vendor = requireVendor(esc);
                String before = vendor.getStatus() != null ? vendor.getStatus().name() : "NULL";
                vendor.setStatus(AccountStatus.LOCKED);
                vendor.setEnforcementActorId(admin.getId());
                vendor.setEnforcementReason(reason);
                vendor.setEnforcementAt(now);
                User saved = userRepository.save(vendor);
                return new ResourceTransition("USER", saved.getId(),
                        before, AccountStatus.LOCKED.name());
            }

            case FUNDS_FROZEN:
                // Phase 3A §21 explicitly excludes FUNDS_FROZEN (Finance scope).
                throw new BadRequestException(
                        "FUNDS_FROZEN thuộc Finance scope (Phase 4C), không hỗ trợ trong Phase 3A");

            default:
                throw new BadRequestException("ViolationAction không hợp lệ: " + action);
        }
    }

    private Product requireProduct(Escalation esc) {
        if (esc.getResourceType() != ReportCaseResourceType.PRODUCT) {
            throw new BadRequestException(
                    "Escalation này không trỏ vào PRODUCT (resourceType="
                            + esc.getResourceType() + "), không thể " + ViolationAction.PRODUCT_HIDDEN);
        }
        return productRepository.findById(esc.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Product với ID: " + esc.getResourceId()));
    }

    private Shop requireShop(Escalation esc) {
        if (!StringUtils.hasText(esc.getShopId())) {
            throw new BadRequestException(
                    "Escalation không có shopId — không thể áp dụng SHOP_RESTRICTED/SHOP_SUSPENDED");
        }
        return shopRepository.findById(esc.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Shop với ID: " + esc.getShopId()));
    }

    private User requireVendor(Escalation esc) {
        if (!StringUtils.hasText(esc.getVendorId())) {
            throw new BadRequestException(
                    "Escalation không có vendorId — không thể áp dụng VENDOR_BANNED");
        }
        User vendor = userRepository.findById(esc.getVendorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Vendor (User) với ID: " + esc.getVendorId()));
        if (vendor.getRole() != UserRole.VENDOR) {
            throw new BadRequestException(
                    "User " + vendor.getId() + " không phải VENDOR (role=" + vendor.getRole() + ")");
        }
        return vendor;
    }

    // ======================== HELPERS ========================

    private Escalation loadAndGuard(String escalationId) {
        if (!StringUtils.hasText(escalationId)) {
            throw new BadRequestException("Escalation ID không hợp lệ");
        }
        // Phase 6 fix: String _id retrieval — same pattern as Phase 5 fix.
        // Use raw mongoTemplate query for reliable String _id lookup.
        Document raw = mongoTemplate.getCollection("escalations")
                .find(new Document("_id", escalationId))
                .first();
        if (raw == null) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy Escalation với ID: " + escalationId);
        }
        if (!raw.containsKey("_class")) {
            raw.put("_class", Escalation.class.getName());
        }
        return mongoTemplate.getConverter().read(Escalation.class, raw);
    }

    private static void validateReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Reason is required and must not be blank");
        }
    }

    private void safeEmitAudit(AuditEvent event) {
        try {
            auditEventWriter.write(event);
        } catch (RuntimeException ex) {
            log.error("Failed to emit AuditEvent: action={} resource={}",
                    event.getAction(), event.getResourceId(), ex);
        }
    }

    private void recordHistory(Escalation esc, ViolationAction action,
                                ResourceTransition transition,
                                CustomUserDetails admin, String reason) {
        ModerationAction mapped = switch (action) {
            case PRODUCT_HIDDEN, PRODUCT_REJECTED -> ModerationAction.APPROVE; // reuse APPROVE for "enforced successfully"
            case SHOP_RESTRICTED, SHOP_SUSPENDED -> ModerationAction.APPROVE;
            case VENDOR_BANNED -> ModerationAction.APPROVE;
            default -> null;
        };
        if (mapped == null) {
            return;
        }
        ModerationStatus before = ModerationStatus.ESCALATED;
        ModerationStatus after = ModerationStatus.APPROVED;
        ModerationHistory history = ModerationHistory.builder()
                .resourceType("ESCALATION")
                .resourceId(esc.getId())
                .resourceName("Escalation #" + esc.getId()
                        + " → " + transition.resourceType + "/" + transition.resourceId)
                .action(mapped)
                .beforeStatus(before)
                .afterStatus(after)
                .reason(reason)
                .moderatorId(admin.getId())
                .moderatorEmail(admin.getUsername())
                .moderatorRole(UserRole.ADMIN)
                .build();
        try {
            historyRepository.save(history);
        } catch (RuntimeException ex) {
            log.error("Failed to persist ModerationHistory for escalation: id={} action={}",
                    esc.getId(), action, ex);
        }
    }

    private void recordDismissHistory(Escalation esc, CustomUserDetails admin, String reason) {
        ModerationHistory history = ModerationHistory.builder()
                .resourceType("ESCALATION")
                .resourceId(esc.getId())
                .resourceName("Escalation #" + esc.getId())
                .action(ModerationAction.REJECT)
                .beforeStatus(ModerationStatus.ESCALATED)
                .afterStatus(ModerationStatus.REJECTED)
                .reason(reason)
                .moderatorId(admin.getId())
                .moderatorEmail(admin.getUsername())
                .moderatorRole(UserRole.ADMIN)
                .build();
        try {
            historyRepository.save(history);
        } catch (RuntimeException ex) {
            log.error("Failed to persist ModerationHistory for escalation dismiss: id={}", esc.getId(), ex);
        }
    }

    /** Internal record carrying transition details for audit emission. */
    private record ResourceTransition(String resourceType, String resourceId,
                                      String before, String after) {
    }

    private static final class AdminGuard {
        static void verify(CustomUserDetails admin) {
            if (admin == null) {
                throw new UnauthorizedException("Yêu cầu đăng nhập Admin");
            }
            if (!"ADMIN".equals(admin.getRole())) {
                throw new UnauthorizedException("Tài khoản không có quyền Admin");
            }
            if (!admin.isEnabled() || !admin.isAccountNonLocked()) {
                throw new UnauthorizedException("Tài khoản Admin đã bị khóa hoặc chưa kích hoạt");
            }
        }
    }
}
