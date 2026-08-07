package com.coursistant.lms.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * CORS for browser frontends (including refresh Cookie credentials).
 * TEMP (FE integration): use origin patterns for any localhost / 127.0.0.1 port.
 * Tighten to explicit {@code auth.cors.allowed-origins} before production.
 */
@Configuration
public class CorsConfig {

    /**
     * Comma-separated origin patterns. Patterns (e.g. {@code http://localhost:*}) work with credentials;
     * a bare {@code *} does not.
     */
    @Value("${auth.cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}")
    private String allowedOriginPatterns;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        List<String> patterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        if (patterns.isEmpty() || patterns.stream().anyMatch(p -> "*".equals(p))) {
            throw new IllegalStateException(
                    "auth.cors.allowed-origin-patterns must list patterns; bare '*' is not allowed with credentials");
        }
        corsConfiguration.setAllowedOriginPatterns(patterns);
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setAllowedHeaders(List.of(
                "Authorization",
                "Idempotency-Key",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"));
        corsConfiguration.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }
}
