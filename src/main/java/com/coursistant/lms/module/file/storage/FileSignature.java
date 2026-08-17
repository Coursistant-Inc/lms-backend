package com.coursistant.lms.module.file.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

/**
 * Magic-byte file type detection. Reads at most {@link #HEADER_BYTES} and never
 * trusts a client Content-Type. Not a Spring bean.
 */
public final class FileSignature {

    public static final int HEADER_BYTES = 12;

    public enum Kind {
        PDF, PNG, JPEG, GIF, WEBP, ZIP, UNKNOWN
    }

    private static final Set<String> KNOWN_FAMILY_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp", "zip", "docx", "pptx", "xlsx");
    private static final Set<String> ZIP_EXTENSIONS = Set.of("zip", "docx", "pptx", "xlsx");

    private static final byte[] PDF = {'%', 'P', 'D', 'F'};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87A = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF89A = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};
    private static final byte[] ZIP_LOCAL = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] ZIP_EMPTY = {0x50, 0x4B, 0x05, 0x06};
    private static final byte[] ZIP_SPANNED = {0x50, 0x4B, 0x07, 0x08};

    private FileSignature() {
    }

    public static Kind detect(MultipartFile file) {
        if (file == null) {
            return Kind.UNKNOWN;
        }
        try (InputStream in = file.getInputStream()) {
            return detect(in.readNBytes(HEADER_BYTES));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded file header", e);
        }
    }

    public static Kind detect(byte[] header) {
        if (header == null || header.length == 0) {
            return Kind.UNKNOWN;
        }
        if (startsWith(header, PDF)) {
            return Kind.PDF;
        }
        if (startsWith(header, PNG)) {
            return Kind.PNG;
        }
        if (startsWith(header, JPEG)) {
            return Kind.JPEG;
        }
        if (startsWith(header, GIF87A) || startsWith(header, GIF89A)) {
            return Kind.GIF;
        }
        if (header.length >= 12 && startsWith(header, RIFF) && regionEquals(header, 8, WEBP)) {
            return Kind.WEBP;
        }
        if (startsWith(header, ZIP_LOCAL) || startsWith(header, ZIP_EMPTY) || startsWith(header, ZIP_SPANNED)) {
            return Kind.ZIP;
        }
        return Kind.UNKNOWN;
    }

    public static String canonicalMime(Kind kind, String normalizedExtension) {
        String ext = normalizedExtension == null ? "" : normalizedExtension.toLowerCase(Locale.ROOT);
        return switch (kind) {
            case PDF -> "application/pdf";
            case PNG -> "image/png";
            case JPEG -> "image/jpeg";
            case GIF -> "image/gif";
            case WEBP -> "image/webp";
            case ZIP -> zipMime(ext);
            case UNKNOWN -> "application/octet-stream";
        };
    }

    public static boolean isKnownFamilyExtension(String extension) {
        return extension != null && KNOWN_FAMILY_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT));
    }

    public static boolean matchesExtension(Kind kind, String extension) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return switch (kind) {
            case PDF -> "pdf".equals(ext);
            case PNG -> "png".equals(ext);
            case JPEG -> "jpg".equals(ext) || "jpeg".equals(ext);
            case GIF -> "gif".equals(ext);
            case WEBP -> "webp".equals(ext);
            case ZIP -> ZIP_EXTENSIONS.contains(ext);
            case UNKNOWN -> !isKnownFamilyExtension(ext);
        };
    }

    public static boolean isPreviewable(Kind kind) {
        return kind == Kind.PDF || kind == Kind.PNG || kind == Kind.JPEG
                || kind == Kind.GIF || kind == Kind.WEBP;
    }

    private static String zipMime(String ext) {
        return switch (ext) {
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/zip";
        };
    }

    private static boolean startsWith(byte[] header, byte[] prefix) {
        return regionEquals(header, 0, prefix);
    }

    private static boolean regionEquals(byte[] header, int offset, byte[] expected) {
        if (header.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (header[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}
