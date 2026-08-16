package com.coursistant.lms.module.file.storage;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class S3DownloadBodyTest {

    @Test
    void close_isIdempotentAndClosesPayloadOnce() throws Exception {
        AtomicInteger closes = new AtomicInteger();
        byte[] body = "hello-world".getBytes(StandardCharsets.UTF_8);
        S3ObjectPayload payload = payload(body, "text/plain", (long) body.length, closes);

        InputStreamResource resource = S3DownloadBody.resource(payload);
        assertEquals(body.length, resource.contentLength());
        InputStream in = resource.getInputStream();
        assertArrayEquals(body, in.readAllBytes());
        in.close();
        in.close();
        assertEquals(1, closes.get());
    }

    @Test
    void close_afterReadFailure_stillClosesOnce() throws Exception {
        AtomicInteger closes = new AtomicInteger();
        InputStream failing = new FilterInputStream(new ByteArrayInputStream(new byte[]{1, 2, 3})) {
            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void close() throws IOException {
                closes.incrementAndGet();
                super.close();
            }
        };
        S3ObjectPayload payload = new S3ObjectPayload(failing, new S3ObjectMetadata("text/plain", 3L, "etag"));

        InputStreamResource resource = S3DownloadBody.resource(payload);
        InputStream in = resource.getInputStream();
        assertThrows(IOException.class, in::readAllBytes);
        in.close();
        in.close();
        assertEquals(1, closes.get());
    }

    @Test
    void contentLength_comesFromMetadataWithoutReadingStream() throws Exception {
        AtomicBoolean read = new AtomicBoolean(false);
        InputStream tracking = new FilterInputStream(new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8))) {
            @Override
            public int read() throws IOException {
                read.set(true);
                return super.read();
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                read.set(true);
                return super.read(b, off, len);
            }
        };
        S3ObjectPayload payload = new S3ObjectPayload(tracking, new S3ObjectMetadata("text/plain", 3L, "etag"));
        InputStreamResource resource = S3DownloadBody.resource(payload);
        assertEquals(3L, resource.contentLength());
        assertEquals(3L, S3DownloadBody.contentLength(payload));
        assertTrue(!read.get());
    }

    private static S3ObjectPayload payload(byte[] body, String contentType, Long length, AtomicInteger closes) {
        InputStream in = counting(new ByteArrayInputStream(body), closes);
        return new S3ObjectPayload(in, new S3ObjectMetadata(contentType, length, "etag"));
    }

    private static InputStream counting(InputStream delegate, AtomicInteger closes) {
        return new FilterInputStream(delegate) {
            @Override
            public void close() throws IOException {
                closes.incrementAndGet();
                super.close();
            }
        };
    }
}
