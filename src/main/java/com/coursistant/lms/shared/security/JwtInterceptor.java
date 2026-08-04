package com.coursistant.lms.shared.security;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String PRINCIPAL_CACHE_ADMIN_PREFIX = "auth:principal:admin:";
    public static final String PRINCIPAL_CACHE_USER_PREFIX = "auth:principal:user:";

    @Resource
    private AccessTokenAuthService accessTokenAuthService;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.isAuthenticated()
                && request.getAttribute(AuthzService.ATTR_USER_ID) != null) {
            return true;
        }

        AccessTokenAuthService.AuthenticatedPrincipal principal =
                accessTokenAuthService.authenticateBearer(request.getHeader("Authorization"), request);
        log.debug("Authentication succeed, userId: {}, role: {}", principal.userId(), principal.role());
        return true;
    }
}
