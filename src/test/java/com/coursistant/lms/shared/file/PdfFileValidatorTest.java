package com.coursistant.lms.shared.file;

import com.coursistant.lms.shared.api.ApiException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PdfFileValidatorTest {

    @Test
    void acceptsPdfWithAtLeastOneReadablePage() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(output);
        }
        MockMultipartFile file = new MockMultipartFile(
                "file", "valid.pdf", "application/pdf", output.toByteArray());

        assertDoesNotThrow(() -> PdfFileValidator.validateIfPdf(file));
    }

    @Test
    void rejectsTruncatedPdfFixture() {
        byte[] truncated = "%PDF-1.1\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "broken.pdf", "application/pdf", truncated);

        ApiException exception = assertThrows(ApiException.class,
                () -> PdfFileValidator.validateIfPdf(file));

        assertEquals("The uploaded PDF is invalid or corrupted", exception.getMessage());
    }

    @Test
    void ignoresNonPdfUploads() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "plain text".getBytes());

        assertDoesNotThrow(() -> PdfFileValidator.validateIfPdf(file));
    }
}
