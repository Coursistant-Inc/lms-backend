package com.coursistant.lms.module.file.storage;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.UncheckedIOException;

/**
 * Prefix sniff for one-shot S3 streams. Unreads the header onto a
 * {@link PushbackInputStream} so the HTTP body stays byte-identical.
 */
public final class FileObjectSniff {

    private FileObjectSniff() {
    }

    public static Result wrap(S3ObjectPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        try {
            PushbackInputStream pushback = new PushbackInputStream(payload.content(), FileSignature.HEADER_BYTES);
            byte[] header = pushback.readNBytes(FileSignature.HEADER_BYTES);
            if (header.length > 0) {
                pushback.unread(header);
            }
            return new Result(payload, pushback, FileSignature.detect(header));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to sniff object stream", e);
        }
    }

    public record Result(S3ObjectPayload payload, PushbackInputStream stream, FileSignature.Kind kind) {
        public void abort() {
            payload.close();
        }
    }
}
