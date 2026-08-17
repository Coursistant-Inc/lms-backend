package com.coursistant.lms.module.file.storage;

import org.springframework.core.io.InputStreamResource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * HTTP download adapter for {@link S3ObjectPayload}.
 *
 * <p>Spring Framework 6.2.5 (Boot 3.4.4) has no {@code InputStreamResource(InputStream, long)}
 * constructor. Returning a standard {@link InputStreamResource} whose {@code contentLength()}
 * is overridden from metadata avoids {@link org.springframework.core.io.AbstractResource}'s
 * default implementation, which would pre-read the one-shot S3 stream. The factory does not
 * subclass {@link InputStreamResource} as a named type; {@link FilterInputStream#close()} is
 * idempotent and closes the payload exactly once.
 */
public final class S3DownloadBody {

    private S3DownloadBody() {
    }

    public static InputStreamResource resource(S3ObjectPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        return resource(payload, payload.content());
    }

    public static InputStreamResource resource(S3ObjectPayload payload, InputStream body) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        if (body == null) {
            throw new IllegalArgumentException("body must not be null");
        }
        InputStream filter = new OnceClosedStream(payload, body);
        long length = contentLength(payload);
        return new InputStreamResource(filter) {
            @Override
            public long contentLength() {
                return length;
            }
        };
    }

    public static long contentLength(S3ObjectPayload payload) {
        if (payload == null || payload.metadata() == null || payload.metadata().contentLength() == null) {
            return -1L;
        }
        return payload.metadata().contentLength();
    }

    private static final class OnceClosedStream extends FilterInputStream {

        private final S3ObjectPayload payload;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        private OnceClosedStream(S3ObjectPayload payload, InputStream body) {
            super(body);
            this.payload = payload;
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            payload.close();
        }
    }
}
