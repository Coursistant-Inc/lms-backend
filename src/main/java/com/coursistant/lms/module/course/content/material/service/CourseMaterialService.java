package com.coursistant.lms.module.course.content.material.service;

import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.material.dto.MaterialResponse;
import com.coursistant.lms.module.course.content.material.dto.MoveMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.RenameMaterialRequest;
import com.coursistant.lms.module.course.content.material.dto.ReorderMaterialsRequest;
import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.material.repository.CourseMaterialMapper;
import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import com.coursistant.lms.module.file.service.MinIOService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseMaterialService {

    private static final Logger log = LoggerFactory.getLogger(CourseMaterialService.class);

    private static final String TYPE_FILE = "FILE";
    private static final String TYPE_LINK = "LINK";
    private static final int MAX_DISPLAY_NAME_LENGTH = 255;

    @Resource
    private CourseMaterialMapper courseMaterialMapper;

    @Resource
    private CourseContentAccessService courseContentAccessService;

    @Resource
    private CourseContentFilePolicy courseContentFilePolicy;

    @Resource
    private MinIOService minIOService;

    @Resource
    private MaterialResponseAssembler materialResponseAssembler;

    @Transactional
    public List<MaterialResponse> create(Integer courseId, Integer weekId, Integer userId,
                                          MultipartFile[] files, String linkUrl, String linkDisplayName) {
        courseContentAccessService.requireMaterialUpload(courseId, weekId, userId);

        boolean hasFiles = files != null && files.length > 0
                && java.util.Arrays.stream(files).anyMatch(f -> f != null && !f.isEmpty());
        boolean hasLink = linkUrl != null && !linkUrl.isBlank();
        if (!hasFiles && !hasLink) {
            throw new ApiException(ErrorType.PARAM_MISSING, "At least one file or a linkUrl is required");
        }

        int nextOrder = nextOrderPosition(weekId);
        List<CourseMaterial> created = new ArrayList<>();

        if (hasFiles) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) {
                    continue;
                }
                courseContentFilePolicy.validateFile(file);
                String originalFilename = file.getOriginalFilename();
                String extension = courseContentFilePolicy.extensionOf(originalFilename);
                String objectKey = courseContentFilePolicy.buildObjectKey(
                        "course-content/" + courseId + "/weeks/" + weekId + "/materials", originalFilename);

                try {
                    minIOService.uploadFile(objectKey, file, courseContentFilePolicy.bucket());
                } catch (Exception e) {
                    log.warn("Failed to upload course material to MinIO: key={}", objectKey, e);
                    throw new ApiException(ErrorType.INTERNAL_SERVER_ERROR, "Failed to upload file");
                }

                CourseMaterial material = new CourseMaterial();
                material.setWeekId(weekId);
                material.setCourseId(courseId);
                material.setMaterialType(TYPE_FILE);
                material.setDisplayName(trimToLength(baseName(originalFilename)));
                material.setOrderPosition(nextOrder++);
                material.setOriginalFilename(originalFilename);
                material.setContentType(file.getContentType());
                material.setExtension(extension);
                material.setSizeBytes(file.getSize());
                material.setObjectKey(objectKey);
                material.setUploadedBy(userId);
                courseMaterialMapper.insert(material);
                created.add(courseMaterialMapper.selectById(material.getId()));
            }
        }

        if (hasLink) {
            String normalizedUrl = validateAndNormalizeUrl(linkUrl);
            String displayName = linkDisplayName != null && !linkDisplayName.isBlank()
                    ? linkDisplayName.trim()
                    : normalizedUrl;

            CourseMaterial material = new CourseMaterial();
            material.setWeekId(weekId);
            material.setCourseId(courseId);
            material.setMaterialType(TYPE_LINK);
            material.setDisplayName(trimToLength(displayName));
            material.setOrderPosition(nextOrder);
            material.setLinkUrl(normalizedUrl);
            material.setUploadedBy(userId);
            courseMaterialMapper.insert(material);
            created.add(courseMaterialMapper.selectById(material.getId()));
        }

        return materialResponseAssembler.toResponses(created);
    }

    public MaterialResponse rename(Integer courseId, Integer weekId, Integer materialId, Integer userId,
                                    RenameMaterialRequest request) {
        courseContentAccessService.requireWeekWritable(courseId, weekId, userId);
        CourseMaterial material = requireMaterialInWeek(weekId, materialId);

        if (request == null || request.getDisplayName() == null || request.getDisplayName().isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "displayName is required");
        }
        String displayName = trimToLength(request.getDisplayName().trim());

        CourseMaterial patch = new CourseMaterial();
        patch.setId(materialId);
        patch.setDisplayName(displayName);
        courseMaterialMapper.updateById(patch);

        return materialResponseAssembler.toResponse(courseMaterialMapper.selectById(materialId));
    }

    @Transactional
    public List<MaterialResponse> reorder(Integer courseId, Integer weekId, Integer userId,
                                           ReorderMaterialsRequest request) {
        courseContentAccessService.requireWeekWritable(courseId, weekId, userId);
        if (request == null || request.getMaterialIds() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "materialIds is required");
        }

        List<CourseMaterial> existing = courseMaterialMapper.selectByWeekId(weekId);
        Set<Integer> existingIds = existing.stream().map(CourseMaterial::getId).collect(Collectors.toSet());
        Set<Integer> requestedIds = new HashSet<>(request.getMaterialIds());

        if (!existingIds.equals(requestedIds) || existingIds.size() != request.getMaterialIds().size()) {
            throw new ApiException(ErrorType.BAD_REQUEST, "materialIds must contain exactly the week's current materials");
        }

        int position = 0;
        for (Integer materialId : request.getMaterialIds()) {
            courseMaterialMapper.updateOrderPosition(materialId, position++);
        }

        return materialResponseAssembler.toResponses(courseMaterialMapper.selectByWeekId(weekId));
    }

    @Transactional
    public MaterialResponse move(Integer courseId, Integer weekId, Integer materialId, Integer userId,
                                  MoveMaterialRequest request) {
        courseContentAccessService.requireWeekWritable(courseId, weekId, userId);
        CourseMaterial material = requireMaterialInWeek(weekId, materialId);

        if (request == null || request.getTargetWeekId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "targetWeekId is required");
        }
        Integer targetWeekId = request.getTargetWeekId();

        if (targetWeekId.equals(weekId)) {
            return materialResponseAssembler.toResponse(material);
        }

        courseContentAccessService.requireWeekWritable(courseId, targetWeekId, userId);
        int newOrder = nextOrderPosition(targetWeekId);
        courseMaterialMapper.updateWeekId(materialId, targetWeekId, newOrder);

        return materialResponseAssembler.toResponse(courseMaterialMapper.selectById(materialId));
    }

    @Transactional
    public void delete(Integer courseId, Integer weekId, Integer materialId, Integer userId) {
        CourseMaterial material = requireMaterialInWeek(weekId, materialId);
        courseContentAccessService.requireMaterialDelete(courseId, weekId, userId, material);

        courseMaterialMapper.deleteById(materialId);

        if (TYPE_FILE.equals(material.getMaterialType()) && material.getObjectKey() != null) {
            try {
                minIOService.deleteFile(material.getObjectKey(), courseContentFilePolicy.bucket());
            } catch (Exception e) {
                log.warn("Failed to delete course material object from MinIO: key={}", material.getObjectKey(), e);
            }
        }
    }

    public ResponseEntity<InputStreamResource> preview(HttpServletRequest request, Integer courseId, Integer weekId,
                                                         Integer materialId, Integer userId) {
        CourseWeek week = courseContentAccessService.requireWeekReadable(request, courseId, weekId, userId);
        CourseMaterial material = requireMaterialInWeek(week.getId(), materialId);

        if (!TYPE_FILE.equals(material.getMaterialType())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Only file materials can be previewed");
        }
        if (!courseContentFilePolicy.isPreviewable(material.getContentType(), material.getExtension())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Material type is not previewable");
        }

        try {
            InputStream stream = minIOService.downloadFile(material.getObjectKey(), courseContentFilePolicy.bucket());
            MediaType mediaType = resolveMediaType(material.getContentType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeHeaderValue(material.getDisplayName()) + "\"")
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            log.warn("Failed to stream course material preview: key={}", material.getObjectKey(), e);
            throw new ApiException(ErrorType.INTERNAL_SERVER_ERROR, "Failed to load preview");
        }
    }

    public ResponseEntity<?> download(HttpServletRequest request, Integer courseId, Integer weekId,
                                       Integer materialId, Integer userId) {
        CourseWeek week = courseContentAccessService.requireWeekReadable(request, courseId, weekId, userId);
        CourseMaterial material = requireMaterialInWeek(week.getId(), materialId);

        if (TYPE_LINK.equals(material.getMaterialType())) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(material.getLinkUrl()))
                    .build();
        }

        try {
            InputStream stream = minIOService.downloadFile(material.getObjectKey(), courseContentFilePolicy.bucket());
            MediaType mediaType = resolveMediaType(material.getContentType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + sanitizeHeaderValue(material.getOriginalFilename()) + "\"")
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            log.warn("Failed to stream course material download: key={}", material.getObjectKey(), e);
            throw new ApiException(ErrorType.INTERNAL_SERVER_ERROR, "Failed to download file");
        }
    }

    private int nextOrderPosition(Integer weekId) {
        Integer max = courseMaterialMapper.selectMaxOrderPosition(weekId);
        return max == null ? 0 : max + 1;
    }

    private CourseMaterial requireMaterialInWeek(Integer weekId, Integer materialId) {
        if (materialId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Material id is required");
        }
        CourseMaterial material = courseMaterialMapper.selectById(materialId);
        if (material == null || !weekId.equals(material.getWeekId())) {
            throw new ApiException(ErrorType.MATERIAL_NOT_FOUND);
        }
        return material;
    }

    private String validateAndNormalizeUrl(String linkUrl) {
        String trimmed = linkUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new ApiException(ErrorType.BAD_REQUEST, "linkUrl must start with http:// or https://");
        }
        if (trimmed.length() > 2048) {
            throw new ApiException(ErrorType.BAD_REQUEST, "linkUrl is too long");
        }
        return trimmed;
    }

    private String trimToLength(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_DISPLAY_NAME_LENGTH ? value.substring(0, MAX_DISPLAY_NAME_LENGTH) : value;
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

    private String baseName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "Untitled file";
        }
        String normalized = filename.replace("\\", "/");
        int slash = normalized.lastIndexOf('/');
        String name = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return name.isBlank() ? "Untitled file" : name;
    }

    private String sanitizeHeaderValue(String value) {
        if (value == null || value.isBlank()) {
            return "file";
        }
        return value.replace("\"", "'");
    }
}
