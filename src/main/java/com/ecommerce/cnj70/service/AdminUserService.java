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

    AccountStatus getCurrentStatus(String id);
}
