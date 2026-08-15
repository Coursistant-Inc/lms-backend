package com.coursistant.lms.module.file.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public final class S3ObjectPayload implements AutoCloseable {

    private final InputStream content;
    private final S3ObjectMetadata metadata;

    public S3ObjectPayload(InputStream content, S3ObjectMetadata metadata) {
        this.content = content;
        this.metadata = metadata;
    }

    public InputStream content() {
        return content;
    }

    public S3ObjectMetadata metadata() {
        return metadata;
    }

    @Override
    public void close() {
        if (content == null) {
            return;
        }
        try {
            content.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close S3 object stream", e);
        }
    }
}
