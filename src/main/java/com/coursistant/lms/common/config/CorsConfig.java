package com.coursistant.lms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 跨域配置
 * Cross-Origin Resource Sharing (CORS) configuration
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*"); // 1 设置访问源地址 / Set allowed origin addresses
        corsConfiguration.addAllowedHeader("*"); // 2 设置访问源请求头 / Set allowed request headers
        corsConfiguration.addAllowedMethod("*"); // 3 设置访问源请求方法 / Set allowed request methods
        source.registerCorsConfiguration("/**", corsConfiguration); // 4 对接口配置跨域设置 / Apply CORS settings to all endpoints
        return new CorsFilter(source);
    }
}
