package com.ecommerce.cnj70.enums;

/**
 * Phase 2C — Severity levels for ReportCase escalation.
 *
 * <p>Used by Moderator when escalating a Case to Admin. Aligned with
 * marketplace {@code ViolationSeverity} (Phase 3+). Phase 2C never
 * auto-derives severity — Moderator picks from this list explicitly.</p>
 */
public enum EscalationSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
