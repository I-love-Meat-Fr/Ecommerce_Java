package com.ecommerce.cnj70.service;

/**
 * Phase 4B — Integration seam for Order/Checkout voucher application.
 *
 * <p>Per Phase 4B §30 + §34: voucher must only be applied (consumed)
 * AFTER the order is successfully created. If the order fails,
 * the voucher MUST be returned to the pool (decrement not applied).</p>
 *
 * <h3>Flow (production)</h3>
 * <pre>
 *   Cart  ───►  validateForCheckout(code)
 *      │
 *      ▼
 *   Order created ───► reserveVoucher(voucherId, orderId)  ◄── atomic used++ (only if used < quantity)
 *      │
 *      ├── Order succeeds  ──► commitReservation(orderId)  ──► usedCount persists
 *      │
 *      └── Order fails     ──► releaseReservation(orderId) ──► usedCount decremented (rollback)
 * </pre>
 *
 * <p>The current {@code VoucherController.placeOrderWithVoucher} already
 * calls {@code incrementUsed}, but it does so BEFORE the order is created.
 * Phase 4B makes the seam explicit so the Order/Checkout backend (Phase 5)
 * can integrate without touching Admin code.</p>
 *
 * <h3>Phase 4B implementation</h3>
 * <p>This seam is currently a thin facade over {@link VoucherService#tryIncrementUsed}
 * and {@link VoucherService#tryDecrementUsed}. The Order/Checkout consumer code
 * must call these methods and handle rollback semantics. The Order Backend
 * (Anh Quân / Phase 5) is the integration point.</p>
 */
public interface VoucherApplicationGateway {

    /**
     * Reserve a voucher for an order being created.
     *
     * @param voucherId voucher to consume
     * @param orderId order that triggered the reservation (for audit trail)
     * @return true if reserved (usedCount atomically incremented), false if exhausted/deactivated/missing.
     */
    boolean reserveVoucher(String voucherId, String orderId);

    /**
     * Release a previously reserved voucher (rollback).
     *
     * @param voucherId voucher to release
     * @param orderId order that triggered the release (for audit trail)
     * @return true if released (usedCount atomically decremented), false if not found or already at 0.
     */
    boolean releaseVoucher(String voucherId, String orderId);
}
