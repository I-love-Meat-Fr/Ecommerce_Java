package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.dto.review.ReviewModerationContext;
import com.ecommerce.cnj70.dto.review.ReviewModerationDecision;
import com.ecommerce.cnj70.dto.review.ReviewModerationQueueItem;
import com.ecommerce.cnj70.enums.ModerationStatus;
import com.ecommerce.cnj70.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Phase 2B — Moderator Review Moderation Service.
 *
 * <p>Owns the Moderator-side review workflow:</p>
 * <ul>
 *     <li>Review Queue (search + filter by moderationStatus + rating + pagination)</li>
 *     <li>Review Detail (with Product / Shop / Vendor / Customer context + integration seams)</li>
 *     <li>Actions: Approve, Reject, Hide, Unhide, Escalate</li>
 * </ul>
 *
 * <p>Hard rules:</p>
 * <ul>
 *     <li>State transitions enforced on EVERY action — no silent overwrite</li>
 *     <li>Reject / Hide / Escalate require non-blank reason (Phase 2B §31)</li>
 *     <li>Locked account cannot perform actions (Phase 2B §46)</li>
 *     <li>Every action writes ModerationHistory + AuditEvent</li>
 *     <li>Review content is never modified by Moderator actions — only
 *         moderation metadata is set (Phase 2B §20)</li>
 *     <li>Verified Purchase / Report / Violation are READ-ONLY from
 *         integration seams — never recomputed by Phase 2B</li>
 * </ul>
 *
 * <p>State transitions (Phase 2B §30):</p>
 * <pre>
 *   PENDING_MANUAL → APPROVE   → APPROVED
 *   PENDING_MANUAL → REJECT    → REJECTED
 *   PENDING_MANUAL → HIDE      → moderationStatus unchanged, hidden=true
 *   VISIBLE        → HIDE      → hidden=true
 *   VISIBLE        → APPROVE   → APPROVED
 *   VISIBLE        → REJECT    → REJECTED
 *   HIDDEN         → UNHIDE    → hidden=false
 *   ANY            → ESCALATE  → ESCALATED
 *   FINAL          → *         → DENY (409 Conflict)
 * </pre>
 */
public interface ModeratorReviewService {

    Page<ReviewModerationQueueItem> listQueue(String q, ModerationStatus moderationStatus,
                                              Integer rating, Pageable pageable);

    ReviewModerationContext getContext(String reviewId);

    ReviewModerationDecision approve(String reviewId, CustomUserDetails moderator, String clientIp);

    ReviewModerationDecision reject(String reviewId, String reason, CustomUserDetails moderator, String clientIp);

    ReviewModerationDecision hide(String reviewId, String reason, CustomUserDetails moderator, String clientIp);

    ReviewModerationDecision unhide(String reviewId, CustomUserDetails moderator, String clientIp);

    ReviewModerationDecision escalate(String reviewId, String reason, CustomUserDetails moderator, String clientIp);
}
