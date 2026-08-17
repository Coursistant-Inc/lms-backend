package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * Builds preview/download responses after sniffing the S3 stream. Not a Spring bean.
 */
public final class SecureFileResponse {

    private SecureFileResponse() {
    }

    public static ResponseEntity<InputStreamResource> from(
            S3ObjectPayload payload,
            String filename,
            String extensionHint,
            boolean attachment,
            ErrorType previewDenied) {
        return from(payload, filename, extensionHint, attachment, previewDenied, null);
    }

    public static ResponseEntity<InputStreamResource> from(
            S3ObjectPayload payload,
            String filename,
            String extensionHint,
            boolean attachment,
            ErrorType previewDenied,
            FileSignature.Kind requiredKind) {
        FileObjectSniff.Result sniffed;
        try {
            sniffed = FileObjectSniff.wrap(payload);
        } catch (RuntimeException e) {
            payload.close();
            throw e;
        }
        boolean previewOk = requiredKind != null
                ? sniffed.kind() == requiredKind
                : FileSignature.isPreviewable(sniffed.kind());
        if (!attachment && !previewOk) {
            sniffed.abort();
            throw new ApiException(previewDenied,
                    "Preview is only available for PDF and image files; use download instead");
        }
        String mime = FileSignature.canonicalMime(sniffed.kind(), extensionHint);
        if (sniffed.kind() == FileSignature.Kind.UNKNOWN) {
            mime = "application/octet-stream";
        }
        String safeName = FileDownloadHeaders.sanitizeFilename(filename);
        String disposition = (attachment || !FileSignature.isPreviewable(sniffed.kind()) ? "attachment" : "inline")
                + "; filename=\"" + safeName + "\"";
        long length = S3DownloadBody.contentLength(sniffed.payload());
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.parseMediaType(mime));
        FileDownloadHeaders.applySecurity(builder);
        if (length >= 0) {
            builder.contentLength(length);
        }
        return builder.body(S3DownloadBody.resource(sniffed.payload(), sniffed.stream()));
    }
}
