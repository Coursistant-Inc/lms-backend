package com.coursistant.lms.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Exact method+path public whitelist (context-path stripped).
 */
@Component
public class AuthPublicPaths {

    private static volatile boolean docsPublic = true;

    @Value("${auth.docs-public:true}")
    public void setDocsPublic(boolean value) {
        docsPublic = value;
    }

    private static final Set<String> PUBLIC_POST = Set.of(
            "/v1/auth/login",
            "/v1/auth/register",
            "/v1/auth/refresh-token",
            "/v1/auth/logout",
            "/v1/auth/email-verifications/register",
            "/v1/auth/email-verifications/reset",
            "/v1/auth/password-resets"
    );

    /** Numeric user-id avatar proxy. Query string is not part of requestURI. */
    private static final Pattern USER_AVATAR_GET = Pattern.compile("^/v2/users/\\d+/avatar$");
    static final String PUBLIC_USER_AVATAR_GET = "GET /v2/users/{userId}/avatar";

    /** Exact POST paths that are public (context-path stripped). Used by OpenAPI security sync. */
    public static Set<String> publicPostPaths() {
        return PUBLIC_POST;
    }

    /** Exact method+path pairs that must appear as security: [] in OpenAPI (excluding docs). */
    public static Set<String> publicMethodPaths() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        out.add("GET /v1");
        for (String path : PUBLIC_POST) {
            out.add("POST " + path);
        }
        out.add(PUBLIC_USER_AVATAR_GET);
        return Set.copyOf(out);
    }

    public static boolean isPublic(String method, String requestUri, String contextPath) {
        String path = stripContext(requestUri, contextPath);
        if (HttpMethod.GET.matches(method) && "/v1".equals(path)) {
            return true;
        }
        if (HttpMethod.GET.matches(method) && USER_AVATAR_GET.matcher(path).matches()) {
            return true;
        }
        if (HttpMethod.POST.matches(method) && PUBLIC_POST.contains(path)) {
            return true;
        }
        if (docsPublic && isDocsPath(path)) {
            return true;
        }
        return false;
    }

    private static boolean isDocsPath(String path) {
        return path.startsWith("/swagger-ui")
                || path.equals("/swagger-ui.html")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/v3/api-docs.yaml")
                || path.startsWith("/v3/api-docs.yaml/");
    }

    /** Context-path stripping shared with PasswordChangeGuard. */
    public static String stripContext(String requestUri, String contextPath) {
        if (requestUri == null) {
            return "";
        }
        if (contextPath != null && !contextPath.isEmpty() && !"/".equals(contextPath)
                && requestUri.startsWith(contextPath)) {
            String stripped = requestUri.substring(contextPath.length());
            return stripped.isEmpty() ? "/" : stripped;
        }
        return requestUri;
    }
}
