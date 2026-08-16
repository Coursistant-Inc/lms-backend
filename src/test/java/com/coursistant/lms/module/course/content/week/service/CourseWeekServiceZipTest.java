package com.coursistant.lms.module.course.content.week.service;

import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.material.repository.CourseMaterialMapper;
import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import com.coursistant.lms.module.course.course.service.CourseAuditService;
import com.coursistant.lms.module.file.storage.S3ObjectKeyResolver;
import com.coursistant.lms.module.file.storage.S3ObjectMetadata;
import com.coursistant.lms.module.file.storage.S3ObjectPayload;
import com.coursistant.lms.module.file.storage.S3ObjectStorage;
import com.coursistant.lms.module.file.storage.S3StorageException;
import com.coursistant.lms.shared.security.ActorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseWeekServiceZipTest {

    @Mock private com.coursistant.lms.module.course.content.week.repository.CourseWeekMapper courseWeekMapper;
    @Mock private CourseMaterialMapper courseMaterialMapper;
    @Mock private CourseContentAccessService courseContentAccessService;
    @Mock private CourseContentFilePolicy courseContentFilePolicy;
    @Mock private S3ObjectStorage s3ObjectStorage;
    @Spy private S3ObjectKeyResolver s3ObjectKeyResolver = new S3ObjectKeyResolver();
    @Mock private com.coursistant.lms.module.course.content.material.service.MaterialResponseAssembler materialResponseAssembler;
    @Mock private CourseAuditService courseAuditService;

    @InjectMocks
    private CourseWeekService service;

    private final ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 10, "USER", 1, "STUDENT", "ACTIVE");

    @Test
    void downloadZip_skipsFailedFileAndClosesSuccessfulPayload() throws Exception {
        CourseWeek week = new CourseWeek();
        week.setId(2);
        week.setTitle("Week 1");
        when(courseContentAccessService.requireWeekReadable(actor, 1, 2)).thenReturn(week);
        when(courseContentFilePolicy.bucket()).thenReturn("lms-uploads");

        CourseMaterial ok = material(1, "ok.pdf");
        CourseMaterial bad = material(2, "bad.pdf");
        when(courseMaterialMapper.selectByWeekIdAndType(2, "FILE")).thenReturn(List.of(ok, bad));

        AtomicInteger closes = new AtomicInteger();
        byte[] body = "hello".getBytes();
        InputStream counting = new FilterInputStream(new ByteArrayInputStream(body)) {
            @Override
            public void close() throws java.io.IOException {
                closes.incrementAndGet();
                super.close();
            }
        };
        S3ObjectPayload payload = new S3ObjectPayload(
                counting, new S3ObjectMetadata("application/pdf", 5L, "e"));
        when(s3ObjectStorage.getObject("lms-uploads/ok.pdf")).thenReturn(payload);
        when(s3ObjectStorage.getObject("lms-uploads/bad.pdf")).thenThrow(new S3StorageException("403"));

        ResponseEntity<StreamingResponseBody> response = service.downloadZip(actor, 1, 2);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        response.getBody().writeTo(out);

        assertEquals(1, closes.get());
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals("ok.pdf", zip.getNextEntry().getName());
            assertEquals("bad.pdf", zip.getNextEntry().getName());
            assertTrue(zip.getNextEntry() == null);
        }
    }

    private CourseMaterial material(Integer id, String name) {
        CourseMaterial material = new CourseMaterial();
        material.setId(id);
        material.setObjectKey(name);
        material.setOriginalFilename(name);
        material.setDisplayName(name);
        return material;
    }
}
