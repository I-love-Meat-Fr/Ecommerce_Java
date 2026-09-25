package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Category;
import com.ecommerce.cnj70.document.KycProfile;
import com.ecommerce.cnj70.document.Product;
import com.ecommerce.cnj70.document.Shop;
import com.ecommerce.cnj70.enums.AccountStatus;
import com.ecommerce.cnj70.enums.UserRole;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.AdminCategoryService;
import com.ecommerce.cnj70.service.AdminKycService;
import com.ecommerce.cnj70.service.AdminProductService;
import com.ecommerce.cnj70.service.AdminShopService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web layer test cho 4 module Admin — Phase 4.
 *
 * Verify routing & role-based access:
 *   - ADMIN allowed (200) trên Shop/KYC/Category/Product list + detail.
 *   - MODERATOR allowed (200).
 *   - ANONYMOUS denied (401 hoặc 302).
 *   - CUSTOMER/VENDOR denied (302 redirect → /auth/login?denied=1).
 *
 * Không gọi các POST write (Approve/Reject/Create/Edit/Delete)
 * theo yêu cầu Phase 4: KHÔNG thay đổi DB thật.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FourModuleSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminShopService adminShopService;
    @MockBean private AdminKycService adminKycService;
    @MockBean private AdminCategoryService adminCategoryService;
    @MockBean private AdminProductService adminProductService;

    private static CustomUserDetails asUser(String id, String role) {
        com.ecommerce.cnj70.document.User u = new com.ecommerce.cnj70.document.User();
        u.setId(id);
        u.setEmail(id + "@test.com");
        u.setRole(UserRole.valueOf(role));
        u.setStatus(AccountStatus.ACTIVE);
        return CustomUserDetails.fromUser(u);
    }

    private void stubList() {
        when(adminShopService.listShops(any(), any(), any()))
                .thenReturn(new PageImpl<Shop>(List.of(), PageRequest.of(0, 5), 0));
        when(adminShopService.listShops(any(), any()))
                .thenReturn(new PageImpl<Shop>(List.of(), PageRequest.of(0, 5), 0));
        when(adminKycService.listByStatus(any(), any(), any()))
                .thenReturn(new PageImpl<KycProfile>(List.of(), PageRequest.of(0, 10), 0));
        when(adminKycService.getStats()).thenReturn(new HashMap<>());
        when(adminCategoryService.listCategories(any(), any(), any()))
                .thenReturn(new PageImpl<Category>(List.of(), PageRequest.of(0, 5), 0));
        when(adminProductService.listProducts(any(), any(), any()))
                .thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(0, 10), 0));
    }

    private void stubDetail() {
        Shop shop = Shop.builder().id("shop1").shopName("Test Shop").build();
        when(adminShopService.getShopById(eq("shop1"))).thenReturn(shop);

        com.ecommerce.cnj70.document.User owner = new com.ecommerce.cnj70.document.User();
        owner.setId("u-owner");
        owner.setEmail("owner@test.com");
        Map<String, Object> detail = new HashMap<>();
        detail.put("profile", KycProfile.builder().id("kyc1").userId("u-owner").build());
        detail.put("user", owner);
        when(adminKycService.getDetail(eq("kyc1"))).thenReturn(detail);
        when(adminKycService.getUserForProfile(any())).thenReturn(owner);

        Category cat = Category.builder().id("cat1").name("Test Cat").build();
        when(adminCategoryService.getCategoryById(eq("cat1"))).thenReturn(cat);

        Product prod = Product.builder().id("prod1").name("Test Prod").build();
        when(adminProductService.getProductById(eq("prod1"))).thenReturn(prod);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanAccessAllFourModulePages() throws Exception {
        stubList();
        stubDetail();
        mockMvc.perform(get("/admin/shops")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/shops/shop1")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/kyc")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/kyc/kyc1")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories/cat1/edit")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/products")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/products/prod1")).andExpect(status().isOk());
    }

    @Test
    void moderatorCanAccessAllFourModulePages() throws Exception {
        stubList();
        stubDetail();
        CustomUserDetails mod = asUser("mod1", "MODERATOR");
        mockMvc.perform(get("/admin/shops").with(user(mod))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/shops/shop1").with(user(mod))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/kyc").with(user(mod))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/kyc/kyc1").with(user(mod))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories").with(user(mod))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories/cat1/edit").with(user(mod))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/products").with(user(mod))).andExpect(status().isOk());
        mockMvc.perform(get("/admin/products/prod1").with(user(mod))).andExpect(status().isOk());
    }

    @Test
    void anonymousDeniedOnAllFourModulePages() throws Exception {
        // Anonymous → JwtAuthenticationEntryPoint → 401
        mockMvc.perform(get("/admin/shops")).andExpect(status().is(401));
        mockMvc.perform(get("/admin/kyc")).andExpect(status().is(401));
        mockMvc.perform(get("/admin/categories")).andExpect(status().is(401));
        mockMvc.perform(get("/admin/products")).andExpect(status().is(401));
    }

    @Test
    void customerDeniedOnAllFourModulePages() throws Exception {
        // Customer authenticated → AccessDeniedHandler → 302 redirect to /auth/login?denied=1
        CustomUserDetails cust = asUser("cust1", "CUSTOMER");
        mockMvc.perform(get("/admin/shops").with(user(cust)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/kyc").with(user(cust)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/categories").with(user(cust)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/products").with(user(cust)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void vendorDeniedOnAllFourModulePages() throws Exception {
        CustomUserDetails vend = asUser("vend1", "VENDOR");
        mockMvc.perform(get("/admin/shops").with(user(vend)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/kyc").with(user(vend)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/categories").with(user(vend)))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/admin/products").with(user(vend)))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanFilterShopByStatus() throws Exception {
        stubList();
        // Verify all ShopStatus values are accepted by the filter
        for (String status : new String[]{"PENDING", "APPROVED", "REJECTED", "SUSPENDED", "RESTRICTED"}) {
            mockMvc.perform(get("/admin/shops?status=" + status))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanFilterProductByStatus() throws Exception {
        stubList();
        // Verify all ProductStatus values are accepted by the filter
        for (String status : new String[]{"DRAFT", "ACTIVE", "OUT_OF_STOCK", "HIDDEN"}) {
            mockMvc.perform(get("/admin/products?status=" + status))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanPaginateAllFourModulePages() throws Exception {
        stubList();
        // Test paging on each module
        mockMvc.perform(get("/admin/shops?page=0&size=5")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/shops?page=1&size=5")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/kyc?page=0&size=10")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories?page=0&size=5")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/products?page=0&size=10")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCanSearchShopsAndProducts() throws Exception {
        stubList();
        mockMvc.perform(get("/admin/shops?q=Test")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/categories?q=Test")).andExpect(status().isOk());
        mockMvc.perform(get("/admin/products?q=Test")).andExpect(status().isOk());
    }
}
