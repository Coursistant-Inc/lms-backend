package com.coursistant.lms.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.filter.CharacterEncodingFilter;

import javax.annotation.Resource;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor).addPathPatterns("/api/**")
                .excludePathPatterns("/api/")  // 允许访问首页 / Allow access to the homepage
                .excludePathPatterns("/api/login")  // 允许登录 / Allow login
                .excludePathPatterns("/api/register")  // 允许注册 / Allow registration
                .excludePathPatterns("/api/files/**")  // 允许访问静态资源 / Allow access to static resources
                .excludePathPatterns("/api/swagger-ui/**")  // 允许 Swagger UI / Allow Swagger UI
                .excludePathPatterns("/api/swagger-resources/**")  // 允许 Swagger 资源 / Allow Swagger resources
                .excludePathPatterns("/api/v3/api-docs/**")  // 允许 API 文档 / Allow API documentation
                .excludePathPatterns("/api/webjars/**")  // 允许 Swagger 相关静态资源 / Allow Swagger-related static resources
                .excludePathPatterns("/api/doc.html")   // 允许 Swagger UI (某些 UI 组件可能用这个) / Allow Swagger UI (some UI components may use this)
                .excludePathPatterns("/api/login/oauth2/**") // Allow third party verification path
                .excludePathPatterns("/api/thirdParty/**") //Allow third party login path
                .excludePathPatterns("/api/sendEmailVerification") // 允许发送邮件验证 / Allow sending email verification
                .excludePathPatterns("/api/sendResetEmailVerification") // 允许发送邮件验证 / Allow sending email verification
                .excludePathPatterns("/api/refresh-token")
                .excludePathPatterns("/api/resetPasswordValidation"); // 允许发送邮件验证 / Allow sending email verification
    
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