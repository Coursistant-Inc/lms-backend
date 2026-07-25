package com.coursistant.lms.module.course.content.syllabus.service;

import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.syllabus.dto.SyllabusResponse;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabus;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabusVersion;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusMapper;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusVersionMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
public class CourseSyllabusService {

    private static final Logger log = LoggerFactory.getLogger(CourseSyllabusService.class);

    private static final String OBJECT_PREFIX = "syllabus/";
    private static final String STATE_ARCHIVED = "Archived";

    @Resource
    private CourseSyllabusMapper courseSyllabusMapper;

    @Resource
    private CourseSyllabusVersionMapper courseSyllabusVersionMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CoursePermissionService coursePermissionService;

    @Resource
    private CourseContentFilePolicy courseContentFilePolicy;

    @Resource
    private MinIOService minIOService;

    public SyllabusResponse getSyllabus(Integer courseId, Integer userId, boolean admin) {
        requireCourse(courseId);
        if (!admin) {
            coursePermissionService.requireActiveEnrollment(courseId, userId);
        }
        boolean instructorView = admin || coursePermissionService.isInstructor(courseId, userId);
        return toResponse(courseSyllabusMapper.selectByCourseId(courseId), instructorView);
    }

    public ResponseEntity<InputStreamResource> preview(Integer courseId, Integer userId, boolean admin) {
        return stream(courseId, userId, admin, false);
    }

    public ResponseEntity<InputStreamResource> download(Integer courseId, Integer userId, boolean admin) {
        return stream(courseId, userId, admin, true);
    }

    @Transactional
    public SyllabusResponse upload(Integer courseId, Integer userId, MultipartFile file) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireInstructor(courseId, userId);
        requireNotArchived(course);
        courseContentFilePolicy.validateSyllabusPdf(file);

        String objectKey = OBJECT_PREFIX + courseId + "/" + UUID.randomUUID().toString().replace("-", "") + ".pdf";
        try {
            minIOService.uploadFile(objectKey, file, courseContentFilePolicy.bucket());
        } catch (Exception e) {
            log.warn("Failed to upload syllabus for course {}: {}", courseId, e.getMessage());
            throw new ApiException(ErrorType.INTERNAL_SERVER_ERROR, "Failed to upload syllabus file");
        }

        CourseSyllabusVersion version = new CourseSyllabusVersion();
        version.setCourseId(courseId);
        version.setObjectKey(objectKey);
        version.setOriginalFilename(resolveFilename(file));
        version.setContentType(resolveContentType(file));
        version.setSizeBytes(file.getSize());
        version.setUploadedBy(userId);
        courseSyllabusVersionMapper.insert(version);

        CourseSyllabus existing = courseSyllabusMapper.selectByCourseId(courseId);
        if (existing == null) {
            CourseSyllabus newRow = new CourseSyllabus();
            newRow.setCourseId(courseId);
            newRow.setCurrentVersionId(version.getId());
            newRow.setPreviousVersionId(null);
            courseSyllabusMapper.insert(newRow);
        } else {
            CourseSyllabus patch = new CourseSyllabus();
            patch.setCourseId(courseId);
            patch.setCurrentVersionId(version.getId());
            patch.setPreviousVersionId(existing.getCurrentVersionId());
            courseSyllabusMapper.updateVersions(patch);
        }

