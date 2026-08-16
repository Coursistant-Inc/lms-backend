package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.file.storage.S3DownloadBody;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * S3 access for assignment objects. Storage failures surface as
 * {@link ErrorType#STORAGE_FAILURE}; they are never masked as "not found" or as an empty result.
 */
@Component
public class AssignmentStorageService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentStorageService.class);

    @Resource
    private S3ObjectStorage s3ObjectStorage;

    @Resource
    private S3ObjectKeyResolver s3ObjectKeyResolver;

    @Resource
    private AssignmentFilePolicy assignmentFilePolicy;

    public void upload(String objectKey, MultipartFile file, Integer courseId, Integer assignmentId, Integer userId) {
        try {
            s3ObjectStorage.putObject(physicalKey(objectKey), file);
        } catch (S3StorageException e) {
            log.error("Assignment object upload failed: courseId={}, assignmentId={}, userId={}, errorType={}, objectKey={}, cause={}",
                    courseId, assignmentId, userId, ErrorType.STORAGE_FAILURE, objectKey, e.getMessage());
            throw new ApiException(ErrorType.STORAGE_FAILURE, "Failed to store the uploaded file");
        }
    }

    public ResponseEntity<InputStreamResource> stream(String objectKey, String originalName, String contentType,
                                                      boolean attachment, Integer courseId, Integer assignmentId,
                                                      Integer userId) {
        try {
            S3ObjectPayload payload = s3ObjectStorage.getObject(physicalKey(objectKey));
            String filename = assignmentFilePolicy.sanitizeFilename(originalName);
            String disposition = (attachment ? "attachment" : "inline")
                    + "; filename=\"" + filename + "\""
                    + "; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            MediaType mediaType = resolveMediaType(contentType, payload);
            long length = S3DownloadBody.contentLength(payload);
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(mediaType);
            if (length >= 0) {
                builder.contentLength(length);
            }
            return builder.body(S3DownloadBody.resource(payload));
        } catch (S3StorageException e) {
            log.error("Assignment object download failed: courseId={}, assignmentId={}, userId={}, errorType={}, objectKey={}, cause={}",
                    courseId, assignmentId, userId, ErrorType.STORAGE_FAILURE, objectKey, e.getMessage());
            throw new ApiException(ErrorType.STORAGE_FAILURE, "Failed to load the requested file");
        }
    }

    /**
     * Deletes an orphaned object. Used after the database row is already gone, where a storage
     * failure must not roll the caller back — it only leaves a harmless orphan for the reaper.
     */
    public void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            s3ObjectStorage.deleteObject(physicalKey(objectKey));
        } catch (S3StorageException e) {
            log.warn("Failed to delete assignment object {} (left as orphan): {}", objectKey, e.getMessage());
        }
    }

    private String physicalKey(String objectKey) {
        return s3ObjectKeyResolver.resolve(assignmentFilePolicy.bucket(), objectKey);
    }

    private MediaType resolveMediaType(String contentType, S3ObjectPayload payload) {
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (Exception e) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        String fromStorage = payload.metadata() != null ? payload.metadata().contentType() : null;
        if (fromStorage == null || fromStorage.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(fromStorage);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
