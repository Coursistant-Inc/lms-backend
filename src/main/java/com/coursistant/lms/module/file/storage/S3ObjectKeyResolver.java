package com.coursistant.lms.module.file.storage;

import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Maps a logical MinIO-style bucket plus object key onto a single-bucket S3 physical key.
 * Logical buckets become prefixes; callers keep generating the same logical keys.
 */
@Component
public class S3ObjectKeyResolver {

    static final Set<String> ALLOWED_LOGICAL_BUCKETS = Set.of("avatar", "lms-uploads");

    public String resolve(String logicalBucket, String objectKey) {
        if (logicalBucket == null || logicalBucket.isBlank()) {
            throw new IllegalArgumentException("logical bucket must not be blank");
        }
        if (!ALLOWED_LOGICAL_BUCKETS.contains(logicalBucket)) {
            throw new IllegalArgumentException("unknown logical bucket: " + logicalBucket);
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("object key must not be blank");
        }

        String trimmed = stripLeadingSlashes(objectKey);
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("object key must not be blank");
        }
        rejectDotDotSegments(trimmed);
        return logicalBucket + "/" + trimmed;
    }

    private static String stripLeadingSlashes(String objectKey) {
        int i = 0;
        while (i < objectKey.length() && objectKey.charAt(i) == '/') {
            i++;
        }
        return objectKey.substring(i);
    }

    private static void rejectDotDotSegments(String key) {
        int start = 0;
        while (start <= key.length()) {
            int slash = key.indexOf('/', start);
            String segment = slash < 0 ? key.substring(start) : key.substring(start, slash);
            if ("..".equals(segment)) {
                throw new IllegalArgumentException("object key must not contain '..' path segments");
            }
            if (slash < 0) {
                break;
            }
            start = slash + 1;
        }
    }
}
