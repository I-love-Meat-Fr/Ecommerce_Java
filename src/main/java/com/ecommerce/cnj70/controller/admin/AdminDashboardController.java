package com.ecommerce.cnj70.controller.admin;

import com.ecommerce.cnj70.dto.response.AdminDashboardRes;
import com.ecommerce.cnj70.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminService adminService;

    @GetMapping("/admin/dashboard")
    public String dashboard(@RequestParam(value = "period", required = false) String period,
                            Model model) {
        AdminDashboardRes stats = adminService.getDashboardStatsByPeriod(period);
        model.addAttribute("stats", stats);
        model.addAttribute("period", stats != null && period != null ? period.toUpperCase() : "WEEK");
        model.addAttribute("isAdmin", true);
        return "admin/dashboard";
    }
}
