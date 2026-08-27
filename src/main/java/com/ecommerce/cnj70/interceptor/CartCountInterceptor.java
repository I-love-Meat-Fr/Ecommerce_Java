package com.ecommerce.cnj70.interceptor;

import com.ecommerce.cnj70.security.CustomUserDetails;
import com.ecommerce.cnj70.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.ui.ModelMap;

@Component
@RequiredArgsConstructor
public class CartCountInterceptor implements HandlerInterceptor {

    private final CartService cartService;

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, org.springframework.web.servlet.ModelAndView modelAndView) {
        if (modelAndView == null || !modelAndView.hasView()) return;
        String viewName = modelAndView.getViewName();
        if (viewName != null && !viewName.startsWith("web/") && !viewName.startsWith("fragments/")) {
            return;
        }
        ModelMap modelMap = modelAndView.getModelMap();
        if (modelMap.containsAttribute("cartCount")) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails user) {
            int count = cartService.countItems(user.getId());
            modelMap.addAttribute("cartCount", count);
        }
    }
}