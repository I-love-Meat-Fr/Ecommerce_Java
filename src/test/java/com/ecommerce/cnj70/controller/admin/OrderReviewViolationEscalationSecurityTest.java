package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.document.Escalation;
import com.ecommerce.cnj70.document.Order;
import com.ecommerce.cnj70.document.Review;
import com.ecommerce.cnj70.document.Violation;
import com.ecommerce.cnj70.dto.response.AdminOrderRes;
import com.ecommerce.cnj70.enums.ReportCaseResourceType;
import com.ecommerce.cnj70.repository.ReportCaseRepository;
import com.ecommerce.cnj70.repository.ReviewRepository;
import com.ecommerce.cnj70.service.AdminEscalationService;
import com.ecommerce.cnj70.service.AdminOrderService;
import com.ecommerce.cnj70.service.AdminReviewService;
import com.ecommerce.cnj70.service.ViolationService;
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
 * Phase 6 — Web layer security test cho 4 Admin modules.
 *
 * Spec §1.1 / §6.15 Role × Module Matrix:
 *   - /admin/orders       ADMIN allow; MODERATOR/CUSTOMER/VENDOR/ANONYMOUS deny
 *   - /admin/reviews      ADMIN + MODERATOR allow; CUSTOMER/VENDOR/ANONYMOUS deny
 *   - /admin/violations   ADMIN + MODERATOR allow; CUSTOMER/VENDOR/ANONYMOUS deny
 *   - /admin/escalations  ADMIN + MODERATOR allow; CUSTOMER/VENDOR/ANONYMOUS deny
 *
 * Không ghi DB (chỉ GET routes, không POST write actions).
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderReviewViolationEscalationSecurityTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private AdminOrderService adminOrderService;
    @MockBean private AdminReviewService adminReviewService;
    @MockBean private ViolationService violationService;
    @MockBean private AdminEscalationService adminEscalationService;
    @MockBean private ReviewRepository reviewRepository;
    @MockBean private ReportCaseRepository reportCaseRepository;

    private static org.springframework.security.core.userdetails.UserDetails asUser(String role) {
        return org.springframework.security.core.userdetails.User.withUsername(role.toLowerCase())
                .password("x").roles(role).build();
    }

    private void stubOrderList() {
        Page<Order> empty = new PageImpl<>(List.of(), PageRequest.of(0, 5), 0);
        when(adminOrderService.listOrders(any(), any(), any())).thenReturn(empty);
    }

    private void stubOrderDetail() {
        Order o = Order.builder().id("o-1").userName("Alice").totalAmount(java.math.BigDecimal.TEN).build();
        when(adminOrderService.getOrderById(eq("o-1"))).thenReturn(o);
        when(adminOrderService.toAdminOrderRes(any(Order.class)))
                .thenReturn(AdminOrderRes.fromOrder(o, java.util.Map.of()));
    }

    private void stubReviewList() {
        Page<Review> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(adminReviewService.listReviews(any(), any(), any())).thenReturn(empty);
    }

    private void stubReviewDetail() {
        Review r = Review.builder().id("r-1").comment("test").rating(5).build();
        when(adminReviewService.getReviewById(eq("r-1"))).thenReturn(r);
    }

    private void stubViolationList() {
        Page<Violation> empty = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(violationService.listViolations(any(), any(), any(), any())).thenReturn(empty);
    }

    private void stubEscalationList() {
        Page<Escalation> empty = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(adminEscalationService.listEscalations(any(), any(), any(), any())).thenReturn(empty);
    }

    private void stubEscalationDetail() {
        Escalation e = Escalation.builder()
                .id("e-1")
                .resourceType(ReportCaseResourceType.PRODUCT)
                .status(Escalation.Status.PENDING)
                .build();
        when(adminEscalationService.getDetail(eq("e-1"))).thenReturn(e);
    }

    // ===== /admin/orders — ADMIN only =====

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnOrderList() throws Exception {
        stubOrderList();
        mockMvc.perform(get("/admin/orders")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnOrderDetail() throws Exception {
        stubOrderDetail();
        mockMvc.perform(get("/admin/orders/o-1")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void moderatorDeniedOnOrderList() throws Exception {
        // Spring Security redirects denied role to /access-denied (302),
        // not 403. This is correct behavior — access is blocked.
        mockMvc.perform(get("/admin/orders").with(user(asUser("MODERATOR"))))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void customerDeniedOnOrderList() throws Exception {
        mockMvc.perform(get("/admin/orders").with(user(asUser("CUSTOMER"))))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "vendor", roles = "VENDOR")
    void vendorDeniedOnOrderList() throws Exception {
        mockMvc.perform(get("/admin/orders").with(user(asUser("VENDOR"))))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void anonymousDeniedOnOrderList() throws Exception {
        // Anonymous → 401 (entry point) or 302 (login redirect) depending on stack.
        // Either way, not 200. We accept any non-success.
        int status = mockMvc.perform(get("/admin/orders")).andReturn().getResponse().getStatus();
        org.assertj.core.api.Assertions.assertThat(status)
                .as("Anonymous must not receive 200 on /admin/orders")
                .isNotEqualTo(200);
    }

    // ===== /admin/reviews — ADMIN + MODERATOR =====

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnReviewList() throws Exception {
        stubReviewList();
        mockMvc.perform(get("/admin/reviews")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void moderatorAllowedOnReviewList() throws Exception {
        stubReviewList();
        mockMvc.perform(get("/admin/reviews").with(user(asUser("MODERATOR"))))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void customerDeniedOnReviewList() throws Exception {
        mockMvc.perform(get("/admin/reviews").with(user(asUser("CUSTOMER"))))
                .andExpect(status().is3xxRedirection());
    }

    // ===== /admin/violations — ADMIN + MODERATOR =====

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnViolationList() throws Exception {
        stubViolationList();
        mockMvc.perform(get("/admin/violations")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void moderatorAllowedOnViolationList() throws Exception {
        stubViolationList();
        mockMvc.perform(get("/admin/violations").with(user(asUser("MODERATOR"))))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void customerDeniedOnViolationList() throws Exception {
        mockMvc.perform(get("/admin/violations").with(user(asUser("CUSTOMER"))))
                .andExpect(status().is3xxRedirection());
    }

    // ===== /admin/escalations — ADMIN + MODERATOR =====

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnEscalationList() throws Exception {
        stubEscalationList();
        mockMvc.perform(get("/admin/escalations")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "admin", roles = "ADMIN")
    void adminAllowedOnEscalationDetail() throws Exception {
        stubEscalationDetail();
        mockMvc.perform(get("/admin/escalations/e-1")).andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "mod", roles = "MODERATOR")
    void moderatorAllowedOnEscalationList() throws Exception {
        stubEscalationList();
        mockMvc.perform(get("/admin/escalations").with(user(asUser("MODERATOR"))))
                .andExpect(status().isOk());
    }

    @Test
    @org.springframework.security.test.context.support.WithMockUser(username = "cust", roles = "CUSTOMER")
    void customerDeniedOnEscalationList() throws Exception {
        mockMvc.perform(get("/admin/escalations").with(user(asUser("CUSTOMER"))))
                .andExpect(status().is3xxRedirection());
    }
}
