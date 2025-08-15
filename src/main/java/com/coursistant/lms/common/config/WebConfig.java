package com.coursistant.lms.common.config;

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
                .excludePathPatterns("/")  // 允许访问首页 / Allow access to the homepage
                .excludePathPatterns("/login")  // 允许登录 / Allow login
                .excludePathPatterns("/register")  // 允许注册 / Allow registration
                .excludePathPatterns("/files/**")  // 允许访问静态资源 / Allow access to static resources
                .excludePathPatterns("/swagger-ui/**")  // 允许 Swagger UI / Allow Swagger UI
                .excludePathPatterns("/swagger-ui.html")  // 允许 Swagger UI / Allow Swagger UI
                .excludePathPatterns("/v3/api-docs/**")  // 允许 API 文档 / Allow API documentation
                .excludePathPatterns("/login/oauth2/**") // Allow third party verification path
                .excludePathPatterns("/thirdParty/**") //Allow third party login path
                .excludePathPatterns("/sendRegisterEmailVerification") // 允许发送邮件验证 / Allow sending email verification
                .excludePathPatterns("/sendResetEmailVerification") // 允许发送邮件验证 / Allow sending email verification
                .excludePathPatterns("/refresh-token")
                .excludePathPatterns("/validateRegisterEmailVerification")
                .excludePathPatterns("/resetPasswordValidation") // 允许发送邮件验证 / Allow sending email verification
                .excludePathPatterns("/sales")
                .excludePathPatterns("/login/oauth2/**")        // ✅ OAuth 回调（Google 重定向回来）
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