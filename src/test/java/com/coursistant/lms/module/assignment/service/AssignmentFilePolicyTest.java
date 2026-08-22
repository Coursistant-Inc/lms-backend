package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.file.storage.FileSignatureSamples;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssignmentFilePolicyTest {

    private final AssignmentFilePolicy policy = new AssignmentFilePolicy();

    @Test
    void xssU4_plainTextAllowedByAssignment_uploadsAsOctetStream() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "hello notes".getBytes(StandardCharsets.UTF_8));
        String mime = policy.validateSubmissionFile(file, List.of("txt"), 10_000L);
        assertEquals("application/octet-stream", mime);
    }

    @Test
    void isPreviewable_pdfAndImagesOnly() {
        assertEquals(true, policy.isPreviewable("application/pdf", "brief.pdf"));
        assertEquals(true, policy.isPreviewable("image/png", "diagram.PNG"));
        assertEquals(false, policy.isPreviewable("application/zip", "bundle.zip"));
        assertEquals(false, policy.isPreviewable("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "prompt.docx"));
    }

    @Test
    void htmlNamedPng_isUnsupportedEvenIfClientSaysImage() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "a.png", "image/png", FileSignatureSamples.HTML);
        ApiException ex = assertThrows(ApiException.class,
                () -> policy.validateAttachmentFile(file));
        assertEquals(ErrorType.UNSUPPORTED_FILE_TYPE, ex.getErrorType());
    }
}
