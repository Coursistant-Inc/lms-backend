package com.coursistant.lms.module.assignment.service;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        service.upload("assignment/1/2/a.pdf", file, 1, 2, 3);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).putObject(key.capture(), eq(file));
        assertEquals("lms-uploads/assignment/1/2/a.pdf", key.getValue());
    }

    @Test
    void upload_storageFailure_is503() {
        MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "x".getBytes());
        doThrow(new S3StorageException("403")).when(s3ObjectStorage).putObject(anyString(), eq(file));

        ApiException ex = assertThrows(ApiException.class, () -> service.upload("k", file, 1, 2, 3));
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
        byte[] body = "pdf-bytes".getBytes(StandardCharsets.UTF_8);
        S3ObjectPayload payload = new S3ObjectPayload(
                new ByteArrayInputStream(body),
                new S3ObjectMetadata("application/pdf", (long) body.length, "etag"));
        when(s3ObjectStorage.getObject("lms-uploads/k")).thenReturn(payload);

        ResponseEntity<InputStreamResource> response =
                service.stream("k", "报告.pdf", "application/pdf", true, 1, 2, 3);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body.length, response.getHeaders().getContentLength());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertArrayEquals(body, response.getBody().getInputStream().readAllBytes());
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
