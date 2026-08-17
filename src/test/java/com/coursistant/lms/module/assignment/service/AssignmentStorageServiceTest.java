package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.file.storage.FileDownloadHeaders;
import com.coursistant.lms.module.file.storage.FileSignatureSamples;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectMetadata;
import com.coursistant.lms.module.file.storage.S3ObjectNotFoundException;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentStorageServiceTest {

    @Mock
    private S3ObjectStorage s3ObjectStorage;

    @Spy
    private S3ObjectKeyResolver s3ObjectKeyResolver = new S3ObjectKeyResolver();

    @Spy
    private AssignmentFilePolicy assignmentFilePolicy = new AssignmentFilePolicy();

    @InjectMocks
    private AssignmentStorageService service;

    @Test
    void upload_putsResolvedLmsUploadsKey() {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());
        service.upload("assignment/1/2/a.pdf", file, "application/pdf", 1, 2, 3);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).putObject(key.capture(), eq(file), eq("application/pdf"));
        assertEquals("lms-uploads/assignment/1/2/a.pdf", key.getValue());
    }

    @Test
    void upload_storageFailure_is503() {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());
        doThrow(new S3StorageException("403")).when(s3ObjectStorage)
                .putObject(anyString(), eq(file), eq("application/pdf"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.upload("k", file, "application/pdf", 1, 2, 3));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void stream_notFound_isStillStorageFailure() throws Exception {
        when(s3ObjectStorage.getObject("lms-uploads/missing"))
                .thenThrow(new S3ObjectNotFoundException("missing"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.stream("missing", "a.pdf", "application/pdf", true, 1, 2, 3));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void stream_forbidden_isStorageFailure() {
        when(s3ObjectStorage.getObject("lms-uploads/k"))
                .thenThrow(new S3StorageException("403"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.stream("k", "a.pdf", "application/pdf", true, 1, 2, 3));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
    }

    @Test
    void stream_setsContentTypeAndLength() throws Exception {
        byte[] body = FileSignatureSamples.PDF;
        S3ObjectPayload payload = new S3ObjectPayload(
                new ByteArrayInputStream(body),
                new S3ObjectMetadata("application/pdf", (long) body.length, "etag"));
        when(s3ObjectStorage.getObject("lms-uploads/k")).thenReturn(payload);

        ResponseEntity<InputStreamResource> response =
                service.stream("k", "报告.pdf", "application/pdf", true, 1, 2, 3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body.length, response.getHeaders().getContentLength());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).contains("attachment"));
        assertEquals(FileDownloadHeaders.CONTENT_SECURITY_POLICY,
                response.getHeaders().getFirst(FileDownloadHeaders.CONTENT_SECURITY_POLICY_HEADER));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertArrayEquals(body, response.getBody().getInputStream().readAllBytes());
    }

    @Test
    void xssP3_htmlPreview_isUnsupportedAndClosesStream() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream tracking = new FilterInputStream(new ByteArrayInputStream(FileSignatureSamples.HTML)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        when(s3ObjectStorage.getObject("lms-uploads/k.png")).thenReturn(
                new S3ObjectPayload(tracking, new S3ObjectMetadata("text/html",
                        (long) FileSignatureSamples.HTML.length, "e")));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.stream("k.png", "test.png", "text/html", false, 1, 2, 3));
        assertEquals(ErrorType.UNSUPPORTED_FILE_TYPE, ex.getErrorType());
        assertTrue(closed.get());
    }

    @Test
    void xssP3_pdfPreview_isInlineWithCsp() throws Exception {
        when(s3ObjectStorage.getObject("lms-uploads/k.pdf")).thenReturn(
                new S3ObjectPayload(new ByteArrayInputStream(FileSignatureSamples.PDF),
                        new S3ObjectMetadata("application/pdf", (long) FileSignatureSamples.PDF.length, "e")));

        ResponseEntity<InputStreamResource> response =
                service.stream("k.pdf", "a.pdf", "text/html", false, 1, 2, 3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        assertTrue(response.getHeaders().getFirst(FileDownloadHeaders.CONTENT_SECURITY_POLICY_HEADER).contains("sandbox"));
    }

    @Test
    void deleteQuietly_blank_doesNotCallStorage() {
        service.deleteQuietly(" ");
        verify(s3ObjectStorage, never()).deleteObject(anyString());
    }

    @Test
    void deleteQuietly_missingObject_doesNotThrow() {
        doThrow(new S3ObjectNotFoundException("gone")).when(s3ObjectStorage)
                .deleteObject("lms-uploads/gone");
        service.deleteQuietly("gone");
        verify(s3ObjectStorage).deleteObject("lms-uploads/gone");
    }

    private static void assertArrayEquals(byte[] expected, byte[] actual) {
        org.junit.jupiter.api.Assertions.assertArrayEquals(expected, actual);
    }
}
