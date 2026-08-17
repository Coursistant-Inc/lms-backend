package com.coursistant.lms.module.file.storage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileObjectSniffTest {

    @Test
    void wrap_unreadKeepsOriginalBytes() throws Exception {
        byte[] body = new byte[FileSignatureSamples.PNG.length + 8];
        System.arraycopy(FileSignatureSamples.PNG, 0, body, 0, FileSignatureSamples.PNG.length);
        for (int i = FileSignatureSamples.PNG.length; i < body.length; i++) {
            body[i] = (byte) i;
        }
        S3ObjectPayload payload = new S3ObjectPayload(
                new ByteArrayInputStream(body), new S3ObjectMetadata("image/png", (long) body.length, "e"));

        FileObjectSniff.Result sniffed = FileObjectSniff.wrap(payload);
        assertEquals(FileSignature.Kind.PNG, sniffed.kind());
        assertArrayEquals(body, sniffed.stream().readAllBytes());
    }

    @Test
    void abort_closesUnderlyingStream() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream tracking = new FilterInputStream(new ByteArrayInputStream(FileSignatureSamples.HTML)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        S3ObjectPayload payload = new S3ObjectPayload(
                tracking, new S3ObjectMetadata("text/html", (long) FileSignatureSamples.HTML.length, "e"));

        FileObjectSniff.Result sniffed = FileObjectSniff.wrap(payload);
        assertEquals(FileSignature.Kind.UNKNOWN, sniffed.kind());
        sniffed.abort();
        assertTrue(closed.get());
    }
}
