package com.ecommerce.cnj70;

import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    private AdminDashboardRes emptyStats() {
        return AdminDashboardRes.builder()
                .totalUsers(0).totalShops(0).totalProducts(0).totalOrders(0)
                .totalPlatformRevenue(BigDecimal.ZERO)
                .recentActivities(List.of())
                .revenueTrend(List.of())
                .build();
    }

    private AdminDashboardRes populatedStats() {
        return AdminDashboardRes.builder()
                .totalUsers(10).totalShops(4).totalProducts(25).totalOrders(15)
                .totalPlatformRevenue(new BigDecimal("1500000"))
                .recentActivities(List.of())
                .revenueTrend(List.of())
                .build();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void adminCanAccessDashboard() throws Exception {
        when(adminService.getDashboardStats()).thenReturn(emptyStats());
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void customerCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "vendor", roles = {"VENDOR"})
    void vendorCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousCannotAccessDashboard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void dashboardLoadsWithEmptyData() throws Exception {
        when(adminService.getDashboardStats()).thenReturn(emptyStats());
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void dashboardLoadsWithPopulatedData() throws Exception {
        when(adminService.getDashboardStats()).thenReturn(populatedStats());
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
    }
}
