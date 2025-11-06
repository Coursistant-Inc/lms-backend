package com.coursistant.lms.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        
        // 明确指定允许的域名
        corsConfiguration.setAllowedOriginPatterns(Arrays.asList(
            "https://usc.xlearnedu.com:*",
            "https://chat.xlearnedu.com:*",
            "https://ec2.dev.xlearnedu.com:*",
            "https://dev.chat.xlearnedu.com:*",
            "http://localhost:3000"  // 开发环境
        ));
        
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setAllowCredentials(true);
        
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }
}
