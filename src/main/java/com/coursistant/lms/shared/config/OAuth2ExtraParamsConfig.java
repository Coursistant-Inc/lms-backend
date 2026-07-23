package com.coursistant.lms.shared.config;



import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Configuration
// @RequiredArgsConstructor
public class OAuth2ExtraParamsConfig {

    private final ClientRegistrationRepository repo; // 构造注入

    public OAuth2ExtraParamsConfig(ClientRegistrationRepository repo)
    {
        this.repo = repo;
    }
    

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver() {
        DefaultOAuth2AuthorizationRequestResolver defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");

        return new OAuth2AuthorizationRequestResolver() {
            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
                return enhanceIfGoogleDrive(defaultResolver.resolve(request));
            }

            @Override
            public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
                return enhanceIfGoogleDrive(defaultResolver.resolve(request, registrationId));
            }

            private OAuth2AuthorizationRequest enhanceIfGoogleDrive(OAuth2AuthorizationRequest req) {
                if (req == null) return null;
                Object rid = req.getAttributes().get(OAuth2ParameterNames.REGISTRATION_ID);
                if ("google-drive".equals(rid)) {
                    Map<String, Object> extra = new HashMap<>(req.getAdditionalParameters());
                    extra.put("access_type", "offline");
                    extra.put("prompt", "consent");
                    return OAuth2AuthorizationRequest.from(req)
                            .additionalParameters(extra)
                            .build();
                }
                return req;
            }
        };
    }
}
