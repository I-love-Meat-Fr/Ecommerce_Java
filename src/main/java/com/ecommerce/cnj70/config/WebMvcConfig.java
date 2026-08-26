package com.ecommerce.cnj70.config;

import com.ecommerce.cnj70.interceptor.CartCountInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CartCountInterceptor cartCountInterceptor;

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/home");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                    "classpath:/static/uploads/",
                    "file:src/main/resources/static/uploads/"
                );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cartCountInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/**", "/admin/**", "/vendor/**", "/auth/**",
                        "/css/**", "/js/**", "/images/**", "/uploads/**", "/webjars/**");
    }
}