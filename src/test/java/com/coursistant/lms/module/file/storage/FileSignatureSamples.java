package com.coursistant.lms.module.file.storage;

import java.nio.charset.StandardCharsets;

/** Shared magic-byte fixtures for signature and preview XSS tests. */
public final class FileSignatureSamples {

    public static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D
    };
    public static final byte[] JPEG = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    public static final byte[] PDF = "%PDF-1.4\n%".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] GIF = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] WEBP = new byte[] {
            'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P'
    };
    public static final byte[] ZIP_LOCAL = {0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0};
    public static final byte[] ZIP_EMPTY = {0x50, 0x4B, 0x05, 0x06, 0, 0, 0, 0};
    public static final byte[] ZIP_SPANNED = {0x50, 0x4B, 0x07, 0x08, 0, 0, 0, 0};
    public static final byte[] HTML = "<!DOCTYPE html><html><body><script>alert(1)</script></body></html>"
            .getBytes(StandardCharsets.UTF_8);

    private FileSignatureSamples() {
    }
}
