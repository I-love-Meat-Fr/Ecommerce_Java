package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    public Page<User> listUsers(Pageable pageable, String q) {
        if (StringUtils.hasText(q)) {
            return userRepository.searchByKeyword(q.trim(), pageable);
        }
        return userRepository.findAll(pageable);
    }

    @Override
    public User getUserById(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User", "id", id));
    }

    @Override
    public void lockUser(String id, String currentUserId) {
        User user = getUserById(id);

        // Admin Protection: ADMIN cannot lock their own ADMIN account.
        if (user.getRole() == UserRole.ADMIN
                && currentUserId != null
                && currentUserId.equals(user.getId())) {
            throw new BusinessException(
                    "Bạn không thể tự khóa tài khoản ADMIN của chính mình");
        }

        if (user.getStatus() == AccountStatus.LOCKED) {
            log.info("AdminUserService.lockUser: user {} already LOCKED, skip", id);
            return;
        }

        user.setStatus(AccountStatus.LOCKED);
        userRepository.save(user);
        log.info("AdminUserService.lockUser: user {} locked by {}", id, currentUserId);
    }

    @Override
    public void unlockUser(String id) {
        User user = getUserById(id);

        if (user.getStatus() == AccountStatus.ACTIVE) {
            log.info("AdminUserService.unlockUser: user {} already ACTIVE, skip", id);
            return;
        }

        user.setStatus(AccountStatus.ACTIVE);
        userRepository.save(user);
        log.info("AdminUserService.unlockUser: user {} unlocked", id);
    }

    @Override
    public AccountStatus getCurrentStatus(String id) {
        return getUserById(id).getStatus();
    }
}