package com.coursistant.lms.module.file.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class S3ObjectKeyResolverTest {

    private final S3ObjectKeyResolver resolver = new S3ObjectKeyResolver();

    @Test
    void resolve_avatarPrefix() {
        assertEquals("avatar/users/1/a.png", resolver.resolve("avatar", "users/1/a.png"));
    }

    @Test
    void resolve_lmsUploadsPrefix() {
        assertEquals("lms-uploads/courses/1/file.pdf", resolver.resolve("lms-uploads", "courses/1/file.pdf"));
    }

    @Test
    void resolve_stripsLeadingSlashes() {
        assertEquals("avatar/users/1/a.png", resolver.resolve("avatar", "///users/1/a.png"));
    }

    @Test
    void resolve_preservesChineseSpacesAndPlus() {
        assertEquals("lms-uploads/资料 作业+.pdf", resolver.resolve("lms-uploads", "资料 作业+.pdf"));
    }

    @Test
    void resolve_rejectsBlankKey() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("avatar", "  "));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("avatar", null));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("avatar", "///"));
    }

    @Test
    void resolve_rejectsDotDotSegments() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("avatar", "../secret"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("lms-uploads", "a/../b"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("lms-uploads", "a/.."));
    }

    @Test
    void resolve_rejectsUnknownBucket() {
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("other", "a.png"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(" ", "a.png"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null, "a.png"));
    }
}
