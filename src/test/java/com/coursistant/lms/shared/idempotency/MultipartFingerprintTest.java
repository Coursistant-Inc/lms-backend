package com.coursistant.lms.shared.idempotency;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MultipartFingerprintTest {

    @Test
    void includesFileIndexAndContentHash() {
        MockMultipartHttpServletRequest a = new MockMultipartHttpServletRequest();
        a.setMethod("POST");
        a.setRequestURI("/v2/courses/1/weeks/2/materials");
        a.addParameter("linkUrl", "https://example.com");
        a.addFile(new MockMultipartFile("files", "a.txt", "text/plain", "hello".getBytes()));
        a.addFile(new MockMultipartFile("files", "b.txt", "text/plain", "world".getBytes()));

        MockMultipartHttpServletRequest b = new MockMultipartHttpServletRequest();
        b.setMethod("POST");
        b.setRequestURI("/v2/courses/1/weeks/2/materials");
        b.addParameter("linkUrl", "https://example.com");
        b.addFile(new MockMultipartFile("files", "b.txt", "text/plain", "world".getBytes()));
        b.addFile(new MockMultipartFile("files", "a.txt", "text/plain", "hello".getBytes()));

        String fa = MultipartFingerprint.compute(a);
        String fb = MultipartFingerprint.compute(b);
        assertNotEquals(fa, fb, "index order must affect fingerprint");

        MockMultipartHttpServletRequest a2 = new MockMultipartHttpServletRequest();
        a2.setMethod("POST");
        a2.setRequestURI("/v2/courses/1/weeks/2/materials");
        a2.addParameter("linkUrl", "https://example.com");
        a2.addFile(new MockMultipartFile("files", "a.txt", "text/plain", "hello".getBytes()));
        a2.addFile(new MockMultipartFile("files", "b.txt", "text/plain", "world".getBytes()));
        assertEquals(fa, MultipartFingerprint.compute(a2));
    }
}
