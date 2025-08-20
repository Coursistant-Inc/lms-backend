package com.coursistant.lms.common.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, OAuth2AuthorizationRequestResolver customResolver) throws Exception {

        http
                // 0. 关闭 CSRF（如以后要启用，改为 .csrf(Customizer.withDefaults()) ）
                .csrf(csrf -> csrf.disable())

                // 1. 认证授权规则
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())


                // ✅ 新增：启用 OAuth2 Client（只作用于 google-drive 这类“代表用户访问 API”的流程）
                .oauth2Client(o -> o.authorizationCodeGrant(c ->
                        c.authorizationRequestResolver(customResolver) // 给 google-drive 授权加 offline/consent
                ))


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
