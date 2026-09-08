package com.ecommerce.cnj70.repository;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String>, UserRepositoryCustom {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByRoleAndStatus(UserRole role, String status);

    // === User Search+Filter: role only / status only / both ===
    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByStatus(AccountStatus status, Pageable pageable);

    Page<User> findByRoleAndStatus(UserRole role, AccountStatus status, Pageable pageable);
}
