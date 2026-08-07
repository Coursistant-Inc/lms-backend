package com.coursistant.lms.shared.security;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AccessTokenAuthService accessTokenAuthService;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public JwtAuthenticationFilter(AccessTokenAuthService accessTokenAuthService,
                                   SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.accessTokenAuthService = accessTokenAuthService;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (AuthPublicPaths.isPublic(request.getMethod(), request.getRequestURI(), request.getContextPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AccessTokenAuthService.AuthenticatedPrincipal principal =
                    accessTokenAuthService.authenticateBearer(request.getHeader("Authorization"), request);
            var auth = new UsernamePasswordAuthenticationToken(
                    principal.userId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
        } catch (ApiException e) {
            SecurityContextHolder.clearContext();
            securityErrorResponseWriter.write(response, e);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            securityErrorResponseWriter.write(response, ErrorType.INVALID_TOKEN, "Invalid Access Token");
        }
    }
}
