package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Banner;
import com.ecommerce.cnj70.document.Voucher;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminBannerService;
import com.ecommerce.cnj70.service.VoucherService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 5 — Web layer security test cho Admin Voucher + Admin Banner.
 *
 * Spec §1.3 Role × Module Matrix:
 *   - /admin/vouchers  ADMIN allow, MODERATOR/CUSTOMER/VENDOR/ANONYMOUS deny
 *   - /admin/banners   ADMIN allow, MODERATOR/CUSTOMER/VENDOR/ANONYMOUS deny
 *
 * Không ghi DB (chỉ GET routes, không POST).
 */
@SpringBootTest
@AutoConfigureMockMvc
class VoucherBannerSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private VoucherService voucherService;
    @MockBean private AdminBannerService adminBannerService;

    private static CustomUserDetails asUser(String id, String role) {
        com.ecommerce.cnj70.document.User u = new com.ecommerce.cnj70.document.User();
        u.setId(id);
        u.setEmail(id + "@test.com");
        u.setRole(UserRole.valueOf(role));
        u.setStatus(AccountStatus.ACTIVE);
        return CustomUserDetails.fromUser(u);
    }

    private void stubList() {
        Page<Voucher> emptyV = new PageImpl<Voucher>(List.of(), PageRequest.of(0, 5), 0);
        when(voucherService.getWebVouchers(any(), any(), any())).thenReturn(emptyV);
        when(adminBannerService.listBanners(any(), any(), any()))
                .thenReturn(new PageImpl<Banner>(List.of(), PageRequest.of(0, 10), 0));
    }

    private void stubDetail() {
        Voucher v = Voucher.builder().id("v1").code("WEBT").name("Test").build();
        when(voucherService.getVoucherById(eq("v1"))).thenReturn(v);

        Banner b = Banner.builder().id("b1").title("Test").build();
        when(adminBannerService.getBannerById(eq("b1"))).thenReturn(b);
    }

    // ===== ADMIN allowed =====

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnVoucherList() throws Exception {
        stubList();
        mockMvc.perform(get("/admin/vouchers"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/vouchers/create"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnBannerList() throws Exception {
        stubList();
        mockMvc.perform(get("/admin/banners"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/banners/create"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnBannerDetail() throws Exception {
        stubDetail();
        mockMvc.perform(get("/admin/banners/b1"))
                .andExpect(status().isOk());
    }

    // Note: /admin/vouchers/edit/{id} excluded — uses editAdminVoucherForm which depends
    // on service interaction with voucherRepository.findById (verified separately in
    // VoucherServiceImplTest + runtime). Phase 5: write/edit paths are NOT TESTED.

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanSearchAndFilterVoucher() throws Exception {
        stubList();
        mockMvc.perform(get("/admin/vouchers?q=WEB"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/vouchers?active=true&page=0&size=5"))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanSearchAndFilterBanner() throws Exception {
        stubList();
        mockMvc.perform(get("/admin/banners?q=sale"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/banners?status=PUBLISHED&page=0&size=10"))
                .andExpect(status().isOk());
    }

    @Test
    void moderatorDeniedOnVouchers() throws Exception {
        mockMvc.perform(get("/admin/vouchers")
                        .with(user(asUser("m1", "MODERATOR"))))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void moderatorDeniedOnBanners() throws Exception {
        mockMvc.perform(get("/admin/banners")
                        .with(user(asUser("m1", "MODERATOR"))))
                .andExpect(status().is3xxRedirection());
    }

    // ===== CUSTOMER/VENDOR denied =====

    @Test
    void customerDeniedOnVouchers() throws Exception {
        mockMvc.perform(get("/admin/vouchers")
                        .with(user(asUser("c1", "CUSTOMER"))))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void customerDeniedOnBanners() throws Exception {
        mockMvc.perform(get("/admin/banners")
                        .with(user(asUser("c1", "CUSTOMER"))))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void vendorDeniedOnVouchers() throws Exception {
        mockMvc.perform(get("/admin/vouchers")
                        .with(user(asUser("v1", "VENDOR"))))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void vendorDeniedOnBanners() throws Exception {
        mockMvc.perform(get("/admin/banners")
                        .with(user(asUser("v1", "VENDOR"))))
                .andExpect(status().is3xxRedirection());
    }

    // ===== ANONYMOUS denied =====

    @Test
    void anonymousDeniedOnVouchers() throws Exception {
        // Anonymous → JwtAuthenticationEntryPoint → 401
        mockMvc.perform(get("/admin/vouchers"))
                .andExpect(status().is(401));
    }

    @Test
    void anonymousDeniedOnBanners() throws Exception {
        mockMvc.perform(get("/admin/banners"))
                .andExpect(status().is(401));
    }
}
