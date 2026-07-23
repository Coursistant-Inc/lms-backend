package com.coursistant.lms.shared.config;

import com.coursistant.lms.shared.security.JwtInterceptor;

import org.springframework.context.annotation.Configuration;
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
                .excludePathPatterns("/login/oauth2/**")
                .excludePathPatterns("/thirdParty/**")
                .excludePathPatterns("/sales")
                .excludePathPatterns("/oauth2/authorization/**")
        ;
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
