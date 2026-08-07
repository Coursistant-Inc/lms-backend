package com.coursistant.lms.shared.idempotency;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyInterceptor.class);

    private static final String ATTR_REDIS_KEY = "idem.redisKey";
    private static final String ATTR_FINGERPRINT = "idem.fingerprint";
    private static final String ATTR_TTL = "idem.ttl";

    private static final int MAX_KEY_LENGTH = 128;
    private static final Pattern KEY_PATTERN = Pattern.compile("^[A-Za-z0-9_\\-]+$");
    private static final Duration PENDING_TTL = Duration.ofSeconds(60);
    private static final int MAX_RESPONSE_CACHE_BYTES = 256 * 1024;

    @Resource(name = "idempotencyStringRedisTemplate")
    private StringRedisTemplate idempotencyRedisTemplate;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod hm)) {
            return true;
        }
        Idempotent annotation = hm.getMethodAnnotation(Idempotent.class);
        if (annotation == null) {
            return true;
        }

        String headerKey = request.getHeader("Idempotency-Key");
        if (headerKey == null || headerKey.isBlank()) {
            throw new ApiException(ErrorType.IDEMPOTENCY_KEY_REQUIRED);
        }
        headerKey = headerKey.trim();
        if (headerKey.length() > MAX_KEY_LENGTH || !KEY_PATTERN.matcher(headerKey).matches()) {
            throw new ApiException(ErrorType.IDEMPOTENCY_KEY_INVALID);
        }

        // Multipart: SHA-256 fingerprint before Redis claim (never MinIO-first).
        String fingerprint = MultipartFingerprint.isMultipart(request)
                ? MultipartFingerprint.compute(request)
                : computeFingerprint(request);
        String userId = request.getAttribute("userId") != null
                ? request.getAttribute("userId").toString()
                : "anon";
        String redisKey = "idem:" + userId + ":" + request.getRequestURI() + ":" + headerKey;

        String pendingValue = IdempotencyRecord.pending(fingerprint);
        Boolean claimed;
        try {
            claimed = idempotencyRedisTemplate.opsForValue().setIfAbsent(redisKey, pendingValue, PENDING_TTL);
        } catch (Exception e) {
            log.warn("Idempotency Redis unavailable on claim: {}", e.getMessage());
            throw new ApiException(ErrorType.IDEMPOTENCY_STORE_UNAVAILABLE);
        }

        if (Boolean.TRUE.equals(claimed)) {
            request.setAttribute(ATTR_REDIS_KEY, redisKey);
            request.setAttribute(ATTR_FINGERPRINT, fingerprint);
            request.setAttribute(ATTR_TTL, annotation.ttlSeconds());
            return true;
        }

        String existing;
        try {
            existing = idempotencyRedisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.warn("Idempotency Redis unavailable on get: {}", e.getMessage());
            throw new ApiException(ErrorType.IDEMPOTENCY_STORE_UNAVAILABLE);
        }
        if (existing == null) {
            request.setAttribute(ATTR_REDIS_KEY, redisKey);
            request.setAttribute(ATTR_FINGERPRINT, fingerprint);
            request.setAttribute(ATTR_TTL, annotation.ttlSeconds());
            try {
                idempotencyRedisTemplate.opsForValue().set(redisKey, pendingValue, PENDING_TTL);
            } catch (Exception e) {
                log.warn("Idempotency Redis unavailable on set: {}", e.getMessage());
                throw new ApiException(ErrorType.IDEMPOTENCY_STORE_UNAVAILABLE);
            }
            return true;
        }

        IdempotencyRecord record = IdempotencyRecord.parse(existing);
        if (record == null) {
            return true;
        }

        if (record.getState() == IdempotencyRecord.State.PENDING) {
            throw new ApiException(ErrorType.IDEMPOTENCY_REQUEST_IN_PROGRESS);
        }

        if (!fingerprint.equals(record.getFingerprint())) {
            throw new ApiException(ErrorType.IDEMPOTENCY_KEY_MISMATCH);
        }

        response.setStatus(record.getHttpStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getOutputStream().write(record.getBody());
        response.getOutputStream().flush();
        log.debug("Idempotency replay for key={}", headerKey);
        return false;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler, Exception ex) {
        String redisKey = (String) request.getAttribute(ATTR_REDIS_KEY);
        if (redisKey == null) {
            return;
        }
        String fingerprint = (String) request.getAttribute(ATTR_FINGERPRINT);
        long ttlSeconds = (long) request.getAttribute(ATTR_TTL);

        int status = response.getStatus();
        try {
            if (status < 200 || status >= 300) {
                idempotencyRedisTemplate.delete(redisKey);
                return;
            }

            byte[] body = extractResponseBody(request, response);
            if (body == null || body.length > MAX_RESPONSE_CACHE_BYTES) {
                idempotencyRedisTemplate.delete(redisKey);
                return;
            }

            String doneValue = IdempotencyRecord.done(fingerprint, status, body);
            idempotencyRedisTemplate.opsForValue().set(redisKey, doneValue, Duration.ofSeconds(ttlSeconds));
        } catch (Exception e) {
            // DB may already have committed; do not convert to client error here.
            log.warn("Idempotency Redis unavailable after completion for key={}: {}", redisKey, e.getMessage());
        }
    }

    private byte[] extractResponseBody(HttpServletRequest request, HttpServletResponse response) {
        Object cached = request.getAttribute("idem.cachedResponse");
        if (cached instanceof ContentCachingResponseWrapper wrapper) {
            return wrapper.getContentAsByteArray();
        }
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            return wrapper.getContentAsByteArray();
        }
        return null;
    }

    private String computeFingerprint(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString() != null ? request.getQueryString() : "";
        byte[] body = new byte[0];
        Object cachedBody = request.getAttribute("idem.cachedBody");
        if (cachedBody instanceof byte[] b) {
            body = b;
        } else if (request instanceof CachedBodyHttpServletRequest cached) {
            body = cached.getCachedBody();
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((method + ":" + path + ":" + query + ":").getBytes(StandardCharsets.UTF_8));
            md.update(body);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
