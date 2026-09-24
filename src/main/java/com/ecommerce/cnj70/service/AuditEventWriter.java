package com.ecommerce.cnj70.service;

/**
 * Phase 2A — Audit Event Writer (integration seam).
 *
 * <p>AuditLog backend is owned by another team (Phase 2A §42). Phase 2A
 * defines this interface so Moderation actions can be audited without
 * coupling to a specific backend implementation.</p>
 *
 * <p>Phase 2A §42 contract:</p>
 * <ul>
 *     <li>Backend available  → real impl persists AuditEvent</li>
 *     <li>Backend unavailable → log-only no-op (NOT a fake production)</li>
 * </ul>
 */
public interface AuditEventWriter {

    /**
     * Persist (or queue) an audit event. Phase 2A passes an
     * implementation-agnostic event DTO so callers don't need to know
     * which AuditLog backend is active.
     */
    void write(com.ecommerce.cnj70.dto.moderation.AuditEvent event);
}
