package com.ecommerce.cnj70.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {
    
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model) {
        return "admin/dashboard";
    }
}
