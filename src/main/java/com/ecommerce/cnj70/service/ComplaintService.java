package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.dto.complaint.CreateComplaintReq;
import com.ecommerce.cnj70.dto.complaint.ResolveComplaintReq;
import com.ecommerce.cnj70.dto.complaint.VendorRespondReq;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

/**
 * Phase 3 — Complaint service contract.
 *
 * <p>State machine: OPEN → VENDOR_RESPONDED → RESOLVED | ESCALATED → MODERATOR_REVIEW
 * → RESOLVED | ESCALATED_L2 → ADMIN_REVIEW → RESOLVED / CLOSED.</p>
 *
 * <p>Backed by {@code complaint_repository} collection. Reuses Escalation /
 * Violation / AuditEventWriter seams from Phase 1+2 (no new audit system).</p>
 */
public interface ComplaintService {

    // =========== Customer (Level 0) ===========

    /** §14 — Customer tạo complaint. customerId/orderId lấy từ authenticated user. */
    Complaint createComplaint(CreateComplaintReq req, CustomUserDetails user);

    /** §12 — Customer xem complaints của mình. */
    Page<Complaint> listMyComplaints(CustomUserDetails user, Pageable pageable);

    // =========== Vendor (Level 0 response) ===========

    /** §17 — Vendor xem complaints liên quan Shop của mình. */
    Page<Complaint> listShopComplaints(CustomUserDetails user, Pageable pageable);

    /** §17 — Vendor phản hồi complaint (Level 0). */
    Complaint vendorRespond(String complaintId, VendorRespondReq req, CustomUserDetails user);

    /** §19 — Customer/Vendor escalate Level 0 → Level 1. */
    Complaint escalateFromLevel0(String complaintId, String reason, CustomUserDetails user);

    /** §18 — Both-party RESOLVED Level 0. */
    Complaint resolveLevel0(String complaintId, String note, CustomUserDetails user);

    // =========== Moderator (Level 1) ===========

    /** §20 — Moderator xem queue Level 1. */
    Page<Complaint> listModeratorQueue(CustomUserDetails user, Pageable pageable);

    /** §22 — Moderator xử lý: claim / resolve / reject / escalate L2. */
    Complaint moderatorAct(String complaintId, ResolveComplaintReq req, CustomUserDetails user);

    // =========== Admin (Level 2) ===========

    /** §25 — Admin xem Level 2 cases. */
    Page<Complaint> listAdminLevel2Queue(CustomUserDetails user, Pageable pageable);

    /** §25 — Admin xử lý Level 2. */
    Complaint adminAct(String complaintId, ResolveComplaintReq req, CustomUserDetails user);

    // =========== Universal read ===========

    /** §22 — Detail — enforced ở controller-level cho từng role. */
    Complaint getById(String id, CustomUserDetails user);

    /** Scheduler hook (§31-§34) — find overdue Level 0 candidates. */
    java.util.List<Complaint> findOverdueForLevel0Escalation(java.time.LocalDateTime now);

    /** Scheduler hook (§31-§34) — find overdue Level 1 candidates. */
    java.util.List<Complaint> findOverdueForLevel2Escalation(java.time.LocalDateTime now);

    /** Scheduler hook — apply Level 0 → Level 1 escalation (idempotent). */
    Complaint autoEscalateToLevel1(Complaint c);

    /** Scheduler hook — apply Level 1 → Level 2 escalation (idempotent). */
    Complaint autoEscalateToLevel2(Complaint c);

    /** Phase 3 §61 — Default vendor response deadline (configurable). */
    Map<String, Object> getEscalationConfig();
}
