package com.ecommerce.cnj70.service;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<User> listUsers(Pageable pageable, String q);

    User getUserById(String id);

    void lockUser(String id, String currentUserId);

    void unlockUser(String id);

    AccountStatus getCurrentStatus(String id);
}