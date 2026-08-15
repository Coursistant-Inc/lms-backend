package com.coursistant.lms.module.file.storage;

public record S3ObjectMetadata(String contentType, Long contentLength, String etag) {

    public static String normalizeEtag(String etag) {
        if (etag == null || etag.isBlank()) {
            return etag;
        }
        String trimmed = etag.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
