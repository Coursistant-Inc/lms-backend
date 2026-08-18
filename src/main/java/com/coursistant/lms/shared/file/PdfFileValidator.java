package com.coursistant.lms.shared.file;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;

/**
 * Structural validation for uploads that claim to be PDF documents.
 *
 * <p>Checking only the filename or MIME type accepts truncated fixtures that begin with
 * {@code %PDF} but have no page tree or cross-reference data. Such objects download normally,
 * yet every browser PDF viewer rejects them. Loading through PDFBox before storage prevents
 * those permanently broken objects from entering the system.</p>
 */
public final class PdfFileValidator {

    private PdfFileValidator() {
    }

    /**
     * Validates PDF structure when the filename or content type identifies the upload as PDF.
     * Non-PDF uploads are intentionally ignored so callers can keep their own allow-list rules.
     */
    public static void validateIfPdf(MultipartFile file) {
        if (file == null || !isPdf(file)) {
            return;
        }

        try (InputStream stream = file.getInputStream();
             PDDocument document = PDDocument.load(stream, MemoryUsageSetting.setupTempFileOnly())) {
            if (document.getNumberOfPages() < 1) {
                throw invalidPdf();
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidPdf();
        }
    }

    private static boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        String filename = file.getOriginalFilename() == null
                ? ""
                : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        return "application/pdf".equals(contentType) || filename.endsWith(".pdf");
    }

    private static ApiException invalidPdf() {
        return new ApiException(ErrorType.UNSUPPORTED_FILE_TYPE,
                "The uploaded PDF is invalid or corrupted");
    }
}
