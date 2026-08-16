package com.coursistant.lms.module.course.content.syllabus.service;

import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabus;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabusVersion;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusMapper;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusVersionMapper;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectMetadata;
import com.coursistant.lms.module.file.storage.S3ObjectNotFoundException;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSyllabusServiceStorageTest {

    @Mock private CourseSyllabusMapper courseSyllabusMapper;
    @Mock private CourseSyllabusVersionMapper courseSyllabusVersionMapper;
    @Mock private CourseAuthorizationService courseAuthorizationService;
    @Mock private CourseContentAccessService courseContentAccessService;
    @Mock private CourseContentFilePolicy courseContentFilePolicy;
    @Mock private S3ObjectStorage s3ObjectStorage;
    @Spy private S3ObjectKeyResolver s3ObjectKeyResolver = new S3ObjectKeyResolver();

    @InjectMocks
    private CourseSyllabusService service;

    private final ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 10, "USER", 1, "INSTRUCTOR", "ACTIVE");

    @BeforeEach
    void policy() {
        org.mockito.Mockito.lenient().when(courseContentFilePolicy.bucket()).thenReturn("lms-uploads");
    }

    @Test
    void upload_putFailure_is503() {
        MockMultipartFile file = new MockMultipartFile("file", "s.pdf", "application/pdf", "x".getBytes());
        doThrow(new S3StorageException("timeout")).when(s3ObjectStorage).putObject(anyString(), any());

        ApiException ex = assertThrows(ApiException.class, () -> service.upload(actor, 8, file));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void upload_putsResolvedKey() {
        MockMultipartFile file = new MockMultipartFile("file", "s.pdf", "application/pdf", "x".getBytes());
        when(courseSyllabusMapper.selectByCourseId(8)).thenReturn(null);

        service.upload(actor, 8, file);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).putObject(key.capture(), any());
        org.junit.jupiter.api.Assertions.assertTrue(key.getValue().startsWith("lms-uploads/syllabus/8/"));
    }

    @Test
    void download_dbMissing_isSyllabusNotFound() {
        when(courseSyllabusMapper.selectByCourseId(8)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> service.download(actor, 8));
        assertEquals(ErrorType.SYLLABUS_NOT_FOUND, ex.getErrorType());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorType().getHttpStatus());
    }

    @Test
    void download_s3Missing_isSyllabusNotFound() {
        CourseSyllabus syllabus = new CourseSyllabus();
        syllabus.setCourseId(8);
        syllabus.setCurrentVersionId(3);
        CourseSyllabusVersion version = new CourseSyllabusVersion();
        version.setId(3);
        version.setObjectKey("syllabus/8/a.pdf");
        version.setOriginalFilename("a.pdf");
        version.setContentType("application/pdf");
        when(courseSyllabusMapper.selectByCourseId(8)).thenReturn(syllabus);
        when(courseSyllabusVersionMapper.selectById(3)).thenReturn(version);
        when(s3ObjectStorage.getObject("lms-uploads/syllabus/8/a.pdf"))
                .thenThrow(new S3ObjectNotFoundException("missing"));

        ApiException ex = assertThrows(ApiException.class, () -> service.download(actor, 8));
        assertEquals(ErrorType.SYLLABUS_NOT_FOUND, ex.getErrorType());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorType().getHttpStatus());
    }

    @Test
    void download_forbidden_is503NotSyllabusNotFound() {
        CourseSyllabus syllabus = new CourseSyllabus();
        syllabus.setCourseId(8);
        syllabus.setCurrentVersionId(3);
        CourseSyllabusVersion version = new CourseSyllabusVersion();
        version.setId(3);
        version.setObjectKey("syllabus/8/a.pdf");
        version.setOriginalFilename("a.pdf");
        version.setContentType("application/pdf");
        when(courseSyllabusMapper.selectByCourseId(8)).thenReturn(syllabus);
        when(courseSyllabusVersionMapper.selectById(3)).thenReturn(version);
        when(s3ObjectStorage.getObject("lms-uploads/syllabus/8/a.pdf"))
                .thenThrow(new S3StorageException("403"));

        ApiException ex = assertThrows(ApiException.class, () -> service.download(actor, 8));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void download_setsContentLength() throws Exception {
        CourseSyllabus syllabus = new CourseSyllabus();
        syllabus.setCourseId(8);
        syllabus.setCurrentVersionId(3);
        CourseSyllabusVersion version = new CourseSyllabusVersion();
        version.setId(3);
        version.setObjectKey("syllabus/8/a.pdf");
        version.setOriginalFilename("a.pdf");
        version.setContentType("application/pdf");
        when(courseSyllabusMapper.selectByCourseId(8)).thenReturn(syllabus);
        when(courseSyllabusVersionMapper.selectById(3)).thenReturn(version);
        byte[] body = "pdf".getBytes();
        when(s3ObjectStorage.getObject("lms-uploads/syllabus/8/a.pdf")).thenReturn(
                new S3ObjectPayload(new ByteArrayInputStream(body), new S3ObjectMetadata("application/pdf", 3L, "e")));

        ResponseEntity<InputStreamResource> response = service.download(actor, 8);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(3L, response.getHeaders().getContentLength());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
    }
}
