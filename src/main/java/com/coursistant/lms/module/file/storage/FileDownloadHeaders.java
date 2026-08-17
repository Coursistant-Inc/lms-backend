package com.coursistant.lms.module.file.storage;

import org.springframework.http.ResponseEntity;

/**
 * Shared security headers for binary download/preview responses. Not a Spring bean.
 */
public final class FileDownloadHeaders {

    public static final String CONTENT_SECURITY_POLICY = "sandbox; default-src 'none'";
    public static final String CONTENT_SECURITY_POLICY_HEADER = "Content-Security-Policy";

    private FileDownloadHeaders() {
    }

    public static void applySecurity(ResponseEntity.BodyBuilder builder) {
        builder.header(CONTENT_SECURITY_POLICY_HEADER, CONTENT_SECURITY_POLICY);
        builder.header("X-Content-Type-Options", "nosniff");
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }
}
