package com.ecommerce.cnj70.exception;

/**
 * Phase 2A — Conflict (HTTP 409) Exception.
 *
 * <p>Thrown when an action conflicts with the current state of a resource,
 * e.g. when two moderators attempt to approve/reject the same product at
 * the same time (Phase 2A §39), or when an action is attempted on a
 * product that has already reached a final state (Phase 2A §35).</p>
 *
 * <p>Mapped to HTTP 409 by {@code GlobalExceptionHandler}.</p>
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
