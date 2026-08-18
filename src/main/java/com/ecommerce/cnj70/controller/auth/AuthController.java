package com.ecommerce.cnj70.controller.auth;

import com.ecommerce.cnj70.dto.request.RegisterReq;
import com.ecommerce.cnj70.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    @GetMapping("/auth/login")
    public String loginPage() {
        return "auth/login";
    }
    
    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("registerReq", new RegisterReq());
        return "auth/register";
    }
    
    @PostMapping("/auth/register")
    public String register(@ModelAttribute @Valid RegisterReq request, 
                          BindingResult result, 
                          Model model) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        
        try {
            if (authService.existsByEmail(request.getEmail())) {
                model.addAttribute("error", "Email already exists");
                return "auth/register";
            }
            
            authService.register(request);
            return "redirect:/auth/login?registered=true";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}
