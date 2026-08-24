package com.ecommerce.cnj70.controller.auth;

import com.ecommerce.cnj70.dto.request.LoginReq;
import com.ecommerce.cnj70.dto.request.RegisterReq;
import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.security.JwtUtils;
import com.ecommerce.cnj70.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    private final JwtUtils jwtUtils;
    
    @GetMapping("/auth/login")
    public String loginPage() {
        return "auth/login";
    }
    
    @PostMapping("/auth/login")
    public String login(@ModelAttribute LoginReq request, 
                        HttpServletResponse response,
                        Model model) {
        try {
            CustomUserDetails user = authService.login(request);
            
            // Generate JWT token
            String token = jwtUtils.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole()
            );
            
            // Set JWT in cookie (not httpOnly so filter can read it)
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(false);
            cookie.setSecure(false);
            cookie.setPath("/");
            cookie.setMaxAge((int) (jwtUtils.getJwtExpiration() / 1000));
            response.addCookie(cookie);
            
            return "redirect:/home";
        } catch (Exception e) {
            model.addAttribute("error", "Invalid email or password");
            model.addAttribute("email", request.getEmail());
            return "auth/login";
        }
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

    @PostMapping("/auth/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt".equals(cookie.getName())) {
                    cookie.setValue(null);
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                    break;
                }
            }
        }
        return "redirect:/auth/login?loggedOut=true";
    }
}
