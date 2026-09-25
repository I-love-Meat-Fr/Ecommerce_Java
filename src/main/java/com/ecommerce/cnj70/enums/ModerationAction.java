package com.ecommerce.cnj70.enums;

/**
 * Phase 2A/2B — Moderation Decision Action.
 *
 * <p>Records what action a Moderator performed against a Product (2A)
 * or Review (2B). Persisted in {@code ModerationHistory} for audit
 * trail (Phase 2A §41, Phase 2B §38).</p>
 *
 * <p>Phase 2B extended with HIDE / UNHIDE — same enum reused, no
 * duplicate enumeration (Phase 2B §23).</p>
 *
 * <p>Phase 6 — Shop moderation extended with SUSPEND / RESTORE / RESTRICT
 * for Moderator Shop queue (read-only Shop Moderation contract).</p>
 */
public enum ModerationAction {
    APPROVE,
    REJECT,
    ESCALATE,
    HIDE,
    UNHIDE,
    /** Moderator Shop moderation — suspend shop pending Admin enforcement. */
    SUSPEND,
    /** Moderator Shop moderation — restore shop to its previous status. */
    RESTORE,
    /** Moderator Shop moderation — apply a lighter restriction. */
    RESTRICT
}
