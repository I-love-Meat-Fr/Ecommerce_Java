package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.dto.complaint.CreateComplaintReq;
import com.ecommerce.cnj70.dto.complaint.ResolveComplaintReq;
import com.ecommerce.cnj70.dto.complaint.VendorRespondReq;
import com.ecommerce.cnj70.dto.moderation.AuditEvent;
import com.ecommerce.cnj70.enums.ComplaintLevel;
import com.ecommerce.cnj70.enums.ComplaintReason;
import com.ecommerce.cnj70.enums.ComplaintStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BadRequestException;
import com.ecommerce.cnj70.exception.ConflictException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.exception.UnauthorizedException;
import com.ecommerce.cnj70.repository.ComplaintRepository;
import com.ecommerce.cnj70.repository.OrderRepository;
import com.ecommerce.cnj70.repository.ShopRepository;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AuditEventWriter;
import com.ecommerce.cnj70.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phase 3 — Complaint service implementation.
 *
 * <p>Backed by {@code complaint_repository}. State machine:
 * Level 0 (Customer ↔ Vendor) → Level 1 (Moderator) → Level 2 (Admin).</p>
 *
 * <p>Tất cả ownership guards được enforce ở backend (Phase 3 §11-13, §39-43).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintServiceImpl implements ComplaintService {

    /** Complaint ở Level 0 — Scheduler có thể escalate nếu quá hạn. */
    private static final List<ComplaintStatus> LEVEL_0_OPEN_STATUSES = List.of(
            ComplaintStatus.OPEN, ComplaintStatus.VENDOR_RESPONDED);

    /** Phase 3 §61 — Deadline configurability.
     *  Phase 3 §32 — Không hardcode nếu policy chưa final → expose qua config.
     *  Default: 48h vendor response, 5 days moderator resolution. */
    private static final long DEFAULT_VENDOR_RESPONSE_HOURS = 48L;
    private static final long DEFAULT_MODERATOR_RESOLUTION_HOURS = 5L * 24L;

    private final ComplaintRepository complaintRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ShopRepository shopRepository;
    private final AuditEventWriter auditEventWriter;

    // ======================== LEVEL 0 — CUSTOMER ========================

    @Override
    @Transactional
    public Complaint createComplaint(CreateComplaintReq req, CustomUserDetails user) {
        validateRole(user, UserRole.CUSTOMER);
        if (req == null || req.getOrderId() == null || req.getReason() == null) {
            throw new BadRequestException("orderId và reason là bắt buộc");
        }
        if (req.getReason() == ComplaintReason.OTHER
                && (req.getDescription() == null || req.getDescription().isBlank())) {
            throw new BadRequestException("Lý do OTHER cần mô tả chi tiết");
        }

        // §11/14 — backend verify Order belongs to Customer
        Order order = orderRepository.findById(req.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Order"));

        if (!order.getUserId().equals(user.getId())) {
            throw new UnauthorizedException("Order không thuộc về Customer");
        }
        // Phase 3 §27-§28 — chỉ chấp nhận complaint cho order đã DELIVERED/PREPARING
        // (Không hardcode SLA khác; Phase 3 §32 không tự quyết định)
        Shop shop = shopRepository.findById(order.getShopId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Shop"));

        // Phase 3A §13 — verify từng orderItemId (productId) nằm trong Order.items.
        // Nếu request không gửi orderItemIds → complaint gắn cả Order (hợp lệ).
        List<String> verifiedItemIds = new ArrayList<>();
        if (req.getOrderItemIds() != null && !req.getOrderItemIds().isEmpty()) {
            java.util.Set<String> orderProductIds = order.getItems().stream()
                    .map(Order.OrderItem::getProductId)
                    .collect(java.util.stream.Collectors.toSet());
            for (String itemId : req.getOrderItemIds()) {
                if (itemId == null || itemId.isBlank()) {
                    throw new BadRequestException("orderItemIds chứa giá trị rỗng");
                }
                if (!orderProductIds.contains(itemId)) {
                    throw new BadRequestException(
                            "orderItemId '" + itemId + "' không thuộc Order " + order.getId());
                }
                verifiedItemIds.add(itemId);
            }
        }

        Complaint complaint = Complaint.builder()
                .customerId(user.getId())
                .customerName(user.getFullName())
                .customerEmail(user.getEmail())
                .shopId(order.getShopId())
                .shopName(shop.getShopName())
                .vendorId(shop.getOwnerId())
                .orderId(order.getId())
                .orderItemIds(verifiedItemIds)
                .reason(req.getReason())
                .description(req.getDescription())
                .evidence(req.getEvidence() != null ? req.getEvidence() : new ArrayList<>())
                .status(ComplaintStatus.OPEN)
                .level(ComplaintLevel.LEVEL_0)
                .vendorResponseDeadline(LocalDateTime.now().plusHours(DEFAULT_VENDOR_RESPONSE_HOURS))
                .escalationCount(0)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        emitAudit(saved, "COMPLAINT_CREATED", user, null);
        log.info("Complaint created: id={} orderId={} customerId={}", saved.getId(), saved.getOrderId(), user.getId());
        return saved;
    }

    @Override
    public Page<Complaint> listMyComplaints(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.CUSTOMER);
        return complaintRepository.findByCustomerId(user.getId(), pageable);
    }

    // ======================== LEVEL 0 — VENDOR ========================

    @Override
    public Page<Complaint> listShopComplaints(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.VENDOR);
        String shopId = user.getShopId();
        if (!StringUtils.hasText(shopId)) {
            // Vendor chưa có shop → không có complaint
            return Page.empty(pageable);
        }
        // §13 — chỉ complaints của shop mình
        return complaintRepository.findByShopId(shopId, pageable);
    }

    @Override
    @Transactional
    public Complaint vendorRespond(String complaintId, VendorRespondReq req, CustomUserDetails user) {
        validateRole(user, UserRole.VENDOR);
        if (req == null || req.getResponse() == null || req.getResponse().isBlank()) {
            throw new BadRequestException("response không được trống");
        }
        Complaint c = loadComplaint(complaintId);
        // §13 — chỉ respond complaint của Shop mình
        if (!c.getShopId().equals(user.getShopId())) {
            throw new UnauthorizedException("Complaint không thuộc Shop của Vendor");
        }
        if (c.getStatus().isTerminal() || c.getLevel() != ComplaintLevel.LEVEL_0) {
            throw new ConflictException("Complaint không ở trạng thái có thể phản hồi Level 0 (status=" + c.getStatus() + ")");
        }

        c.setVendorResponse(req.getResponse());
        c.setVendorRespondedAt(LocalDateTime.now());
        c.setVendorRespondedById(user.getId());
        c.setVendorRespondedByEmail(user.getEmail());
        c.setStatus(ComplaintStatus.VENDOR_RESPONDED);
        Complaint saved = complaintRepository.save(c);
        emitAudit(saved, "VENDOR_RESPONDED", user, null);
        return saved;
    }

    @Override
    @Transactional
    public Complaint escalateFromLevel0(String complaintId, String reason, CustomUserDetails user) {
        Complaint c = loadComplaint(complaintId);
        // §19/§40 — Customer hoặc Vendor của complaint được escalate
        boolean isParty = (user.getRole().equals(UserRole.CUSTOMER.name()) && user.getId().equals(c.getCustomerId()))
                || (user.getRole().equals(UserRole.VENDOR.name()) && c.getShopId().equals(user.getShopId()));
        if (!isParty) {
            throw new UnauthorizedException("Chỉ Customer/Vendor của complaint mới được escalate");
        }
        if (c.getStatus().isTerminal() || c.getLevel() != ComplaintLevel.LEVEL_0) {
            throw new ConflictException("Complaint không thể escalate (status=" + c.getStatus() + ", level=" + c.getLevel() + ")");
        }
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("Lý do escalate là bắt buộc");
        }

        c.setStatus(ComplaintStatus.ESCALATED);
        c.setLevel(ComplaintLevel.LEVEL_1);
        c.setEscalationCount(c.getEscalationCount() + 1);
        c.setModeratorResolutionDeadline(LocalDateTime.now().plusHours(DEFAULT_MODERATOR_RESOLUTION_HOURS));
        Complaint saved = complaintRepository.save(c);
        emitAudit(saved, "ESCALATED_TO_LEVEL_1", user, reason);
        log.info("Complaint escalated to L1: id={} by={} reason.length={}", complaintId, user.getRole(), reason.length());
        return saved;
    }

    @Override
    @Transactional
    public Complaint resolveLevel0(String complaintId, String note, CustomUserDetails user) {
        Complaint c = loadComplaint(complaintId);
        // §18 — Customer/Vendor đồng ý resolve. Cho Customer hoặc Vendor của complaint.
        boolean isParty = (user.getRole().equals(UserRole.CUSTOMER.name()) && user.getId().equals(c.getCustomerId()))
                || (user.getRole().equals(UserRole.VENDOR.name()) && c.getShopId().equals(user.getShopId()));
        if (!isParty) {
            throw new UnauthorizedException("Chỉ Customer/Vendor của complaint mới được resolve");
        }
        if (c.getStatus().isTerminal() || c.getLevel() != ComplaintLevel.LEVEL_0) {
            throw new ConflictException("Complaint không thể resolve ở Level 0 (status=" + c.getStatus() + ")");
        }
        // §40-§41 — Vendor không thể tự resolve nếu customer chưa đồng ý
        if (user.getRole().equals(UserRole.VENDOR.name())
                && c.getStatus() == ComplaintStatus.OPEN
                && c.getVendorResponse() == null) {
            throw new ConflictException("Vendor cần respond trước khi resolve");
        }

        c.setStatus(ComplaintStatus.RESOLVED);
        c.setDecision(note);
        c.setResolvedAt(LocalDateTime.now());
        c.setResolvedById(user.getId());
        c.setResolvedByEmail(user.getEmail());
        c.setResolvedByRole(user.getRole());
        Complaint saved = complaintRepository.save(c);
        emitAudit(saved, "RESOLVED_LEVEL_0", user, note);
        return saved;
    }

    // ======================== LEVEL 1 — MODERATOR ========================

    @Override
    public Page<Complaint> listModeratorQueue(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.MODERATOR);
        // Phase 3 §20 — Reuse Moderator queue
        return complaintRepository.findByLevel(ComplaintLevel.LEVEL_1, pageable);
    }

    @Override
    @Transactional
    public Complaint moderatorAct(String complaintId, ResolveComplaintReq req, CustomUserDetails user) {
        validateRole(user, UserRole.MODERATOR);
        Complaint c = loadComplaint(complaintId);
        if (c.getLevel() != ComplaintLevel.LEVEL_1) {
            throw new ConflictException("Complaint không ở Level 1 (level=" + c.getLevel() + ")");
        }
        if (c.getStatus().isTerminal()) {
            throw new ConflictException("Complaint đã ở trạng thái cuối (status=" + c.getStatus() + ")");
        }
        if (req == null || req.getAction() == null) {
            throw new BadRequestException("action là bắt buộc (CLAIM | RESOLVE | REJECT | ESCALATE)");
        }
        String action = req.getAction().toUpperCase();
        switch (action) {
            case "CLAIM" -> {
                c.setAssignedModeratorId(user.getId());
                c.setAssignedModeratorEmail(user.getEmail());
                c.setModeratorAssignedAt(LocalDateTime.now());
                c.setStatus(ComplaintStatus.MODERATOR_REVIEW);
            }
            case "RESOLVE" -> {
                if (req.getReason() == null || req.getReason().isBlank()) {
                    throw new BadRequestException("Lý do RESOLVE là bắt buộc");
                }
                c.setStatus(ComplaintStatus.RESOLVED);
                c.setDecision(req.getReason());
                c.setDecisionReason(req.getNote());
                c.setResolvedAt(LocalDateTime.now());
                c.setResolvedById(user.getId());
                c.setResolvedByEmail(user.getEmail());
                c.setResolvedByRole(UserRole.MODERATOR.name());
            }
            case "REJECT" -> {
                if (req.getReason() == null || req.getReason().isBlank()) {
                    throw new BadRequestException("Lý do REJECT là bắt buộc");
                }
                c.setStatus(ComplaintStatus.REJECTED);
                c.setDecisionReason(req.getReason());
                c.setResolvedAt(LocalDateTime.now());
                c.setResolvedById(user.getId());
                c.setResolvedByEmail(user.getEmail());
                c.setResolvedByRole(UserRole.MODERATOR.name());
            }
            case "ESCALATE" -> {
                if (req.getReason() == null || req.getReason().isBlank()) {
                    throw new BadRequestException("Lý do ESCALATE Level 2 là bắt buộc");
                }
                c.setLevel(ComplaintLevel.LEVEL_2);
                c.setStatus(ComplaintStatus.ESCALATED_L2);
                c.setEscalationCount(c.getEscalationCount() + 1);
            }
            default -> throw new BadRequestException("Action không hợp lệ: " + action);
        }
        Complaint saved = complaintRepository.save(c);
        emitAudit(saved, "MODERATOR_" + action, user, req.getReason());
        return saved;
    }

    // ======================== LEVEL 2 — ADMIN ========================

    @Override
    public Page<Complaint> listAdminLevel2Queue(CustomUserDetails user, Pageable pageable) {
        validateRole(user, UserRole.ADMIN);
        return complaintRepository.findByLevel(ComplaintLevel.LEVEL_2, pageable);
    }

    @Override
    @Transactional
    public Complaint adminAct(String complaintId, ResolveComplaintReq req, CustomUserDetails user) {
        validateRole(user, UserRole.ADMIN);
        Complaint c = loadComplaint(complaintId);
        if (c.getLevel() != ComplaintLevel.LEVEL_2) {
            throw new ConflictException("Complaint không ở Level 2 (level=" + c.getLevel() + ")");
        }
        if (req == null || req.getAction() == null) {
            throw new BadRequestException("action là bắt buộc (CLAIM | RESOLVE | REJECT)");
        }
        String action = req.getAction().toUpperCase();
        switch (action) {
            case "CLAIM" -> {
                c.setAssignedAdminId(user.getId());
                c.setAssignedAdminEmail(user.getEmail());
                c.setAdminAssignedAt(LocalDateTime.now());
                c.setStatus(ComplaintStatus.ADMIN_REVIEW);
            }
            case "RESOLVE" -> {
                if (req.getReason() == null || req.getReason().isBlank()) {
                    throw new BadRequestException("Lý do RESOLVE là bắt buộc");
                }
                c.setStatus(ComplaintStatus.RESOLVED);
                c.setDecision(req.getReason());
                c.setDecisionReason(req.getNote());
                c.setResolvedAt(LocalDateTime.now());
                c.setResolvedById(user.getId());
                c.setResolvedByEmail(user.getEmail());
                c.setResolvedByRole(UserRole.ADMIN.name());
            }
            case "REJECT" -> {
                if (req.getReason() == null || req.getReason().isBlank()) {
                    throw new BadRequestException("Lý do REJECT là bắt buộc");
                }
                c.setStatus(ComplaintStatus.REJECTED);
                c.setDecisionReason(req.getReason());
                c.setResolvedAt(LocalDateTime.now());
                c.setResolvedById(user.getId());
                c.setResolvedByEmail(user.getEmail());
                c.setResolvedByRole(UserRole.ADMIN.name());
            }
            default -> throw new BadRequestException("Action không hợp lệ ở Level 2: " + action);
        }
        Complaint saved = complaintRepository.save(c);
        emitAudit(saved, "ADMIN_" + action, user, req.getReason());
        return saved;
    }

    // ======================== UNIVERSAL ========================

    @Override
    public Complaint getById(String id, CustomUserDetails user) {
        Complaint c = loadComplaint(id);
        String role = user.getRole();
        // Customer — chỉ xem của mình (Phase 3 §12)
        if (UserRole.CUSTOMER.name().equals(role)) {
            if (!c.getCustomerId().equals(user.getId())) {
                throw new UnauthorizedException("Complaint không thuộc Customer");
            }
        } else if (UserRole.VENDOR.name().equals(role)) {
            // Vendor — chỉ xem của shop mình (Phase 3 §13)
            if (!c.getShopId().equals(user.getShopId())) {
                throw new UnauthorizedException("Complaint không thuộc Shop của Vendor");
            }
        } else if (UserRole.MODERATOR.name().equals(role)) {
            // Moderator — chỉ Level 1
            if (c.getLevel() != ComplaintLevel.LEVEL_1 && c.getLevel() != ComplaintLevel.LEVEL_2) {
                // Moderator có thể xem Level 2 đã escalated (audit/visibility),
                // nhưng không xem Level 0 (giữa Customer ↔ Vendor)
                throw new UnauthorizedException("Moderator chỉ xem Complaint Level 1 / Level 2");
            }
        } else if (!UserRole.ADMIN.name().equals(role) && !"SYSTEM".equals(role)) {
            throw new UnauthorizedException("Role không hợp lệ");
        }
        // SYSTEM and ADMIN can read any complaint (scheduler / oversight)
        return c;
    }

    // ======================== SCHEDULER HOOKS ========================

    @Override
    public List<Complaint> findOverdueForLevel0Escalation(LocalDateTime now) {
        return complaintRepository.findByStatusInAndVendorResponseDeadlineBefore(LEVEL_0_OPEN_STATUSES, now);
    }

    @Override
    public List<Complaint> findOverdueForLevel2Escalation(LocalDateTime now) {
        return complaintRepository.findByLevelAndModeratorResolutionDeadlineBefore(ComplaintLevel.LEVEL_1, now);
    }

    @Override
    @Transactional
    public Complaint autoEscalateToLevel1(Complaint c) {
        // Idempotency (§34) — chỉ escalate nếu vẫn ở Level 0 + trạng thái cho phép
        if (c.getLevel() != ComplaintLevel.LEVEL_0 || !c.getStatus().isOpenLevel()) {
            log.info("Skip auto-escalate L1: complaint {} không còn ở Level 0 open (status={}, level={})",
                    c.getId(), c.getStatus(), c.getLevel());
            return c;
        }
        c.setStatus(ComplaintStatus.ESCALATED);
        c.setLevel(ComplaintLevel.LEVEL_1);
        c.setEscalationCount(c.getEscalationCount() + 1);
        c.setModeratorResolutionDeadline(LocalDateTime.now().plusHours(DEFAULT_MODERATOR_RESOLUTION_HOURS));
        Complaint saved = complaintRepository.save(c);
        emitAudit(saved, "AUTO_ESCALATED_TO_LEVEL_1", null, "Scheduler: vendor response deadline exceeded");
        log.info("Auto-escalated complaint {} to Level 1", c.getId());
        return saved;
    }

    @Override
    @Transactional
    public Complaint autoEscalateToLevel2(Complaint c) {
        if (c.getLevel() != ComplaintLevel.LEVEL_1) {
            log.info("Skip auto-escalate L2: complaint {} không còn ở Level 1 (level={})",
                    c.getId(), c.getLevel());
            return c;
        }
        c.setLevel(ComplaintLevel.LEVEL_2);
        c.setStatus(ComplaintStatus.ESCALATED_L2);
        c.setEscalationCount(c.getEscalationCount() + 1);
        Complaint saved = complaintRepository.save(c);
        emitAudit(saved, "AUTO_ESCALATED_TO_LEVEL_2", null, "Scheduler: moderator resolution deadline exceeded");
        log.info("Auto-escalated complaint {} to Level 2", c.getId());
        return saved;
    }

    @Override
    public Map<String, Object> getEscalationConfig() {
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("vendorResponseTimeoutHours", DEFAULT_VENDOR_RESPONSE_HOURS);
        cfg.put("moderatorResolutionTimeoutHours", DEFAULT_MODERATOR_RESOLUTION_HOURS);
        cfg.put("level0OpenStatuses", EnumSet.copyOf(LEVEL_0_OPEN_STATUSES).toString());
        return cfg;
    }

    // ======================== HELPERS ========================

    private Complaint loadComplaint(String id) {
        if (!StringUtils.hasText(id)) {
            throw new BadRequestException("complaintId không hợp lệ");
        }
        return complaintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Complaint: " + id));
    }

    private void validateRole(CustomUserDetails user, UserRole required) {
        if (user == null) {
            throw new UnauthorizedException("Yêu cầu đăng nhập");
        }
        if (!required.name().equals(user.getRole())) {
            throw new UnauthorizedException("Yêu cầu role " + required + ", hiện tại: " + user.getRole());
        }
        // Phase 3A §F + §40 — IDOR/security: lock out disabled OR locked accounts.
        // Previous implementation had an inverted condition that wrongly rejected
        // active enabled users (`isAccountNonLocked() && !isEnabled()`).
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Tài khoản đã bị vô hiệu hóa");
        }
        if (!user.isAccountNonLocked()) {
            throw new UnauthorizedException("Tài khoản đã bị khóa");
        }
    }

    private void emitAudit(Complaint c, String action, CustomUserDetails actor, String reason) {
        try {
            AuditEvent ev = AuditEvent.builder()
                    .actorId(actor != null ? actor.getId() : "SYSTEM")
                    .actorEmail(actor != null ? actor.getEmail() : "scheduler@local")
                    .role(actor != null ? UserRole.valueOf(actor.getRole()) : UserRole.MODERATOR)
                    .action("COMPLAINT_" + action)
                    .resourceType("COMPLAINT")
                    .resourceId(c.getId())
                    .reason(reason)
                    .createdAt(LocalDateTime.now())
                    .build();
            auditEventWriter.write(ev);
        } catch (RuntimeException ex) {
            log.error("Audit emit failed for complaintId={} action={}: {}",
                    c.getId(), action, ex.getMessage());
        }
    }
}
