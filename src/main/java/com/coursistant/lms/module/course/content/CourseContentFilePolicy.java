package com.coursistant.lms.module.course.content;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Shared validation rules and object-storage conventions for files and links
 * attached to course content (syllabus, weekly materials, etc.). The upload
 * size limit is configurable via {@code lms.content.max-file-bytes} (default
 * 200MB).
 */
@Component
public class CourseContentFilePolicy {

    private static final long DEFAULT_MAX_FILE_BYTES = 209_715_200L; // 200MB
    private static final String BUCKET = "lms-uploads";

    private static final Set<String> SYLLABUS_CONTENT_TYPES = Set.of("application/pdf");
    private static final Set<String> SYLLABUS_EXTENSIONS = Set.of("pdf");

    private static final Set<String> MATERIAL_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/zip",
            "application/x-zip-compressed",
            "application/octet-stream",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/gif",
            "image/webp"
    );
    private static final Set<String> MATERIAL_EXTENSIONS = Set.of(
            "pdf", "pptx", "docx", "xlsx", "zip", "png", "jpg", "jpeg", "gif", "webp"
    );

    private static final Set<String> PREVIEWABLE_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp"
    );

    private static final int MAX_LINK_TITLE_LENGTH = 255;
    private static final int MAX_LINK_URL_LENGTH = 2048;

    @Value("${lms.content.max-file-bytes:" + DEFAULT_MAX_FILE_BYTES + "}")
    private long maxFileBytes;

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    /**
     * The shared MinIO bucket used for all course content objects (syllabus, materials, ...).
     */
    public String bucket() {
        return BUCKET;
    }

    /**
     * Syllabus uploads must be a PDF file within the configured size limit.
     */
    public void validateSyllabusPdf(MultipartFile file) {
        requireNonEmpty(file);
        requireWithinSizeLimit(file);
        requireAllowedType(file, SYLLABUS_CONTENT_TYPES, SYLLABUS_EXTENSIONS, "Syllabus must be a PDF file");
    }

    /**
     * Weekly material uploads allow PDF, PPTX, DOCX, XLSX, common images, and ZIP archives.
     */
    public void validateMaterialFile(MultipartFile file) {
        requireNonEmpty(file);
        requireWithinSizeLimit(file);
        requireAllowedType(file, MATERIAL_CONTENT_TYPES, MATERIAL_EXTENSIONS,
                "Material must be a PDF, PPTX, DOCX, XLSX, image, or ZIP file");
    }

    /**
     * Alias for {@link #validateMaterialFile(MultipartFile)}, kept for callers that
     * validate a generic course content file without naming the specific content type.
     */
    public void validateFile(MultipartFile file) {
        validateMaterialFile(file);
    }

    /**
     * Validates a link's title and URL (used for weekly material links).
     */
    public void validateLink(String title, String url) {
        if (title == null || title.trim().isEmpty()) {
            throw new ApiException(ErrorType.INVALID_LINK, "Link title is required");
        }
        if (title.trim().length() > MAX_LINK_TITLE_LENGTH) {
            throw new ApiException(ErrorType.INVALID_LINK,
                    "Link title must be at most " + MAX_LINK_TITLE_LENGTH + " characters");
        }
        if (url == null || url.trim().isEmpty()) {
            throw new ApiException(ErrorType.INVALID_LINK, "Link URL is required");
        }
        String trimmedUrl = url.trim();
        if (trimmedUrl.length() > MAX_LINK_URL_LENGTH) {
            throw new ApiException(ErrorType.INVALID_LINK,
                    "Link URL must be at most " + MAX_LINK_URL_LENGTH + " characters");
        }
        String lower = trimmedUrl.toLowerCase(Locale.ROOT);
        if (!(lower.startsWith("http://") || lower.startsWith("https://"))) {
            throw new ApiException(ErrorType.INVALID_LINK, "Link URL must start with http:// or https://");
        }
    }

    /**
     * Lower-cased extension (without the dot) of a filename, or empty string if none.
     */
    public String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Builds a collision-free MinIO object key under the given logical prefix,
     * preserving the original file's extension.
     */
    public String buildObjectKey(String prefix, String originalFilename) {
        String extension = extensionOf(originalFilename);
        String uniqueName = UUID.randomUUID().toString().replace("-", "");
        return prefix + "/" + uniqueName + (extension.isEmpty() ? "" : "." + extension);
    }

    /**
     * Whether a file with the given content type/extension can be streamed inline
     * for browser preview (PDF or common image formats).
     */
    public boolean isPreviewable(String contentType, String extension) {
        String ext = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        if (PREVIEWABLE_EXTENSIONS.contains(ext)) {
            return true;
        }
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return type.equals("application/pdf") || type.startsWith("image/");
    }

    private void requireNonEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "File is required");
        }
    }

    private void requireWithinSizeLimit(MultipartFile file) {
        if (file.getSize() > maxFileBytes) {
            throw new ApiException(ErrorType.FILE_TOO_LARGE,
                    "File exceeds the maximum allowed size of " + maxFileBytes + " bytes");
        }
    }

    private void requireAllowedType(MultipartFile file, Set<String> allowedContentTypes,
                                     Set<String> allowedExtensions, String message) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extensionOf(file.getOriginalFilename());
        boolean contentTypeOk = allowedContentTypes.contains(contentType);
        boolean extensionOk = allowedExtensions.contains(extension);
        if (!contentTypeOk && !extensionOk) {
            throw new ApiException(ErrorType.UNSUPPORTED_FILE_TYPE, message);
        }
    }
}
