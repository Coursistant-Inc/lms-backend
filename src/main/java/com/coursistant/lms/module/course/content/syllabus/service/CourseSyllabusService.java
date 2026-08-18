package com.coursistant.lms.module.course.content.syllabus.service;

import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.syllabus.dto.SyllabusResponse;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabus;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabusVersion;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusMapper;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusVersionMapper;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.file.storage.FileSignature;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectNotFoundException;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.module.file.storage.S3UploadRollback;
import com.coursistant.lms.module.file.storage.SecureFileResponse;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class CourseSyllabusService {

    private static final Logger log = LoggerFactory.getLogger(CourseSyllabusService.class);

    private static final String OBJECT_PREFIX = "syllabus/";

    @Resource
    private CourseSyllabusMapper courseSyllabusMapper;

    @Resource
    private CourseSyllabusVersionMapper courseSyllabusVersionMapper;

    @Resource
    private CourseAuthorizationService courseAuthorizationService;

    @Resource
    private CourseContentAccessService courseContentAccessService;

    @Resource
    private CourseContentFilePolicy courseContentFilePolicy;

    @Resource
    private S3ObjectStorage s3ObjectStorage;

    @Resource
    private S3ObjectKeyResolver s3ObjectKeyResolver;

    @Resource
    private S3UploadRollback s3UploadRollback;

    public SyllabusResponse getSyllabus(ActorContext actor, Integer courseId) {
        courseAuthorizationService.requireVisibleCourse(actor, courseId);
        boolean managerView = courseAuthorizationService.isCourseManager(actor, courseId);
        return toResponse(courseSyllabusMapper.selectByCourseId(courseId), managerView);
    }

    public ResponseEntity<InputStreamResource> preview(ActorContext actor, Integer courseId) {
        return stream(actor, courseId, false);
    }

    public ResponseEntity<InputStreamResource> download(ActorContext actor, Integer courseId) {
        return stream(actor, courseId, true);
    }

    @Transactional
    public SyllabusResponse upload(ActorContext actor, Integer courseId, MultipartFile file) {
        courseContentAccessService.requireCourseManagerWritable(actor, courseId);
        String canonicalMime = courseContentFilePolicy.validateSyllabusPdf(file);

        String objectKey = OBJECT_PREFIX + courseId + "/" + UUID.randomUUID().toString().replace("-", "") + ".pdf";
        S3UploadRollback.Scope rollback = s3UploadRollback.open(courseId, null);
        try {
            try {
                s3ObjectStorage.putObject(physicalKey(objectKey), file, canonicalMime);
            } catch (S3StorageException e) {
                log.warn("Failed to upload syllabus for course {}: {}", courseId, e.getMessage());
                throw new ApiException(ErrorType.STORAGE_FAILURE, "Failed to upload syllabus file");
            }
            rollback.remember(courseContentFilePolicy.bucket(), objectKey);

            CourseSyllabusVersion version = new CourseSyllabusVersion();
            version.setCourseId(courseId);
            version.setObjectKey(objectKey);
            version.setOriginalFilename(resolveFilename(file));
            version.setContentType(canonicalMime);
            version.setSizeBytes(file.getSize());
            version.setUploadedBy(actor.getActorId());
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
        } catch (RuntimeException e) {
            rollback.abortIfNoTransaction();
            throw e;
        }
    }

    @Transactional
    public SyllabusResponse clear(ActorContext actor, Integer courseId) {
        courseContentAccessService.requireCourseManagerWritable(actor, courseId);

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
    public SyllabusResponse restorePrevious(ActorContext actor, Integer courseId) {
        courseContentAccessService.requireCourseManagerWritable(actor, courseId);

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

    private ResponseEntity<InputStreamResource> stream(ActorContext actor, Integer courseId, boolean attachment) {
        courseAuthorizationService.requireVisibleCourse(actor, courseId);

        CourseSyllabus syllabus = courseSyllabusMapper.selectByCourseId(courseId);
        if (syllabus == null || syllabus.getCurrentVersionId() == null) {
            throw new ApiException(ErrorType.SYLLABUS_NOT_FOUND);
        }
        CourseSyllabusVersion version = requireVersion(syllabus.getCurrentVersionId());

        try {
            S3ObjectPayload payload = s3ObjectStorage.getObject(physicalKey(version.getObjectKey()));
            return SecureFileResponse.from(
                    payload,
                    version.getOriginalFilename(),
                    courseContentFilePolicy.extensionOf(version.getOriginalFilename()),
                    attachment,
                    ErrorType.UNSUPPORTED_FILE_TYPE,
                    FileSignature.Kind.PDF);
        } catch (ApiException e) {
            throw e;
        } catch (S3ObjectNotFoundException e) {
            throw new ApiException(ErrorType.SYLLABUS_NOT_FOUND, "Failed to load syllabus file");
        } catch (S3StorageException e) {
            log.warn("Failed to load syllabus object for course {}: {}", courseId, e.getMessage());
            throw new ApiException(ErrorType.STORAGE_FAILURE, "Failed to load syllabus file");
        }
    }

    private String physicalKey(String objectKey) {
        return s3ObjectKeyResolver.resolve(courseContentFilePolicy.bucket(), objectKey);
    }

    private SyllabusResponse toResponse(CourseSyllabus syllabus, boolean managerView) {
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
        if (managerView) {
            response.setCanRestorePrevious(syllabus.getPreviousVersionId() != null);
        }
        return response;
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
}
