package com.coursistant.lms.shared.config;

import com.coursistant.lms.shared.idempotency.IdempotencyFilter;
import com.coursistant.lms.shared.idempotency.IdempotencyInterceptor;
import com.coursistant.lms.shared.security.JwtInterceptor;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

import jakarta.annotation.Resource;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Resource
    private IdempotencyInterceptor idempotencyInterceptor;

    @Resource
    private IdempotencyFilter idempotencyFilter;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Keep interceptor as compatibility layer; Security filter is primary auth gate.
        // Exact public auth paths only — no /files/** or avatar bypass.
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/v1")
                .excludePathPatterns("/v1/auth/login")
                .excludePathPatterns("/v1/auth/register")
                .excludePathPatterns("/v1/auth/refresh-token")
                .excludePathPatterns("/v1/auth/logout")
                .excludePathPatterns("/v1/auth/email-verifications/register")
                .excludePathPatterns("/v1/auth/email-verifications/reset")
                .excludePathPatterns("/v1/auth/password-resets")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/v3/api-docs")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/v3/api-docs.yaml")
                .excludePathPatterns("/v3/api-docs.yaml/**")
        ;

        registry.addInterceptor(idempotencyInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/v1")
                .excludePathPatterns("/v1/auth/login")
                .excludePathPatterns("/v1/auth/register")
                .excludePathPatterns("/v1/auth/refresh-token")
                .excludePathPatterns("/v1/auth/logout")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/v3/api-docs")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/v3/api-docs.yaml")
                .excludePathPatterns("/v3/api-docs.yaml/**")
        ;
    }

    @Bean
    public FilterRegistrationBean<IdempotencyFilter> idempotencyFilterRegistration() {
        FilterRegistrationBean<IdempotencyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(idempotencyFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<CharacterEncodingFilter> characterEncodingFilter() {
        FilterRegistrationBean<CharacterEncodingFilter> filterRegistrationBean = new FilterRegistrationBean<>();
        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);
        filterRegistrationBean.setFilter(encodingFilter);
        return filterRegistrationBean;
    }
}
