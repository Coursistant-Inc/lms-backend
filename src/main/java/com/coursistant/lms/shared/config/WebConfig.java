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
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/v1")
                .excludePathPatterns("/v1/auth/login")
                .excludePathPatterns("/v1/auth/register")
                .excludePathPatterns("/v1/auth/refresh-token")
                .excludePathPatterns("/v1/auth/email-verifications/**")
                .excludePathPatterns("/v1/auth/password-resets/**")
                .excludePathPatterns("/files/**")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/v3/api-docs/**")
                .excludePathPatterns("/v2/users/*/avatar")
        ;

        // Idempotency exclude is intentionally narrower than JWT: keep email-verifications
        // and password-resets so @Idempotent on those endpoints can run.
        registry.addInterceptor(idempotencyInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/v1")
                .excludePathPatterns("/v1/auth/login")
                .excludePathPatterns("/v1/auth/register")
                .excludePathPatterns("/v1/auth/refresh-token")
                .excludePathPatterns("/files/**")
                .excludePathPatterns("/swagger-ui/**")
                .excludePathPatterns("/swagger-ui.html")
                .excludePathPatterns("/v3/api-docs/**")
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
