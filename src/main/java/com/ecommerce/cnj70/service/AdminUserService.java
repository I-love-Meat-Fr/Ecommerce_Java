package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<User> listUsers(Pageable pageable, String q);

    Page<User> listUsers(Pageable pageable, String q, UserRole role, AccountStatus status);

    User getUserById(String id);

    void lockUser(String id, String currentUserId);

    void unlockUser(String id);

    /**
     * Phase 3 §3.7 — Edit User Status.
     * Cho phép Admin/Moderator đổi status giữa ACTIVE / LOCKED / UNVERIFIED
     * (AccountStatus enum hiện có). Có self-action protection, validation
     * transition, và AuditLog.
     */
    void updateUserStatus(String id, AccountStatus newStatus, String currentUserId);

    AccountStatus getCurrentStatus(String id);
}