        return toResponse(courseSyllabusMapper.selectByCourseId(courseId), true);
    }

    @Transactional
    public SyllabusResponse clear(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireInstructor(courseId, userId);
        requireNotArchived(course);

        CourseSyllabus existing = courseSyllabusMapper.selectByCourseId(courseId);
        if (existing == null || (existing.getCurrentVersionId() == null && existing.getPreviousVersionId() == null)) {
            return toResponse(existing, true);
        }
        CourseSyllabus patch = new CourseSyllabus();
        patch.setCourseId(courseId);
        patch.setCurrentVersionId(null);
        patch.setPreviousVersionId(null);
        courseSyllabusMapper.updateVersions(patch);
        return toResponse(courseSyllabusMapper.selectByCourseId(courseId), true);
    }

    @Transactional
    public SyllabusResponse restorePrevious(Integer courseId, Integer userId) {
        Course course = requireCourse(courseId);
        coursePermissionService.requireInstructor(courseId, userId);
        requireNotArchived(course);

        CourseSyllabus syllabus = courseSyllabusMapper.selectByCourseId(courseId);
        if (syllabus == null || syllabus.getCurrentVersionId() == null) {
            throw new ApiException(ErrorType.SYLLABUS_NOT_FOUND);
        }
        if (syllabus.getPreviousVersionId() == null) {
            throw new ApiException(ErrorType.NO_PREVIOUS_SYLLABUS_VERSION);
        }

        CourseSyllabus patch = new CourseSyllabus();
        patch.setCourseId(courseId);
        patch.setCurrentVersionId(syllabus.getPreviousVersionId());
        patch.setPreviousVersionId(syllabus.getCurrentVersionId());
        courseSyllabusMapper.updateVersions(patch);

        return toResponse(courseSyllabusMapper.selectByCourseId(courseId), true);
    }

    private ResponseEntity<InputStreamResource> stream(Integer courseId, Integer userId, boolean admin, boolean attachment) {
        requireCourse(courseId);
        if (!admin) {
            coursePermissionService.requireActiveEnrollment(courseId, userId);
        }

        CourseSyllabus syllabus = courseSyllabusMapper.selectByCourseId(courseId);
        if (syllabus == null || syllabus.getCurrentVersionId() == null) {
            throw new ApiException(ErrorType.SYLLABUS_NOT_FOUND);
        }
        CourseSyllabusVersion version = requireVersion(syllabus.getCurrentVersionId());

        try {
            InputStream stream = minIOService.downloadFile(version.getObjectKey(), courseContentFilePolicy.bucket());
            MediaType mediaType = resolveMediaType(version.getContentType());
            String disposition = (attachment ? "attachment" : "inline")
                    + "; filename=\"" + sanitizeFilename(version.getOriginalFilename()) + "\"";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(mediaType)
                    .body(new InputStreamResource(stream));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to load syllabus object for course {}: {}", courseId, e.getMessage());
            throw new ApiException(ErrorType.SYLLABUS_NOT_FOUND, "Failed to load syllabus file");
        }
    }

    private SyllabusResponse toResponse(CourseSyllabus syllabus, boolean instructorView) {
        SyllabusResponse response = new SyllabusResponse();
        if (syllabus == null || syllabus.getCurrentVersionId() == null) {
            response.setPosted(false);
            return response;
        }

        CourseSyllabusVersion version = requireVersion(syllabus.getCurrentVersionId());
        response.setPosted(true);
        response.setVersionId(version.getId());
        response.setOriginalFilename(version.getOriginalFilename());
        response.setContentType(version.getContentType());
        response.setSizeBytes(version.getSizeBytes());
        response.setUploadedBy(version.getUploadedBy());
        response.setUploadedAt(version.getCreatedAt());
        if (instructorView) {
            response.setCanRestorePrevious(syllabus.getPreviousVersionId() != null);
        }
        return response;
    }

    private Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Course id is required");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return course;
    }

    private void requireNotArchived(Course course) {
        if (STATE_ARCHIVED.equals(course.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
    }

    private CourseSyllabusVersion requireVersion(Integer versionId) {
        CourseSyllabusVersion version = courseSyllabusVersionMapper.selectById(versionId);
        if (version == null) {
            throw new ApiException(ErrorType.SYLLABUS_NOT_FOUND);
        }
        return version;
    }

    private String resolveFilename(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return (filename == null || filename.isBlank()) ? "syllabus.pdf" : filename;
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return (contentType == null || contentType.isBlank()) ? MediaType.APPLICATION_PDF_VALUE : contentType;
    }

    private MediaType resolveMediaType(String contentType) {
        try {
            return contentType == null ? MediaType.APPLICATION_PDF : MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            return MediaType.APPLICATION_PDF;
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "syllabus.pdf";
        }
        return filename.replace("\"", "");
    }
}
