package com.ecommerce.cnj70.service.impl;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.AuditAction;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.exception.BusinessException;
import com.ecommerce.cnj70.exception.ResourceNotFoundException;
import com.ecommerce.cnj70.repository.UserRepository;
import com.ecommerce.cnj70.service.AuditLogService;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho AdminUserServiceImpl — Phase 3 (User Management).
 *
 * Phase 3 — getUserById() đã đổi sang raw Document query qua MongoTemplate
 * (để tránh findById(ObjectId) không match _id String). Test stub MongoCollection
 * để giả lập raw query trả về User.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private MongoTemplate mongoTemplate;
    @Mock private AuditLogService auditLogService;
    @Mock private MongoCollection<Document> mongoCollection;

    @InjectMocks private AdminUserServiceImpl service;

    private User sampleUser(String id, UserRole role, AccountStatus status) {
        User u = new User();
        u.setId(id);
        u.setEmail(id + "@test.com");
        u.setRole(role);
        u.setStatus(status);
        return u;
    }

    /**
     * Stub raw getUserById(): mongoTemplate.getCollection("users").find(filter).first() → Document
     */
    @SuppressWarnings("unchecked")
    private void stubRawFindOne(User user) {
        Document raw = new Document("_id", user.getId())
                .append("email", user.getEmail())
                .append("role", user.getRole() != null ? user.getRole().name() : null)
                .append("status", user.getStatus() != null ? user.getStatus().name() : null);

        FindIterable<Document> findIterable = mock(FindIterable.class);
        when(mongoTemplate.getCollection("users")).thenReturn(mongoCollection);
        when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(raw);
        when(mongoTemplate.getConverter()).thenReturn(mock(org.springframework.data.mongodb.core.convert.MongoConverter.class));
        when(mongoTemplate.getConverter().read(eq(User.class), eq(raw))).thenReturn(user);
    }

    @SuppressWarnings("unchecked")
    private void stubRawFindOneReturnsNull() {
        FindIterable<Document> findIterable = mock(FindIterable.class);
        when(mongoTemplate.getCollection("users")).thenReturn(mongoCollection);
        when(mongoCollection.find(any(Bson.class))).thenReturn(findIterable);
        when(findIterable.first()).thenReturn(null);
    }

    @Test
    void lockUser_happyPath_writesAudit() {
        User u = sampleUser("u1", UserRole.CUSTOMER, AccountStatus.ACTIVE);
        stubRawFindOne(u);

        service.lockUser("u1", "admin");

        assertEquals(AccountStatus.LOCKED, u.getStatus());
        verify(auditLogService).logWarning(
                eq(AuditAction.USER_LOCKED),
                eq("USER"),
                eq("u1"),
                eq("admin"),
                eq(null),
                eq("ADMIN"),
                any(String.class));
    }

    @Test
    void lockUser_adminCannotLockSelf() {
        User admin = sampleUser("a1", UserRole.ADMIN, AccountStatus.ACTIVE);
        stubRawFindOne(admin);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.lockUser("a1", "a1"));
        org.junit.jupiter.api.Assertions.assertTrue(
                ex.getMessage() != null && ex.getMessage().toLowerCase().contains("admin"),
                "Message phải đề cập admin / tự khóa. Actual: " + ex.getMessage());

        verify(auditLogService, never()).logWarning(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void lockUser_moderatorCannotLockSelf() {
        // Phase 3 §1.2 / §3.9 — MODERATOR cũng không được tự khóa.
        User mod = sampleUser("m1", UserRole.MODERATOR, AccountStatus.ACTIVE);
        stubRawFindOne(mod);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.lockUser("m1", "m1"));
        org.junit.jupiter.api.Assertions.assertTrue(
                ex.getMessage() != null && ex.getMessage().toLowerCase().contains("moderator"),
                "Message phải đề cập moderator / tự khóa. Actual: " + ex.getMessage());

        verify(auditLogService, never()).logWarning(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void lockUser_userNotFound() {
        stubRawFindOneReturnsNull();

        assertThrows(ResourceNotFoundException.class,
                () -> service.lockUser("nope", "admin"));

        verify(auditLogService, never()).logWarning(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void unlockUser_happyPath() {
        User u = sampleUser("u2", UserRole.CUSTOMER, AccountStatus.LOCKED);
        stubRawFindOne(u);

        service.unlockUser("u2");

        assertEquals(AccountStatus.ACTIVE, u.getStatus());
        verify(auditLogService).logInfo(
                eq(AuditAction.USER_UNLOCKED),
                eq("USER"),
                eq("u2"),
                eq(null),
                eq(null),
                eq("ADMIN"),
                any(String.class));
    }

    @Test
    void updateUserStatus_adminCannotEditOwnActiveToOther() {
        // Phase 3 §3.7 — Admin không được tự đổi status ACTIVE của chính mình.
        User admin = sampleUser("a1", UserRole.ADMIN, AccountStatus.ACTIVE);
        stubRawFindOne(admin);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateUserStatus("a1", AccountStatus.LOCKED, "a1"));
        org.junit.jupiter.api.Assertions.assertTrue(
                ex.getMessage() != null && ex.getMessage().toLowerCase().contains("chính mình"),
                "Message phải đề cập 'chính mình'. Actual: " + ex.getMessage());

        verify(auditLogService, never()).logInfo(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void updateUserStatus_noOpThrows() {
        // Phase 3 §3.8 — không cho ACTIVE → ACTIVE (no-op).
        User u = sampleUser("u3", UserRole.CUSTOMER, AccountStatus.ACTIVE);
        stubRawFindOne(u);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateUserStatus("u3", AccountStatus.ACTIVE, "admin"));
        org.junit.jupiter.api.Assertions.assertTrue(
                ex.getMessage() != null && ex.getMessage().toLowerCase().contains("không có thay đổi"),
                "Message phải đề cập 'không có thay đổi'. Actual: " + ex.getMessage());
    }
}
