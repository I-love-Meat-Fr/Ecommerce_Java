package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ViolationAction;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 3A — Admin Escalation + Enforcement Service.
 *
 * <h3>Contract</h3>
 * <ul>
 *     <li>{@link #listEscalations} — Admin queue (PENDING + IN_REVIEW)
 *         with optional filters (status / severity / resourceType).</li>
 *     <li>{@link #getDetail} — fetch an escalation with full context
 *         (resource, vendor, shop, source ReportCase, previous escalations).</li>
 *     <li>{@link #claim} — first Admin action: PENDING → IN_REVIEW.</li>
 *     <li>{@link #enforce} — apply a {@link ViolationAction}. Performs
 *         the actual state transition (Product → HIDDEN, Shop → SUSPENDED /
 *         RESTRICTED, User → LOCKED for VENDOR) AND emits the audit event
 *         via the AuditEventWriter seam.</li>
 *     <li>{@link #dismiss} — Admin decides no enforcement is needed
 *         (e.g. Moderator over-escalated).</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <ul>
 *     <li>Route-level: SecurityConfig.hasRole("ADMIN") (Phase 1).</li>
 *     <li>Object-level: every action calls {@code loadAndGuard(id)}.</li>
 *     <li>Account-status: locked admins cannot act.</li>
 *     <li>State-level: terminal escalations reject further actions with 409.</li>
 *     <li>Role guard: only ADMIN role may call (defense-in-depth beyond
 *         SecurityConfig).</li>
 *     <li>Moderator cannot reach this service (route-level + service-level).</li>
 * </ul>
 *
 * <h3>Audit / History</h3>
 * <ul>
 *     <li>Every {@link #enforce} / {@link #dismiss} writes a
 *         {@code ModerationHistory} entry (resourceType="ESCALATION",
 *         action via {@link com.ecommerce.cnj70.enums.ModerationAction}).</li>
 *     <li>Same path emits an {@link com.ecommerce.cnj70.dto.moderation.AuditEvent}
 *         via the AuditEventWriter seam (Phase 2A) with action
 *         {@code PRODUCT_SUSPENDED} / {@code SHOP_SUSPENDED} / etc., before /
 *         after state captured from the backend transition.</li>
 * </ul>
 */
public interface AdminEscalationService {

    Page<Escalation> listEscalations(Escalation.Status statusFilter,
                                     EscalationSeverity severityFilter,
                                     ReportCaseResourceType resourceTypeFilter,
                                     Pageable pageable);

    Escalation getDetail(String escalationId);

    Escalation claim(String escalationId, CustomUserDetails admin, String clientIp);

    Escalation enforce(String escalationId, ViolationAction action,
                       String reason, String note,
                       CustomUserDetails admin, String clientIp);

    Escalation dismiss(String escalationId, String reason, String note,
                       CustomUserDetails admin, String clientIp);
}
