package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {

    Page<User> searchByKeyword(String q, Pageable pageable);
}