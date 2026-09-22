package com.ecommerce.cnj70.security;

import com.ecommerce.cnj70.config.SecurityConfig;
import com.ecommerce.cnj70.document.User;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TASK #25 — Security Compliance Test.
 *
 * Test route/method security cho từng role và locked account.
 *
 * Test cases:
 * - Customer: chỉ truy cập được endpoint customer
 * - Vendor: truy cập được vendor + customer endpoints
 * - Admin: truy cập được admin + vendor + customer endpoints
 * - Moderator: truy cập được moderation endpoints
 * - Locked account: bị chặn trên mọi endpoint (trừ public)
 * - Unauthenticated: bị chặn trên protected endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TASK #25: Security Compliance Test")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class SecurityComplianceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    // ============================================================
    // Setup: user entities cho mock authentication
    // ============================================================
    private static User customerUser;
    private static User vendorUser;
    private static User adminUser;
    private static User moderatorUser;
    private static User lockedUser;

    @BeforeAll
    static void setUpUsers() {
        customerUser = User.builder()
                .id("customer-001")
                .email("customer@test.com")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        vendorUser = User.builder()
                .id("vendor-001")
                .email("vendor@test.com")
                .role(UserRole.VENDOR)
                .status(AccountStatus.ACTIVE)
                .build();

        adminUser = User.builder()
                .id("admin-001")
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .status(AccountStatus.ACTIVE)
                .build();

        moderatorUser = User.builder()
                .id("mod-001")
                .email("mod@test.com")
                .role(UserRole.MODERATOR)
                .status(AccountStatus.ACTIVE)
                .build();

        lockedUser = User.builder()
                .id("locked-001")
                .email("locked@test.com")
                .role(UserRole.CUSTOMER)
                .status(AccountStatus.LOCKED)
                .build();
    }

    // ============================================================
    // SECTION 1: Public endpoints — không cần authentication
    // ============================================================

    @Nested
    @DisplayName("S1: Public endpoints (no auth required)")
    class PublicEndpoints {

        @Test
        @DisplayName("S1-TC01: GET / — trả 200, không cần auth")
        void getHomePage_noAuth_200() throws Exception {
            mockMvc.perform(get("/"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S1-TC02: GET /api/products — không cần auth")
        void getProducts_noAuth_200() throws Exception {
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S1-TC03: GET /api/categories — không cần auth")
        void getCategories_noAuth_200() throws Exception {
            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S1-TC04: GET /api/legal — không cần auth")
        void getLegalDocuments_noAuth_200() throws Exception {
            mockMvc.perform(get("/api/legal"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S1-TC05: POST /api/auth/register — không cần auth")
        void register_noAuth_200() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType("application/json")
                            .content("""
                                {
                                    "email": "newuser@test.com",
                                    "password": "password123",
                                    "fullName": "New User",
                                    "acceptTerms": true,
                                    "acceptPrivacy": true
                                }
                                """))
                    .andExpect(status().isOk());
        }
    }

    // ============================================================
    // SECTION 2: Customer-only endpoints
    // ============================================================

    @Nested
    @DisplayName("S2: Customer role access")
    class CustomerRoleAccess {

        @Test
        @DisplayName("S2-TC01: Customer truy cập /cart — 200")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerAccessCart_200() throws Exception {
            mockMvc.perform(get("/cart"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S2-TC02: Customer truy cập /orders — 200")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerAccessOrders_200() throws Exception {
            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S2-TC03: Customer truy cập /my-reviews — 200")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerAccessMyReviews_200() throws Exception {
            mockMvc.perform(get("/my-reviews"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S2-TC04: Customer truy cập /admin/** — 403 Forbidden")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerAccessAdmin_403() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S2-TC05: Customer truy cập /vendor/** — 403 Forbidden")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerAccessVendor_403() throws Exception {
            mockMvc.perform(get("/vendor/dashboard"))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // SECTION 3: Vendor role access
    // ============================================================

    @Nested
    @DisplayName("S3: Vendor role access")
    class VendorRoleAccess {

        @Test
        @DisplayName("S3-TC01: Vendor truy cập /vendor/dashboard — 200")
        @WithMockUser(username = "vendor@test.com", roles = {"VENDOR"})
        void vendorAccessDashboard_200() throws Exception {
            mockMvc.perform(get("/vendor/dashboard"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S3-TC02: Vendor truy cập /vendor/products — 200")
        @WithMockUser(username = "vendor@test.com", roles = {"VENDOR"})
        void vendorAccessProducts_200() throws Exception {
            mockMvc.perform(get("/vendor/products"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S3-TC03: Vendor truy cập /vendor/orders — 200")
        @WithMockUser(username = "vendor@test.com", roles = {"VENDOR"})
        void vendorAccessOrders_200() throws Exception {
            mockMvc.perform(get("/vendor/orders"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S3-TC04: Vendor truy cập POST /api/kyc/submit — 200")
        @WithMockUser(username = "vendor@test.com", roles = {"VENDOR"})
        void vendorAccessKycSubmit_200() throws Exception {
            mockMvc.perform(post("/api/kyc/submit")
                            .contentType("application/json")
                            .content("""
                                {
                                    "citizenId": "001234567890"
                                }
                                """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S3-TC05: Vendor truy cập /admin/** — 403 Forbidden")
        @WithMockUser(username = "vendor@test.com", roles = {"VENDOR"})
        void vendorAccessAdmin_403() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S3-TC06: Vendor truy cập GET /api/kyc/status — 200")
        @WithMockUser(username = "vendor@test.com", roles = {"VENDOR"})
        void vendorAccessKycStatus_200() throws Exception {
            mockMvc.perform(get("/api/kyc/status"))
                    .andExpect(status().isOk());
        }
    }

    // ============================================================
    // SECTION 4: Admin role access
    // ============================================================

    @Nested
    @DisplayName("S4: Admin role access")
    class AdminRoleAccess {

        @Test
        @DisplayName("S4-TC01: Admin truy cập /admin/users — 200")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void adminAccessUsers_200() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S4-TC02: Admin truy cập /admin/shops — 200")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void adminAccessShops_200() throws Exception {
            mockMvc.perform(get("/admin/shops"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S4-TC03: Admin truy cập /admin/reviews — 200")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void adminAccessReviews_200() throws Exception {
            mockMvc.perform(get("/admin/reviews"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S4-TC04: Admin truy cập PUT /api/legal/{type} — 200")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void adminUpdateLegalDocument_200() throws Exception {
            mockMvc.perform(put("/api/legal/TERMS")
                            .contentType("application/json")
                            .content("""
                                {
                                    "title": "Điều khoản sử dụng (updated)",
                                    "content": "# Cập nhật điều khoản"
                                }
                                """))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S4-TC05: Admin truy cập /api/admin/audit-logs — 200")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void adminAccessAuditLogs_200() throws Exception {
            mockMvc.perform(get("/api/admin/audit-logs"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S4-TC06: Admin truy cập POST /admin/shops/{id}/approve — 200")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void adminApproveShop_200() throws Exception {
            mockMvc.perform(post("/admin/shops/test-shop-id/approve"))
                    .andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("S4-TC07: Customer truy cập /api/admin/audit-logs — 403")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerAccessAuditLogs_403() throws Exception {
            mockMvc.perform(get("/api/admin/audit-logs"))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // SECTION 5: Locked account bị chặn
    // ============================================================

    @Nested
    @DisplayName("S5: Locked account bị chặn")
    class LockedAccountTests {

        @Test
        @DisplayName("S5-TC01: Locked account truy cập /orders — 403")
        @WithMockUser(username = "locked@test.com", roles = {"CUSTOMER"})
        void lockedAccountAccessOrders_403() throws Exception {
            mockMvc.perform(get("/orders"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S5-TC02: Locked account truy cập POST /checkout — 403")
        @WithMockUser(username = "locked@test.com", roles = {"CUSTOMER"})
        void lockedAccountAccessCheckout_403() throws Exception {
            mockMvc.perform(post("/checkout"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S5-TC03: Locked account truy cập /api/orders/** — 403")
        @WithMockUser(username = "locked@test.com", roles = {"CUSTOMER"})
        void lockedAccountAccessApiOrders_403() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S5-TC04: Locked account truy cập /admin/** — 403")
        @WithMockUser(username = "locked@test.com", roles = {"ADMIN"})
        void lockedAdminAccessAdmin_403() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S5-TC05: Locked account vẫn xem được sản phẩm công khai")
        @WithMockUser(username = "locked@test.com", roles = {"CUSTOMER"})
        void lockedAccountViewProducts_200() throws Exception {
            // Public endpoints vẫn cho phép
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk());
        }
    }

    // ============================================================
    // SECTION 6: Unauthenticated access bị chặn
    // ============================================================

    @Nested
    @DisplayName("S6: Unauthenticated access bị chặn")
    class UnauthenticatedTests {

        @Test
        @DisplayName("S6-TC01: Unauthenticated truy cập /orders — 401/302")
        void unauthAccessOrders_redirectOr401() throws Exception {
            mockMvc.perform(get("/orders"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("S6-TC02: Unauthenticated truy cập /api/orders — 401")
        void unauthAccessApiOrders_401() throws Exception {
            mockMvc.perform(get("/api/orders"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("S6-TC03: Unauthenticated truy cập POST /api/cart — 401")
        void unauthAccessCart_401() throws Exception {
            mockMvc.perform(post("/api/cart"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("S6-TC04: Unauthenticated truy cập /admin/** — 401/403")
        void unauthAccessAdmin_403() throws Exception {
            mockMvc.perform(get("/admin/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S6-TC05: Unauthenticated truy cập /vendor/** — 401/403")
        void unauthAccessVendor_403() throws Exception {
            mockMvc.perform(get("/vendor/dashboard"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("S6-TC06: Unauthenticated truy cập POST /api/kyc/submit — 401")
        void unauthAccessKycSubmit_401() throws Exception {
            mockMvc.perform(post("/api/kyc/submit")
                            .contentType("application/json")
                            .content("{\"citizenId\": \"001234567\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ============================================================
    // SECTION 7: CSRF Protection
    // ============================================================

    @Nested
    @DisplayName("S7: CSRF Protection")
    class CsrfProtectionTests {

        @Test
        @DisplayName("S7-TC01: POST không có CSRF token → 403 (public form)")
        void postWithoutCsrf_403() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType("application/json")
                            .content("""
                                {
                                    "email": "csrf@test.com",
                                    "password": "test123",
                                    "fullName": "CSRF Test",
                                    "acceptTerms": true,
                                    "acceptPrivacy": true
                                }
                                """))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ============================================================
    // SECTION 8: HTTP Method security
    // ============================================================

    @Nested
    @DisplayName("S8: HTTP Method restrictions")
    class HttpMethodSecurity {

        @Test
        @DisplayName("S8-TC01: GET /admin/shops/{id}/approve — 405 Method Not Allowed")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void getApproveShop_405() throws Exception {
            mockMvc.perform(get("/admin/shops/test-id/approve"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("S8-TC02: POST /admin/shops — 405 (chỉ GET và form POST được)")
        @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
        void postAdminShops_405() throws Exception {
            mockMvc.perform(post("/admin/shops"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("S8-TC03: PUT /api/legal không có quyền Admin → 403")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerPutLegal_403() throws Exception {
            mockMvc.perform(put("/api/legal/TERMS")
                            .contentType("application/json")
                            .content("{\"title\": \"X\", \"content\": \"Y\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ============================================================
    // SECTION 9: Moderator role access
    // ============================================================

    @Nested
    @DisplayName("S9: Moderator role access")
    class ModeratorRoleAccess {

        @Test
        @DisplayName("S9-TC01: Moderator truy cập /admin/reviews — 200")
        @WithMockUser(username = "mod@test.com", roles = {"MODERATOR"})
        void moderatorAccessReviews_200() throws Exception {
            mockMvc.perform(get("/admin/reviews"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S9-TC02: Moderator truy cập /admin/moderation — 200")
        @WithMockUser(username = "mod@test.com", roles = {"MODERATOR"})
        void moderatorAccessModeration_200() throws Exception {
            mockMvc.perform(get("/admin/moderation"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("S9-TC03: Customer truy cập /admin/moderation — 403")
        @WithMockUser(username = "customer@test.com", roles = {"CUSTOMER"})
        void customerAccessModeration_403() throws Exception {
            mockMvc.perform(get("/admin/moderation"))
                    .andExpect(status().isForbidden());
        }
    }
}
