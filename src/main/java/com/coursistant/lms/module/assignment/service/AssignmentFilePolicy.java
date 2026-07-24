package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * File rules for the assignment module: the 100MB system ceiling, per-assignment type and size
 * constraints for student submissions, PDF-only rubrics, and the MinIO object key layout.
 */
@Component
public class AssignmentFilePolicy {

    /** Hard system ceiling; an assignment may configure a smaller per-file limit but never a larger one. */
    public static final long SYSTEM_MAX_FILE_BYTES = 104_857_600L; // 100MB
    public static final int MIN_FILE_COUNT = 1;
    public static final int MAX_FILE_COUNT = 10;

    private static final String BUCKET = "lms-uploads";

    private static final Logger log = LoggerFactory.getLogger(AssignmentFilePolicy.class);

    private static final Set<String> ATTACHMENT_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "csv", "txt", "md",
            "zip", "png", "jpg", "jpeg", "gif", "webp"
    );
    private static final Set<String> PREVIEWABLE_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "webp"
    );

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String bucket() {
        return BUCKET;
    }

    // --- Object keys ---

    public String attachmentKey(Integer courseId, Integer assignmentId, String originalName) {
        return buildKey("assignment/" + courseId + "/" + assignmentId + "/attachment", originalName);
    }

    public String rubricKey(Integer courseId, Integer assignmentId, String originalName) {
        return buildKey("assignment/" + courseId + "/" + assignmentId + "/rubric", originalName);
    }

    public String stagingKey(Integer courseId, Integer assignmentId, Integer ownerUserId, String originalName) {
        return buildKey("assignment/" + courseId + "/" + assignmentId + "/staging/" + ownerUserId, originalName);
    }

    public String annotatedKey(Integer courseId, Integer assignmentId, Integer studentUserId, String originalName) {
        return buildKey("assignment/" + courseId + "/" + assignmentId + "/annotated/" + studentUserId, originalName);
    }

    public String annotatedGroupKey(Integer courseId, Integer assignmentId, Integer groupId, String originalName) {
        return buildKey("assignment/" + courseId + "/" + assignmentId + "/annotated/groups/" + groupId, originalName);
    }

    private String buildKey(String prefix, String originalName) {
        String extension = extensionOf(originalName);
        String unique = UUID.randomUUID().toString().replace("-", "");
        return prefix + "/" + unique + (extension.isEmpty() ? "" : "." + extension);
    }

    // --- Constraint validation ---

    /**
     * Validates the per-assignment submission constraints chosen by the instructor.
     */
    public void validateFileConstraints(Integer maxFileCount, Long maxFileSizeBytes, List<String> allowedFileTypes) {
        if (maxFileCount == null || maxFileCount < MIN_FILE_COUNT || maxFileCount > MAX_FILE_COUNT) {
            throw new ApiException(ErrorType.ASSIGNMENT_FILE_CONSTRAINT_INVALID,
                    "maxFileCount must be between " + MIN_FILE_COUNT + " and " + MAX_FILE_COUNT);
        }
        if (maxFileSizeBytes == null || maxFileSizeBytes < 1L || maxFileSizeBytes > SYSTEM_MAX_FILE_BYTES) {
            throw new ApiException(ErrorType.ASSIGNMENT_FILE_CONSTRAINT_INVALID,
                    "maxFileSizeBytes must be between 1 and " + SYSTEM_MAX_FILE_BYTES);
        }
        if (allowedFileTypes == null || allowedFileTypes.isEmpty()) {
            throw new ApiException(ErrorType.ASSIGNMENT_FILE_CONSTRAINT_INVALID,
                    "allowedFileTypes must contain at least one extension");
        }
        for (String type : allowedFileTypes) {
            if (type == null || type.trim().isEmpty()) {
                throw new ApiException(ErrorType.ASSIGNMENT_FILE_CONSTRAINT_INVALID,
                        "allowedFileTypes must not contain blank entries");
            }
        }
    }

    /**
     * Lower-cases, strips leading dots, and de-duplicates the instructor-supplied extensions.
     */
    public List<String> normalizeAllowedTypes(List<String> allowedFileTypes) {
        Set<String> normalized = new LinkedHashSet<>();
        if (allowedFileTypes != null) {
            for (String type : allowedFileTypes) {
                if (type == null) {
                    continue;
                }
                String value = type.trim().toLowerCase(Locale.ROOT);
                if (value.startsWith(".")) {
                    value = value.substring(1);
                }
                if (!value.isEmpty()) {
                    normalized.add(value);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    public String toAllowedTypesJson(List<String> allowedFileTypes) {
        try {
            return objectMapper.writeValueAsString(normalizeAllowedTypes(allowedFileTypes));
        } catch (Exception e) {
            throw new ApiException(ErrorType.ASSIGNMENT_FILE_CONSTRAINT_INVALID, "allowedFileTypes is not serializable");
        }
    }

    /**
     * Parses the JSON array stored in {@code assignment.allowed_file_types}. A malformed value is
     * a data problem, not an empty allow-list, so it surfaces as an internal error.
     */
    public List<String> parseAllowedTypes(String allowedFileTypesJson) {
        if (allowedFileTypesJson == null || allowedFileTypesJson.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> parsed = objectMapper.readValue(allowedFileTypesJson, new TypeReference<List<String>>() {
            });
            return normalizeAllowedTypes(parsed);
        } catch (Exception e) {
            log.error("Failed to parse allowed_file_types JSON '{}': {}", allowedFileTypesJson, e.getMessage());
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Assignment file type configuration is corrupted");
        }
    }

    // --- Upload validation ---

    /**
     * Validates a student submission file against the assignment's own type and size limits.
     */
    public void validateSubmissionFile(MultipartFile file, List<String> allowedTypes, Long maxFileSizeBytes) {
        requireNonEmpty(file);
        long limit = maxFileSizeBytes == null ? SYSTEM_MAX_FILE_BYTES : Math.min(maxFileSizeBytes, SYSTEM_MAX_FILE_BYTES);
        if (file.getSize() > limit) {
            throw new ApiException(ErrorType.FILE_TOO_LARGE,
                    "File exceeds the maximum allowed size of " + limit + " bytes");
        }
        String extension = extensionOf(file.getOriginalFilename());
        if (allowedTypes != null && !allowedTypes.isEmpty() && !allowedTypes.contains(extension)) {
            throw new ApiException(ErrorType.UNSUPPORTED_FILE_TYPE,
                    "Allowed file types for this assignment: " + String.join(", ", allowedTypes));
        }
    }

    /**
     * Instructor-provided assignment attachments: system size ceiling plus a broad allow-list.
     * These are independent of the student submission {@code allowedFileTypes}.
     */
    public void validateAttachmentFile(MultipartFile file) {
        requireNonEmpty(file);
        requireWithinSystemLimit(file);
        String extension = extensionOf(file.getOriginalFilename());
        if (!ATTACHMENT_EXTENSIONS.contains(extension)) {
            throw new ApiException(ErrorType.UNSUPPORTED_FILE_TYPE,
                    "Attachment must be one of: " + String.join(", ", ATTACHMENT_EXTENSIONS));
        }
    }

    /**
     * Rubrics are PDF-only.
     */
    public void validateRubricPdf(MultipartFile file) {
        requireNonEmpty(file);
        requireWithinSystemLimit(file);
        String extension = extensionOf(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!"pdf".equals(extension) && !"application/pdf".equals(contentType)) {
            throw new ApiException(ErrorType.UNSUPPORTED_FILE_TYPE, "Rubric must be a PDF file");
        }
    }

    /**
     * Annotated feedback files returned to a student; PDF or image, within the system ceiling.
     */
    public void validateAnnotatedFile(MultipartFile file) {
        requireNonEmpty(file);
        requireWithinSystemLimit(file);
        String extension = extensionOf(file.getOriginalFilename());
        if (!PREVIEWABLE_EXTENSIONS.contains(extension) && !"docx".equals(extension)) {
            throw new ApiException(ErrorType.UNSUPPORTED_FILE_TYPE,
                    "Annotated file must be a PDF, DOCX, or image");
        }
    }

    // --- Helpers ---

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

    public boolean isPreviewable(String contentType, String filename) {
        if (PREVIEWABLE_EXTENSIONS.contains(extensionOf(filename))) {
            return true;
        }
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return type.equals("application/pdf") || type.startsWith("image/");
    }

    public String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        return filename.replace("\"", "").replace("\r", "").replace("\n", "");
    }

    public String checksumSha256(MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            StringBuilder hex = new StringBuilder(64);
            for (byte b : digest.digest()) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (Exception e) {
            log.error("Failed to compute SHA-256 for upload '{}': {}", file.getOriginalFilename(), e.getMessage());
            throw new ApiException(ErrorType.INTERNAL_ERROR, "Failed to compute file checksum");
        }
    }

    private void requireNonEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "File is required");
        }
    }

    private void requireWithinSystemLimit(MultipartFile file) {
        if (file.getSize() > SYSTEM_MAX_FILE_BYTES) {
            throw new ApiException(ErrorType.FILE_TOO_LARGE,
                    "File exceeds the maximum allowed size of " + SYSTEM_MAX_FILE_BYTES + " bytes");
        }
    }
}
