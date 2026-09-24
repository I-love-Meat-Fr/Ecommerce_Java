package com.ecommerce.cnj70.enums;

/**
 * Phase 3B — KYC Provider source. Mirrors what the external KYC
 * backend reports back; not an Admin-configurable field.
 *
 * <p>{@code MOCK} is reserved for the no-op integration profile so
 * test environments can produce deterministic KYC records without
 * touching a real Provider. Production will receive VNPT / FPT /
 * OTHER from the real backend.</p>
 */
public enum KycProvider {
    VNPT,
    FPT,
    MOCK,
    OTHER
}
