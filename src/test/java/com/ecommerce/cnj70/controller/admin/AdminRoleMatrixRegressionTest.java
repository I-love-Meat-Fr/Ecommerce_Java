package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.service.AdminAuditLogService;
import com.ecommerce.cnj70.service.AdminBannerService;
import com.ecommerce.cnj70.service.AdminCategoryService;
import com.ecommerce.cnj70.service.AdminEscalationService;
import com.ecommerce.cnj70.service.AdminKycService;
import com.ecommerce.cnj70.service.AdminOrderService;
import com.ecommerce.cnj70.service.AdminProductService;
import com.ecommerce.cnj70.service.AdminReviewService;
import com.ecommerce.cnj70.service.AdminShopService;
import com.ecommerce.cnj70.service.AdminUserService;
import com.ecommerce.cnj70.service.VoucherService;
import com.ecommerce.cnj70.service.ViolationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 — Full Role Matrix Regression Test.
 *
 * <p>Spec §7.5 yêu cầu 12 module × 4 role = 48 ô test. Mỗi ô verify:
 * <ul>
 *   <li>ALLOW: GET → 200 (or 302 if rendering fails downstream — we accept either non-error)</li>
 *   <li>DENY: GET → 302 redirect (Spring Security access-denied handler to /auth/login?denied=1)
 *       hoặc 401 (anonymous) — không phải 200</li>
 * </ul>
 *
 * <p>Modules (12):
 * <ol>
 *   <li>users, 2) categories, 3) vouchers, 4) shops, 5) kyc, 6) violations,
 *   7) orders, 8) products, 9) reviews, 10) escalations, 11) audit, 12) banners</li>
 * </ol>
 *
 * <p>Roles (4): ADMIN, MODERATOR, CUSTOMER, VENDOR.</p>
 *
 * <p>Note: This test uses {@code @WithMockUser} which by design supplies
 * the role prefix correctly via Spring Security Test framework. We do
 * not rely on real authentication wiring — that is verified separately
 * via Phase 2 report.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Phase 7 Role Matrix Regression (48 cells)")
class AdminRoleMatrixRegressionTest {

    @Autowired private MockMvc mockMvc;

    // ===== Mock services so 200-path không blow up due to missing DB =====
    @MockBean private AdminUserService adminUserService;
    @MockBean private AdminCategoryService adminCategoryService;
    @MockBean private VoucherService voucherService;
    @MockBean private AdminShopService adminShopService;
    @MockBean private AdminKycService adminKycService;
    @MockBean private ViolationService violationService;
    @MockBean private AdminOrderService adminOrderService;
    @MockBean private AdminProductService adminProductService;
    @MockBean private AdminReviewService adminReviewService;
    @MockBean private AdminEscalationService adminEscalationService;
    @MockBean private AdminAuditLogService adminAuditLogService;
    @MockBean private AdminBannerService adminBannerService;

    private void stubAll() {
        // Stub enough to make ALLOW cells return 200 instead of NPE downstream.
        when(adminUserService.listUsers(any(), any(), any(), any())).thenReturn(Page.empty());
        when(adminCategoryService.listCategories(any(), any(), any())).thenReturn(Page.empty());
        when(voucherService.getWebVouchers(any(), any(), any())).thenReturn(Page.empty());
        when(adminShopService.listShops(any(), any(), any())).thenReturn(Page.empty());
        when(adminKycService.listByStatus(any(), any(), any())).thenReturn(Page.empty());
        when(violationService.listViolations(any(), any(), any(), any())).thenReturn(Page.empty());
        when(adminOrderService.listOrders(any(), any(), any())).thenReturn(Page.empty());
        when(adminProductService.listProducts(any(), any(), any())).thenReturn(Page.empty());
        when(adminReviewService.listReviews(any(), any(), any())).thenReturn(Page.empty());
        when(adminEscalationService.listEscalations(any(), any(), any(), any()))
                .thenReturn(Page.empty());
        when(adminAuditLogService.listAll(any())).thenReturn(Page.empty());
        when(adminBannerService.listBanners(any(), any(), any())).thenReturn(Page.empty());
    }

    // ============================================================
    // /admin/users — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void users_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void users_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/users")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void users_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void users_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/categories — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void categories_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/categories")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void categories_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/categories")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void categories_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/categories")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void categories_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/categories")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/vouchers — ADMIN only
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void vouchers_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/vouchers")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void vouchers_moderatorDeny() throws Exception {
        mockMvc.perform(get("/admin/vouchers")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void vouchers_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/vouchers")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void vouchers_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/vouchers")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/shops — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void shops_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/shops")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void shops_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/shops")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void shops_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/shops")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void shops_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/shops")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/kyc — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void kyc_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/kyc")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void kyc_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/kyc")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void kyc_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/kyc")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void kyc_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/kyc")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/violations — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void violations_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/violations")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void violations_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/violations")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void violations_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/violations")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void violations_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/violations")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/orders — ADMIN only
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void orders_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/orders")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void orders_moderatorDeny() throws Exception {
        mockMvc.perform(get("/admin/orders")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void orders_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/orders")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void orders_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/orders")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/products — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void products_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/products")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void products_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/products")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void products_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/products")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void products_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/products")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/reviews — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void reviews_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/reviews")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void reviews_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/reviews")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void reviews_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/reviews")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void reviews_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/reviews")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/escalations — ADMIN + MODERATOR
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void escalations_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/escalations")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void escalations_moderatorAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/escalations")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void escalations_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/escalations")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void escalations_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/escalations")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/audit — ADMIN only
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void audit_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/audit")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void audit_moderatorDeny() throws Exception {
        mockMvc.perform(get("/admin/audit")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void audit_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/audit")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void audit_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/audit")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // /admin/banners — ADMIN only
    // ============================================================
    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void banners_adminAllow() throws Exception {
        stubAll();
        mockMvc.perform(get("/admin/banners")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void banners_moderatorDeny() throws Exception {
        mockMvc.perform(get("/admin/banners")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void banners_customerDeny() throws Exception {
        mockMvc.perform(get("/admin/banners")).andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void banners_vendorDeny() throws Exception {
        mockMvc.perform(get("/admin/banners")).andExpect(status().is3xxRedirection());
    }

    // ============================================================
    // Anonymous (no auth) — DENY all admin paths
    // ============================================================
    @Test
    void anonymous_deniesAdminList() throws Exception {
        // No @WithMockUser — anonymous.
        // Spring Security returns 401 (entry point) for unauthenticated.
        // The exact code depends on whether entry point triggers.
        // For admin paths, anonymous → 401.
        int status = mockMvc.perform(get("/admin/users")).andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(status)
                .as("Anonymous must not receive 200 on /admin/users")
                .isNotEqualTo(200);
    }
}
