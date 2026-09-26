package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.security.JwtAuthenticationEntryPoint;
import com.ecommerce.cnj70.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final UserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:8081", "http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/auth/**", "/css/**", "/js/**", "/images/**", "/uploads/**", "/error").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/products/**").permitAll()
                .requestMatchers("/api/categories/**").permitAll()
                .requestMatchers("/vouchers").permitAll()  // Trang công khai xem voucher
                // Phase 4 §43/§44 — /checkout/apply-voucher phải authenticated.
                // Endpoint này thực hiện price/discount evaluation dựa trên voucher
                // contract, chỉ meaningful khi user đã login và đang trong checkout flow.
                // permitAll() trước đây cho phép anonymous user dò voucher — fix tại Phase 4.
                .requestMatchers("/api/kyc/callback").permitAll()  // KYC provider webhook (Phase 4 §8)
                // ===== PHASE 2 — SECURITY FIX & ROUTE MAPPING =====
                // §1.1 Role × Module Matrix.
                // Thứ tự rule quan trọng: rule cụ thể (/admin/users/**) phải ĐỨNG TRƯỚC
                // rule tổng quát (/admin/**). Spring Security match first match wins.
                // ADMIN + MODERATOR (theo matrix §1.1)
                .requestMatchers("/admin/users/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/admin/categories/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/admin/shops/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/admin/kyc/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/admin/violations/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/admin/escalations/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/admin/products/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers("/admin/reviews/**").hasAnyRole("ADMIN", "MODERATOR")
                // ADMIN-only (theo matrix §1.1)
                .requestMatchers("/admin/orders/**").hasRole("ADMIN")
                .requestMatchers("/admin/vouchers/**").hasRole("ADMIN")
                .requestMatchers("/admin/audit/**").hasRole("ADMIN")
                .requestMatchers("/admin/banners/**").hasRole("ADMIN")
                // Fallback cho admin path khác (dashboard, ...) — chỉ ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/moderator/**").hasRole("MODERATOR")
                .requestMatchers("/vendor/**").hasRole("VENDOR")
                .requestMatchers("/complaints/**").authenticated()
                .requestMatchers("/reports/**").authenticated()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                // Khi user đã authenticated nhưng thiếu role, redirect về login
                // thay vì để Spring forward về /error (gây 404 thay vì 403).
                .accessDeniedHandler((req, res, ex) -> {
                    log.warn("Access denied for {} {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
                    res.sendRedirect("/auth/login?denied=1");
                })
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
