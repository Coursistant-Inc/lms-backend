package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.file.service.MinIOService;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;

/**
 * MinIO access for assignment objects. Storage failures surface as
 * {@link ErrorType#STORAGE_FAILURE}; they are never masked as "not found" or as an empty result.
 */
@Component
public class AssignmentStorageService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentStorageService.class);

    @Resource
    private MinIOService minIOService;

    @Resource
    private AssignmentFilePolicy assignmentFilePolicy;

    public void upload(String objectKey, MultipartFile file, Integer courseId, Integer assignmentId, Integer userId) {
        try {
            minIOService.uploadFile(objectKey, file, assignmentFilePolicy.bucket());
        } catch (Exception e) {
            log.error("Assignment object upload failed: courseId={}, assignmentId={}, userId={}, errorType={}, objectKey={}, cause={}",
                    courseId, assignmentId, userId, ErrorType.STORAGE_FAILURE, objectKey, e.getMessage());
            throw new ApiException(ErrorType.STORAGE_FAILURE, "Failed to store the uploaded file");
        }
    }

    public ResponseEntity<InputStreamResource> stream(String objectKey, String originalName, String contentType,
                                                      boolean attachment, Integer courseId, Integer assignmentId,
                                                      Integer userId) {
        try {
            InputStream stream = minIOService.downloadFile(objectKey, assignmentFilePolicy.bucket());
            String filename = assignmentFilePolicy.sanitizeFilename(originalName);
            String disposition = (attachment ? "attachment" : "inline")
                    + "; filename=\"" + filename + "\""
                    + "; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(resolveMediaType(contentType))
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
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
            minIOService.deleteFile(objectKey, assignmentFilePolicy.bucket());
        } catch (Exception e) {
            log.warn("Failed to delete assignment object {} (left as orphan): {}", objectKey, e.getMessage());
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
