package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.shared.config.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.MetadataDirective;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3ObjectStorageTest {

    private static final String BUCKET = "lms-uploads";

    @Mock
    private S3Client s3Client;

    private S3ObjectStorage storage;

    @BeforeEach
    void setUp() {
        S3Properties properties = new S3Properties();
        properties.setEnabled(true);
        properties.setRegion("us-west-2");
        properties.setBucket(BUCKET);
        storage = new S3ObjectStorage(s3Client, properties);
    }

    @Test
    void putObject_sendsFixedBucketKeyContentTypeAndLength() {
        byte[] body = "hello".getBytes(StandardCharsets.UTF_8);

        storage.putObject("course/a.txt", new ByteArrayInputStream(body), body.length, "text/plain");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        PutObjectRequest req = captor.getValue();
        assertEquals(BUCKET, req.bucket());
        assertEquals("course/a.txt", req.key());
        assertEquals("text/plain", req.contentType());
        assertEquals(body.length, req.contentLength());
    }

    @Test
    void putObject_multipart_usesExplicitCanonicalMimeNotClientType() throws Exception {
        MultipartFile file = new MockMultipartFile(
                "file", "x.png", "text/html", FileSignatureSamples.PNG);

        storage.putObject("img.png", file, "image/png");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals("image/png", captor.getValue().contentType());
        assertEquals("img.png", captor.getValue().key());
    }

    @Test
    void putObject_multipartOverloadRequiresCanonicalMime() {
        long twoArgMultipart = java.util.Arrays.stream(S3ObjectStorage.class.getMethods())
                .filter(m -> "putObject".equals(m.getName())
                        && m.getParameterCount() == 2
                        && MultipartFile.class.isAssignableFrom(m.getParameterTypes()[1]))
                .count();
        assertEquals(0, twoArgMultipart);
    }

    @Test
    void putObject_multipart_blankCanonicalMimeDefaultsToOctetStream() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "x.bin", "  ", "abc".getBytes(StandardCharsets.UTF_8));

        storage.putObject("disk/x.bin", file, "  ");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertEquals(S3ObjectStorage.DEFAULT_CONTENT_TYPE, captor.getValue().contentType());
        assertEquals(3L, captor.getValue().contentLength());
        assertEquals("disk/x.bin", captor.getValue().key());
    }

    @Test
    void getObject_returnsMetadataAndUnderlyingStream() throws Exception {
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("text/plain")
                .contentLength((long) body.length)
                .eTag("\"abc123\"")
                .build();
        ResponseInputStream<GetObjectResponse> stream = new ResponseInputStream<>(
                response, AbortableInputStream.create(new ByteArrayInputStream(body)));
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(stream);

        try (S3ObjectPayload payload = storage.getObject("k")) {
            assertEquals("text/plain", payload.metadata().contentType());
            assertEquals((long) body.length, payload.metadata().contentLength());
            assertEquals("abc123", S3ObjectMetadata.normalizeEtag(payload.metadata().etag()));
            assertEquals("payload", new String(payload.content().readAllBytes(), StandardCharsets.UTF_8));
        }

        ArgumentCaptor<GetObjectRequest> captor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals("k", captor.getValue().key());
    }

    @Test
    void payloadClose_closesUnderlyingStream() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream tracking = new FilterInputStream(new ByteArrayInputStream(new byte[]{1})) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        GetObjectResponse response = GetObjectResponse.builder()
                .contentType("application/octet-stream")
                .contentLength(1L)
                .eTag("etag")
                .build();
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(response, AbortableInputStream.create(tracking)));

        storage.getObject("k").close();
        assertTrue(closed.get());
    }

    @Test
    void deleteObject_usesFixedBucketAndKey() {
        storage.deleteObject("gone.txt");

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals(BUCKET, captor.getValue().bucket());
        assertEquals("gone.txt", captor.getValue().key());
    }

    @Test
    void copyObject_sameBucket_preservesMetadataDirectiveAndSpecialKeys() {
        String source = "path with space/中文+plus.txt";
        storage.copyObject(source, "dest.txt");

        ArgumentCaptor<CopyObjectRequest> captor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(captor.capture());
        CopyObjectRequest req = captor.getValue();
        assertEquals(BUCKET, req.sourceBucket());
        assertEquals(source, req.sourceKey());
        assertEquals(BUCKET, req.destinationBucket());
        assertEquals("dest.txt", req.destinationKey());
        assertEquals(MetadataDirective.COPY, req.metadataDirective());
    }

    @Test
    void headObject_present_returnsNormalizedMetadata() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(
                HeadObjectResponse.builder()
                        .contentType("image/png")
                        .contentLength(12L)
                        .eTag("\"deadbeef\"")
                        .build());

        Optional<S3ObjectMetadata> meta = storage.headObject("avatar.png");
        assertTrue(meta.isPresent());
        assertEquals("image/png", meta.get().contentType());
        assertEquals(12L, meta.get().contentLength());
        assertEquals("deadbeef", S3ObjectMetadata.normalizeEtag(meta.get().etag()));
        assertEquals("\"deadbeef\"", meta.get().etag());
    }

    @Test
    void headObject_404_returnsEmpty() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Status(404, "missing"));

        assertTrue(storage.headObject("missing").isEmpty());
    }

    @Test
    void headObject_noSuchKey_returnsEmpty() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(
                NoSuchKeyException.builder().message("gone").build());

        assertTrue(storage.headObject("missing").isEmpty());
    }

    @Test
    void headObject_403_throwsStorageException() {
        S3Exception forbidden = s3Status(403, "denied");
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(forbidden);

        S3StorageException ex = assertThrows(S3StorageException.class, () -> storage.headObject("secret"));
        assertSame(forbidden, ex.getCause());
    }

    @Test
    void headObject_500_throwsStorageException() {
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(s3Status(500, "boom"));

        assertThrows(S3StorageException.class, () -> storage.headObject("k"));
    }

    @Test
    void getObject_404_throwsNotFound() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(s3Status(404, "missing"));

        assertThrows(S3ObjectNotFoundException.class, () -> storage.getObject("missing"));
    }

    @Test
    void blankKey_doesNotCallSdk() {
        assertThrows(IllegalArgumentException.class, () -> storage.putObject(" ", new ByteArrayInputStream(new byte[0]), 0, "text/plain"));
        assertThrows(IllegalArgumentException.class, () -> storage.getObject(null));
        assertThrows(IllegalArgumentException.class, () -> storage.deleteObject(""));
        assertThrows(IllegalArgumentException.class, () -> storage.headObject("\t"));
        assertThrows(IllegalArgumentException.class, () -> storage.copyObject("ok", " "));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(s3Client, never()).getObject(any(GetObjectRequest.class));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
        verify(s3Client, never()).headObject(any(HeadObjectRequest.class));
        verify(s3Client, never()).copyObject(any(CopyObjectRequest.class));
    }

    @Test
    void negativeContentLength_doesNotCallSdk() {
        assertThrows(IllegalArgumentException.class,
                () -> storage.putObject("k", new ByteArrayInputStream(new byte[0]), -1, "text/plain"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void nullStreamAndNullMultipart_doNotCallSdk() {
        assertThrows(IllegalArgumentException.class, () -> storage.putObject("k", (InputStream) null, 0, "text/plain"));
        assertThrows(IllegalArgumentException.class, () -> storage.putObject("k", (MultipartFile) null, "image/png"));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void sdkException_isWrappedWithCause() {
        RuntimeException cause = new RuntimeException("network");
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(cause);

        S3StorageException ex = assertThrows(S3StorageException.class, () -> storage.deleteObject("k"));
        assertSame(cause, ex.getCause());
        assertInstanceOf(S3StorageException.class, ex);
    }

    private static S3Exception s3Status(int status, String message) {
        return (S3Exception) S3Exception.builder().statusCode(status).message(message).build();
    }
}
