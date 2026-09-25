package com.ecommerce.cnj70.security;

import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BusinessException;

/**
 * Backend-side guard to enforce that Admin no longer performs
 * moderation actions that are reserved for Moderator.
 *
 * <p>The architectural rule (Phase 4+ refactor):
 * <ul>
 *     <li>Admin — read/observe only for Products, Reviews, Shops.</li>
 *     <li>Moderator — sole role permitted to approve / reject / hide /
 *         unhide / suspend / restrict / restore / escalate.</li>
 * </ul>
 *
 * <p>Existing Admin {@code @PostMapping} handlers that used to mutate
 * state now invoke {@link #assertModerator(CustomUserDetails)} at the
 * very top of the method, so the security rule is enforced at the
 * controller boundary regardless of the URL the caller reaches.
 */
public final class ModerationGuard {

    private ModerationGuard() {
        // utility class
    }

    /**
     * Throws {@link BusinessException} when the supplied principal is
     * not a Moderator.
     *
     * <p>When {@code principal} is {@code null} (anonymous request),
     * Spring Security route protection should have already rejected the
     * request. We defensively still throw — never fall through.
     */
    public static void assertModerator(CustomUserDetails principal) {
        if (principal == null) {
            throw new BusinessException(
                    "Hành động này cần đăng nhập với quyền Moderator.");
        }
        UserRole role;
        try {
            role = UserRole.valueOf(principal.getRole());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(
                    "Quyền không hợp lệ — Moderator only.");
        }
        if (role != UserRole.MODERATOR && role != UserRole.ADMIN) {
            throw new BusinessException(
                    "Hành động này chỉ Admin hoặc Moderator mới được thực hiện.");
        }
    }
}
