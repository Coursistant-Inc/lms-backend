package com.coursistant.lms.shared.idempotency;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Set;

/**
 * Wraps request/response for idempotency processing when the
 * Idempotency-Key header is present on a mutating request.
 * <p>
 * JSON: cache request body + response.
 * Multipart: wrap response only (never buffer the file body; fingerprint is computed in Interceptor).
 */
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String idempotencyKey = request.getHeader("Idempotency-Key");
        boolean mutating = MUTATING_METHODS.contains(request.getMethod().toUpperCase());
        boolean hasKey = idempotencyKey != null && !idempotencyKey.isBlank();
        boolean multipart = MultipartFingerprint.isMultipart(request);

        if (!hasKey || !mutating) {
            chain.doFilter(request, response);
            return;
        }

        if (multipart) {
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
            request.setAttribute("idem.cachedResponse", wrappedResponse);
            try {
                chain.doFilter(request, wrappedResponse);
            } finally {
                wrappedResponse.copyBodyToResponse();
            }
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        wrappedRequest.setAttribute("idem.cachedBody", wrappedRequest.getCachedBody());
        wrappedRequest.setAttribute("idem.cachedResponse", wrappedResponse);

        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
        }
    }
}
