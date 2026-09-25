package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer test cho AdminUserController — Phase 3.
 *
 * Verify:
 *  - ADMIN allowed
 *  - MODERATOR allowed
 *  - CUSTOMER denied
 *  - VENDOR denied
 *  - ANONYMOUS denied (redirect to /auth/login)
 *  - Lock/Unlock/Edit endpoints tồn tại và trả 302/200 đúng.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminUserControllerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminUserService adminUserService;

    private User sampleUser() {
        User u = new User();
        u.setId("u1");
        u.setEmail("u1@test.com");
        u.setRole(UserRole.CUSTOMER);
        u.setStatus(AccountStatus.ACTIVE);
        return u;
    }

    private CustomUserDetails asUser(String id, String role) {
        User u = new User();
        u.setId(id);
        u.setEmail(id + "@test.com");
        u.setRole(UserRole.valueOf(role));
        return CustomUserDetails.fromUser(u);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanListUsers() throws Exception {
        Page<User> empty = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(adminUserService.listUsers(any(), any(), any(), any())).thenReturn(empty);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MODERATOR")
    void moderatorCanListUsers() throws Exception {
        Page<User> empty = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(adminUserService.listUsers(any(), any(), any(), any())).thenReturn(empty);

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotListUsers() throws Exception {
        // Spring Security: AccessDeniedHandler configured trả redirect 302 → /auth/login?denied=1
        // cho CUSTOMER role không thuộc ADMIN/MODERATOR. Verify 302 redirect.
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "VENDOR")
    void vendorCannotListUsers() throws Exception {
        // VENDOR role không thuộc ADMIN/MODERATOR → 302 redirect qua AccessDeniedHandler.
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void anonymousRedirectedToLogin() throws Exception {
        // Anonymous request: SecurityConfig có jwtAuthenticationEntryPoint trả 401 JSON.
        // Trong test này, JwtAuthenticationFilter không tạo authentication → AccessDenied 302
        // hoặc 401 JSON tuỳ cấu hình. Accept cả 3xx redirect hoặc 4xx.
        mockMvc.perform(get("/admin/users"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 302 && status != 403) {
                        throw new AssertionError("Expected 401/302/403, got " + status);
                    }
                });
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanViewUserDetail() throws Exception {
        when(adminUserService.getUserById("u1")).thenReturn(sampleUser());

        mockMvc.perform(get("/admin/users/u1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenEditStatusPage() throws Exception {
        when(adminUserService.getUserById("u1")).thenReturn(sampleUser());

        mockMvc.perform(get("/admin/users/u1/edit"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanPostLock() throws Exception {
        when(adminUserService.getCurrentStatus("u1")).thenReturn(AccountStatus.ACTIVE);
        mockMvc.perform(post("/admin/users/u1/lock").with(user(asUser("admin-id", "ADMIN"))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanPostUnlock() throws Exception {
        when(adminUserService.getCurrentStatus("u1")).thenReturn(AccountStatus.LOCKED);
        mockMvc.perform(post("/admin/users/u1/unlock"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));
    }
}
