package com.ecommerce.cnj70.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.error("Unauthorized error: {}", authException.getMessage());
        // Detect browser navigation: redirect to login page instead of returning raw JSON.
        // Without this, the user sees a JSON {"status":401,"error":"Unauthorized"} page,
        // which makes it look like the login is broken.
        String accept = request.getHeader("Accept");
        String xrw = request.getHeader("X-Requested-With");
        boolean isBrowser = accept != null && accept.contains("text/html")
                && (xrw == null || !xrw.equalsIgnoreCase("XMLHttpRequest"));
        if (isBrowser) {
            String target = request.getRequestURI();
            response.sendRedirect(request.getContextPath() + "/auth/login?expired=1&redirect=" + target);
            return;
        }
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
