package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.document.Complaint;
import com.ecommerce.cnj70.service.ComplaintService;
import com.ecommerce.cnj70.service.SchedulerIdempotencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 3C — Complaint Escalation Scheduler (Production-safe).
 *
 * <p>Architecture:</p>
 * <pre>
 *   @Scheduled (cron trigger)
 *     │
 *     ├── Scheduler Job (this class — orchestration only)
 *     │     │
 *     │     ├── SchedulerIdempotencyService — concurrency / restart guard
 *     │     │
 *     │     ├── ComplaintService.findOverdue... — eligibility / deadline
 *     │     │
 *     │     ├── ComplaintService.autoEscalate... — business mutation
 *     │     │
 *     │     └── AuditEventWriter — audit trail
 *     │
 *     └── Logs each step (root cause on failure, no silent swallow)
 * </pre>
 *
 * <h3>Phase 3C Requirements</h3>
 * <ul>
 *     <li>Idempotent: same operation never runs twice (persisted in MongoDB).</li>
 *     <li>Restart-safe: state persists across application restarts.</li>
 *     <li>Concurrency-safe: unique-index guards against race conditions.</li>
 *     <li>Per-record failure isolation: one bad record doesn't kill the batch.</li>
 *     <li>System actor: scheduler runs without authenticated principal.</li>
 *     <li>Audit: scheduler actions are recorded with role=SYSTEM/MODERATOR.</li>
 * </ul>
 *
 * <h3>Schedule</h3>
 * <p>Cron expressions are configurable via {@code application.yml}:
 * <ul>
 *     <li>{@code complaint.scheduler.cron} — Level 0 → Level 1 (default: every 15 minutes)</li>
 *     <li>{@code complaint.scheduler.cron-moderator} — Level 1 → Level 2 (default: hourly)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComplaintEscalationScheduler {

    /** Job name for Level 0 → Level 1 escalation. */
    public static final String JOB_L0_ESCALATION = "complaint-l0-escalation";

    /** Job name for Level 1 → Level 2 escalation. */
    public static final String JOB_L1_ESCALATION = "complaint-l1-escalation";

    /** Operation name for escalation. */
    public static final String OP_ESCALATE = "ESCALATE";

    /** Entity type for idempotency records. */
    public static final String ENTITY_COMPLAINT = "COMPLAINT";

    private final ComplaintService complaintService;
    private final SchedulerIdempotencyService idempotencyService;

    /**
     * Phase 3 §31 / Phase 3C — Level 0 overdue → Level 1 escalation.
     */
    @Scheduled(cron = "${complaint.scheduler.cron:0 */15 * * * *}")
    public void escalateOverdueLevel0() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Scheduler [L0] starting at {}", now);

        List<Complaint> overdue;
        try {
            overdue = complaintService.findOverdueForLevel0Escalation(now);
        } catch (RuntimeException ex) {
            log.error("Scheduler [L0] lookup failed: {}", ex.getMessage(), ex);
            return;
        }
        if (overdue.isEmpty()) {
            log.debug("Scheduler [L0] no overdue complaints at {}", now);
            return;
        }
        log.info("Scheduler [L0] found {} overdue complaints at {}", overdue.size(), now);

        int escalated = 0, skipped = 0, failed = 0;
        for (Complaint c : overdue) {
            try {
                processOne(JOB_L0_ESCALATION, c);
                escalated++;
            } catch (RuntimeException ex) {
                failed++;
                log.error("Scheduler [L0] failure for complaintId={}: {}", c.getId(), ex.getMessage(), ex);
            }
        }
        log.info("Scheduler [L0] complete: escalated={} skipped={} failed={}", escalated, skipped, failed);
    }

    /**
     * Phase 3 §31 / Phase 3C — Level 1 overdue → Level 2 escalation.
     */
    @Scheduled(cron = "${complaint.scheduler.cron-moderator:0 0 * * * *}")
    public void escalateOverdueLevel1() {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Scheduler [L1] starting at {}", now);

        List<Complaint> overdue;
        try {
            overdue = complaintService.findOverdueForLevel2Escalation(now);
        } catch (RuntimeException ex) {
            log.error("Scheduler [L1] lookup failed: {}", ex.getMessage(), ex);
            return;
        }
        if (overdue.isEmpty()) {
            log.debug("Scheduler [L1] no overdue complaints at {}", now);
            return;
        }
        log.info("Scheduler [L1] found {} overdue complaints at {}", overdue.size(), now);

        int escalated = 0, skipped = 0, failed = 0;
        for (Complaint c : overdue) {
            try {
                processOne(JOB_L1_ESCALATION, c);
                escalated++;
            } catch (RuntimeException ex) {
                failed++;
                log.error("Scheduler [L1] failure for complaintId={}: {}", c.getId(), ex.getMessage(), ex);
            }
        }
        log.info("Scheduler [L1] complete: escalated={} skipped={} failed={}", escalated, skipped, failed);
    }

    /**
     * Process one overdue complaint with full idempotency protection.
     * Per-record failure isolation: throws RuntimeException to be caught
     * by the batch loop, never silently swallowed.
     */
    private void processOne(String jobName, Complaint c) {
        // Phase 3C §B — Idempotency check via stable operation key
        if (!idempotencyService.tryReserve(jobName, ENTITY_COMPLAINT, c.getId(), OP_ESCALATE)) {
            log.info("Scheduler [{}] skip complaint {} — already attempted", jobName, c.getId());
            return;
        }

        try {
            // Re-read the complaint to ensure fresh state (concurrency safety)
            // (the findOverdue query may return a stale snapshot)
            Complaint fresh = complaintService.getById(c.getId(),
                    systemPrincipal()); // System principal — bypasses role check

            // Phase 3C §A — Deadline re-check at execution time
            // If deadline is no longer exceeded (e.g. vendor responded between
            // find and process), skip without applying.
            if (jobName.equals(JOB_L0_ESCALATION)
                    && (fresh.getStatus().isTerminal()
                    || fresh.getLevel() != com.ecommerce.cnj70.enums.ComplaintLevel.LEVEL_0)) {
                idempotencyService.markSkipped(jobName, ENTITY_COMPLAINT, c.getId(),
                        "State changed — no longer eligible");
                log.info("Scheduler [{}] skip complaint {} — state changed (level={}, status={})",
                        jobName, c.getId(), fresh.getLevel(), fresh.getStatus());
                return;
            }
            if (jobName.equals(JOB_L1_ESCALATION)
                    && fresh.getLevel() != com.ecommerce.cnj70.enums.ComplaintLevel.LEVEL_1) {
                idempotencyService.markSkipped(jobName, ENTITY_COMPLAINT, c.getId(),
                        "Level changed — no longer eligible");
                log.info("Scheduler [{}] skip complaint {} — level changed to {}",
                        jobName, c.getId(), fresh.getLevel());
                return;
            }

            // Apply escalation
            Complaint result = jobName.equals(JOB_L0_ESCALATION)
                    ? complaintService.autoEscalateToLevel1(fresh)
                    : complaintService.autoEscalateToLevel2(fresh);

            // Verify mutation actually occurred (defense-in-depth)
            boolean changed = (jobName.equals(JOB_L0_ESCALATION)
                    && result.getLevel() == com.ecommerce.cnj70.enums.ComplaintLevel.LEVEL_1)
                    || (jobName.equals(JOB_L1_ESCALATION)
                    && result.getLevel() == com.ecommerce.cnj70.enums.ComplaintLevel.LEVEL_2);

            if (changed) {
                idempotencyService.markSuccess(jobName, ENTITY_COMPLAINT, c.getId());
                log.info("Scheduler [{}] escalated complaint {} → level={}",
                        jobName, c.getId(), result.getLevel());
            } else {
                idempotencyService.markSkipped(jobName, ENTITY_COMPLAINT, c.getId(),
                        "autoEscalate returned without state change");
                log.info("Scheduler [{}] complaint {} — no state change (idempotent no-op)",
                        jobName, c.getId());
            }
        } catch (RuntimeException ex) {
            // Mark as failed and re-throw so the batch loop counts it.
            idempotencyService.markFailed(jobName, ENTITY_COMPLAINT, c.getId(),
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            throw ex;
        }
    }

    /**
     * Build a system principal for scheduler-driven reads.
     * This bypasses role-based ownership checks (since the scheduler has no
     * authenticated principal) but the underlying business logic still applies.
     */
    private com.ecommerce.cnj70.security.CustomUserDetails systemPrincipal() {
        // Use a special "system" user identity; the read path does not require
        // ownership — only the writes do, and those are handled by the service.
        com.ecommerce.cnj70.security.CustomUserDetails sys =
                new com.ecommerce.cnj70.security.CustomUserDetails(
                        "SYSTEM",
                        "scheduler@local",
                        "",
                        "Scheduler System",
                        "SYSTEM",
                        "ACTIVE",
                        null,
                        null
                );
        return sys;
    }
}
