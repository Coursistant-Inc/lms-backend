package com.coursistant.lms.common.config;

import com.coursistant.lms.mapper.user.UserMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;


@Configuration
public class SecurityConfig {
    @Resource
    private UserMapper userMapper;
    @Resource(name = "generalRedisTemplate")
    private RedisTemplate<String, Object> generalRedisTemplate;

    @Bean
    protected SecurityFilterChain SecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf().disable() // Disable spring CSRF protection at this time to allow POST requests
            .authorizeHttpRequests(registry -> {
                    registry.anyRequest().permitAll();  // Google authentication not required at this time
            })
            .oauth2Login(oauth2 -> 
                oauth2
                    //.loginPage("/oauth2/authorization/google") //Configure google login path
                    .redirectionEndpoint(redirection -> redirection
			        .baseUri("/thidParty"))
			    )
            
            .build(); 
    }

    // Custom success handler to capture email and id token after successful Google authentication
    // private void handleSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    //     String authorizationCode = request.getParameter("code");

    //     if (authorizationCode != null) {

    //         // Optionally, you could store this authorization code in a session or database
    //         // for later use to request access tokens and user info.
            
    //         response.getWriter().write("Authorization Code: " + authorizationCode);
    //     } else {
    //         response.getWriter().write("Authorization code not found.");
    //     }
    // }
}
