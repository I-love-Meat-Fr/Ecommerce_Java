package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {
    
    private final AdminService adminService;
    
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        AdminDashboardRes stats = adminService.getDashboardStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }
}
