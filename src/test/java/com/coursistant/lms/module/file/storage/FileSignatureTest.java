package com.coursistant.lms.module.file.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileSignatureTest {

    @Test
    void detect_knownMagicBytes() {
        assertEquals(FileSignature.Kind.PDF, FileSignature.detect(FileSignatureSamples.PDF));
        assertEquals(FileSignature.Kind.PNG, FileSignature.detect(FileSignatureSamples.PNG));
        assertEquals(FileSignature.Kind.JPEG, FileSignature.detect(FileSignatureSamples.JPEG));
        assertEquals(FileSignature.Kind.GIF, FileSignature.detect(FileSignatureSamples.GIF));
        assertEquals(FileSignature.Kind.WEBP, FileSignature.detect(FileSignatureSamples.WEBP));
        assertEquals(FileSignature.Kind.ZIP, FileSignature.detect(FileSignatureSamples.ZIP_LOCAL));
        assertEquals(FileSignature.Kind.ZIP, FileSignature.detect(FileSignatureSamples.ZIP_EMPTY));
        assertEquals(FileSignature.Kind.ZIP, FileSignature.detect(FileSignatureSamples.ZIP_SPANNED));
    }

    @Test
    void detect_emptyOrShort_isUnknown() {
        assertEquals(FileSignature.Kind.UNKNOWN, FileSignature.detect(new byte[0]));
        assertEquals(FileSignature.Kind.UNKNOWN, FileSignature.detect((byte[]) null));
        assertEquals(FileSignature.Kind.UNKNOWN, FileSignature.detect("x".getBytes()));
        assertEquals(FileSignature.Kind.UNKNOWN, FileSignature.detect(FileSignatureSamples.HTML));
    }

    @Test
    void detect_multipartFile_readsHeaderOnly() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "text/html", FileSignatureSamples.PNG);
        assertEquals(FileSignature.Kind.PNG, FileSignature.detect(file));
    }

    @Test
    void matchesExtension_knownFamilyMustAgree() {
        assertTrue(FileSignature.matchesExtension(FileSignature.Kind.PNG, "png"));
        assertFalse(FileSignature.matchesExtension(FileSignature.Kind.JPEG, "png"));
        assertFalse(FileSignature.matchesExtension(FileSignature.Kind.UNKNOWN, "png"));
        assertFalse(FileSignature.matchesExtension(FileSignature.Kind.UNKNOWN, "pdf"));
        assertTrue(FileSignature.matchesExtension(FileSignature.Kind.UNKNOWN, "txt"));
        assertTrue(FileSignature.matchesExtension(FileSignature.Kind.ZIP, "docx"));
        assertTrue(FileSignature.matchesExtension(FileSignature.Kind.ZIP, "zip"));
        assertFalse(FileSignature.matchesExtension(FileSignature.Kind.ZIP, "pdf"));
    }

    @Test
    void canonicalMime_zipDependsOnExtension() {
        assertEquals("application/pdf", FileSignature.canonicalMime(FileSignature.Kind.PDF, "pdf"));
        assertEquals("image/png", FileSignature.canonicalMime(FileSignature.Kind.PNG, "png"));
        assertEquals("application/zip", FileSignature.canonicalMime(FileSignature.Kind.ZIP, "zip"));
        assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                FileSignature.canonicalMime(FileSignature.Kind.ZIP, "docx"));
        assertEquals("application/octet-stream", FileSignature.canonicalMime(FileSignature.Kind.UNKNOWN, "txt"));
    }

    @Test
    void isPreviewable_onlyPdfAndImages() {
        assertTrue(FileSignature.isPreviewable(FileSignature.Kind.PDF));
        assertTrue(FileSignature.isPreviewable(FileSignature.Kind.PNG));
        assertFalse(FileSignature.isPreviewable(FileSignature.Kind.ZIP));
        assertFalse(FileSignature.isPreviewable(FileSignature.Kind.UNKNOWN));
    }
}
