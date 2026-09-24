package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.reportcase.ReportCaseDecision;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseDetail;
import com.ecommerce.cnj70.dto.reportcase.ReportCaseQueueItem;
import com.ecommerce.cnj70.enums.EscalationSeverity;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.enums.ReportCaseStatus;
import com.ecommerce.cnj70.enums.ReportReason;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 2C — Moderator ReportCase Service.
 *
 * <h3>Contract</h3>
 * <ul>
 *     <li>{@link #listQueue} — list OPEN + IN_REVIEW cases, with
 *         optional filters by reason / resourceType / status.</li>
 *     <li>{@link #getDetail} — fetch a case with full context
 *         (resource, reporter, evidence, vendor history, previous cases).</li>
 *     <li>{@link #approve} — confirm the report is valid. Reason is
 *         optional but accepted as a note.</li>
 *     <li>{@link #reject} — dismiss the report. Reason is MANDATORY.</li>
 *     <li>{@link #escalate} — escalate to Admin. Reason MANDATORY,
 *         Severity required (Phase 2C §22 — Moderator picks, not derived).</li>
 *     <li>{@link #claim} — assign the case to the current Moderator
 *         (transitions OPEN → IN_REVIEW).</li>
 * </ul>
 *
 * <h3>Security</h3>
 * <ul>
 *     <li>Route-level: SecurityConfig.hasRole("MODERATOR") (Phase 1).</li>
 *     <li>Object-level: every action calls {@code loadAndGuard(caseId)}.</li>
 *     <li>Account-status: locked moderators cannot act (Phase 2C §29).</li>
 *     <li>State-level: terminal cases reject further actions with 409.</li>
 * </ul>
 *
 * <h3>History / Audit</h3>
 * <p>Every action writes a {@code ModerationHistory} entry
 * (resourceType="REPORTCASE", action via {@link
 * com.ecommerce.cnj70.enums.ModerationAction}) and emits an
 * {@link com.ecommerce.cnj70.dto.moderation.AuditEvent} via the
 * AuditEventWriter seam (Phase 2A).</p>
 */
public interface ModeratorReportCaseService {

    Page<ReportCaseQueueItem> listQueue(ReportCaseStatus statusFilter,
                                       ReportReason reasonFilter,
                                       ReportCaseResourceType resourceTypeFilter,
                                       Pageable pageable);

    ReportCaseDetail getDetail(String caseId);

    ReportCaseDecision approve(String caseId, String reason, String note,
                               CustomUserDetails moderator, String clientIp);

    ReportCaseDecision reject(String caseId, String reason, String note,
                              CustomUserDetails moderator, String clientIp);

    ReportCaseDecision escalate(String caseId, String reason, EscalationSeverity severity,
                                CustomUserDetails moderator, String clientIp);

    ReportCaseDecision claim(String caseId, CustomUserDetails moderator, String clientIp);
}
