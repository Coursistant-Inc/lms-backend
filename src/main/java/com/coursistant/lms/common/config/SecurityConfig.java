package com.coursistant.lms.common.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 0. 关闭 CSRF（如以后要启用，改为 .csrf(Customizer.withDefaults()) ）
                .csrf(csrf -> csrf.disable())

                // 1. 认证授权规则
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())


                // 2. OAuth2 登录配置（如目前只调试，可保持全部 permitAll）
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(ep -> ep.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(ep -> ep.baseUri("/thirdParty"))
                )

                // 3. 其他可选：异常处理、无状态会话等
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, rsp, e) ->
                                rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                )
        ;

        return http.build();
    }
}
