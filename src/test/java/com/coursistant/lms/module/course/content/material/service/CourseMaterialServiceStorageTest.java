package com.coursistant.lms.module.course.content.material.service;

import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.module.course.content.CourseContentFilePolicy;
import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.material.repository.CourseMaterialMapper;
import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import com.coursistant.lms.module.course.course.service.CourseAuditService;
import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import com.coursistant.lms.module.course.storage.service.UploadOperationService;
import com.coursistant.lms.module.file.storage.FileDownloadHeaders;
import com.coursistant.lms.module.file.storage.FileSignatureSamples;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InOrder;

import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseMaterialServiceStorageTest {

    @Mock private CourseMaterialMapper courseMaterialMapper;
    @Mock private CourseContentAccessService courseContentAccessService;
    @Mock private CourseContentFilePolicy courseContentFilePolicy;
    @Mock private S3ObjectStorage s3ObjectStorage;
    @Spy private S3ObjectKeyResolver s3ObjectKeyResolver = new S3ObjectKeyResolver();
    @Mock private MaterialResponseAssembler materialResponseAssembler;
    @Mock private MinioOutboxService minioOutboxService;
    @Mock private UploadOperationService uploadOperationService;
    @Mock private CourseAuditService courseAuditService;

    @InjectMocks
    private CourseMaterialService service;

    private final ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 10, "USER", 1, "INSTRUCTOR", "ACTIVE");

    @BeforeEach
    void policy() {
        org.mockito.Mockito.lenient().when(courseContentFilePolicy.bucket()).thenReturn("lms-uploads");
        org.mockito.Mockito.lenient().when(courseContentFilePolicy.validateFile(any()))
                .thenReturn("application/pdf");
    }

    @Test
    void create_putUsesResolvedKey() {
        MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());
        when(courseContentFilePolicy.extensionOf("a.pdf")).thenReturn("pdf");
        when(courseContentFilePolicy.buildObjectKey(anyString(), eq("a.pdf"))).thenReturn("course-content/1/weeks/2/materials/abc.pdf");
        when(courseMaterialMapper.selectMaxOrderPosition(2)).thenReturn(0);
        stubInsert(7);
        when(courseMaterialMapper.selectById(7)).thenReturn(material(7, "course-content/1/weeks/2/materials/abc.pdf"));

        service.create(actor, 1, 2, new MockMultipartFile[]{file}, null, null, new MockHttpServletRequest());

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(s3ObjectStorage).putObject(key.capture(), eq(file), eq("application/pdf"));
        assertEquals("lms-uploads/course-content/1/weeks/2/materials/abc.pdf", key.getValue());
        verify(s3ObjectStorage, never()).copyObject(anyString(), anyString());
    }

    @Test
    void create_putFailure_is503() {
        MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());
        when(courseContentFilePolicy.extensionOf("a.pdf")).thenReturn("pdf");
        when(courseContentFilePolicy.buildObjectKey(anyString(), eq("a.pdf"))).thenReturn("k.pdf");
        when(courseMaterialMapper.selectMaxOrderPosition(2)).thenReturn(null);
        doThrow(new S3StorageException("timeout")).when(s3ObjectStorage)
                .putObject(anyString(), eq(file), anyString());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.create(actor, 1, 2, new MockMultipartFile[]{file}, null, null, new MockHttpServletRequest()));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void create_stagingCopyUsesSourceAndDestKeys() {
        MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", "idem-1");
        request.setAttribute("idem.fingerprint", "fp");
        com.coursistant.lms.module.course.storage.entity.UploadOperation op =
                new com.coursistant.lms.module.course.storage.entity.UploadOperation();
        op.setId("op-1");
        when(uploadOperationService.createOrResume(any(), eq("idem-1"), anyString(), eq("fp"), eq(1))).thenReturn(op);
        when(courseContentFilePolicy.extensionOf("a.pdf")).thenReturn("pdf");
        when(courseContentFilePolicy.buildObjectKey(eq("staging/op-1/materials"), eq("a.pdf")))
                .thenReturn("staging/op-1/materials/abc.pdf");
        when(courseContentFilePolicy.buildObjectKey(eq("course-content/1/weeks/2/materials"), eq("a.pdf")))
                .thenReturn("course-content/1/weeks/2/materials/abc.pdf");
        when(courseMaterialMapper.selectMaxOrderPosition(2)).thenReturn(0);
        stubInsert(7);
        when(courseMaterialMapper.selectById(7)).thenReturn(material(7, "course-content/1/weeks/2/materials/abc.pdf"));

        service.create(actor, 1, 2, new MockMultipartFile[]{file}, null, null, request);

        InOrder inOrder = inOrder(s3ObjectStorage, minioOutboxService);
        inOrder.verify(s3ObjectStorage).putObject(eq("lms-uploads/staging/op-1/materials/abc.pdf"), eq(file),
                eq("application/pdf"));
        inOrder.verify(s3ObjectStorage).copyObject(
                "lms-uploads/staging/op-1/materials/abc.pdf",
                "lms-uploads/course-content/1/weeks/2/materials/abc.pdf");
        inOrder.verify(minioOutboxService).enqueueDelete("lms-uploads", "staging/op-1/materials/abc.pdf", 1, "op-1");
    }

    @Test
    void create_copyFailure_is503AndAbortsStaging() {
        MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", "x".getBytes());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Idempotency-Key", "idem-1");
        request.setAttribute("idem.fingerprint", "fp");
        com.coursistant.lms.module.course.storage.entity.UploadOperation op =
                new com.coursistant.lms.module.course.storage.entity.UploadOperation();
        op.setId("op-1");
        when(uploadOperationService.createOrResume(any(), anyString(), anyString(), anyString(), anyInt())).thenReturn(op);
        when(courseContentFilePolicy.extensionOf("a.pdf")).thenReturn("pdf");
        when(courseContentFilePolicy.buildObjectKey(eq("staging/op-1/materials"), eq("a.pdf")))
                .thenReturn("staging/src.pdf");
        when(courseContentFilePolicy.buildObjectKey(eq("course-content/1/weeks/2/materials"), eq("a.pdf")))
                .thenReturn("final.pdf");
        when(courseMaterialMapper.selectMaxOrderPosition(2)).thenReturn(0);
        doThrow(new S3StorageException("NoSuchKey")).when(s3ObjectStorage)
                .copyObject("lms-uploads/staging/src.pdf", "lms-uploads/final.pdf");

        ApiException ex = assertThrows(ApiException.class,
                () -> service.create(actor, 1, 2, new MockMultipartFile[]{file}, null, null, request));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        verify(minioOutboxService).enqueueAbortStagingIndependent("lms-uploads", "staging/src.pdf", 1, "op-1");
    }

    @Test
    void preview_missingObject_is404() {
        CourseWeek week = week();
        when(courseContentAccessService.requireWeekReadable(actor, 1, 2)).thenReturn(week);
        when(courseMaterialMapper.selectById(9)).thenReturn(material(9, "k.pdf"));
        when(courseContentFilePolicy.isPreviewable("application/pdf", "pdf")).thenReturn(true);
        when(s3ObjectStorage.getObject("lms-uploads/k.pdf")).thenThrow(new S3ObjectNotFoundException("k.pdf"));

        ApiException ex = assertThrows(ApiException.class, () -> service.preview(actor, 1, 2, 9));
        assertEquals(ErrorType.NOT_FOUND, ex.getErrorType());
        assertEquals(HttpStatus.NOT_FOUND, ex.getErrorType().getHttpStatus());
    }

    @Test
    void download_forbidden_is503() {
        CourseWeek week = week();
        when(courseContentAccessService.requireWeekReadable(actor, 1, 2)).thenReturn(week);
        when(courseMaterialMapper.selectById(9)).thenReturn(material(9, "k.pdf"));
        when(s3ObjectStorage.getObject("lms-uploads/k.pdf")).thenThrow(new S3StorageException("403"));

        ApiException ex = assertThrows(ApiException.class, () -> service.download(actor, 1, 2, 9));
        assertEquals(ErrorType.STORAGE_FAILURE, ex.getErrorType());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getErrorType().getHttpStatus());
    }

    @Test
    void download_setsContentLength() throws Exception {
        CourseWeek week = week();
        when(courseContentAccessService.requireWeekReadable(actor, 1, 2)).thenReturn(week);
        when(courseMaterialMapper.selectById(9)).thenReturn(material(9, "k.pdf"));
        byte[] body = FileSignatureSamples.PDF;
        when(s3ObjectStorage.getObject("lms-uploads/k.pdf")).thenReturn(
                new S3ObjectPayload(new ByteArrayInputStream(body),
                        new S3ObjectMetadata("application/pdf", (long) body.length, "e")));

        ResponseEntity<?> response = service.download(actor, 1, 2, 9);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(body.length, response.getHeaders().getContentLength());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment"));
        assertSecurityHeaders(response);
        InputStreamResource resource = (InputStreamResource) response.getBody();
        assertEquals(new String(body), new String(resource.getInputStream().readAllBytes()));
    }

    @Test
    void xssU2_create_persistsCanonicalPngNotClientHtml() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "test.png", "text/html", FileSignatureSamples.PNG);
        when(courseContentFilePolicy.validateFile(file)).thenReturn("image/png");
        when(courseContentFilePolicy.extensionOf("test.png")).thenReturn("png");
        when(courseContentFilePolicy.buildObjectKey(anyString(), eq("test.png")))
                .thenReturn("course-content/1/weeks/2/materials/abc.png");
        when(courseMaterialMapper.selectMaxOrderPosition(2)).thenReturn(0);
        stubInsert(7);
        when(courseMaterialMapper.selectById(7)).thenReturn(material(7, "course-content/1/weeks/2/materials/abc.png"));

        service.create(actor, 1, 2, new MockMultipartFile[]{file}, null, null, new MockHttpServletRequest());

        ArgumentCaptor<CourseMaterial> inserted = ArgumentCaptor.forClass(CourseMaterial.class);
        verify(courseMaterialMapper).insert(inserted.capture());
        assertEquals("image/png", inserted.getValue().getContentType());
        verify(s3ObjectStorage).putObject(anyString(), eq(file), eq("image/png"));
    }

    @Test
    void xssP1_htmlStoredAsPng_previewIs400AndClosesStream() throws Exception {
        CourseWeek week = week();
        when(courseContentAccessService.requireWeekReadable(actor, 1, 2)).thenReturn(week);
        CourseMaterial material = material(9, "poison.png");
        material.setContentType("text/html");
        material.setExtension("png");
        material.setDisplayName("test.png");
        when(courseMaterialMapper.selectById(9)).thenReturn(material);
        when(courseContentFilePolicy.isPreviewable("text/html", "png")).thenReturn(true);
        AtomicBoolean closed = new AtomicBoolean(false);
        InputStream tracking = new FilterInputStream(new ByteArrayInputStream(FileSignatureSamples.HTML)) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        when(s3ObjectStorage.getObject("lms-uploads/poison.png")).thenReturn(
                new S3ObjectPayload(tracking, new S3ObjectMetadata("text/html",
                        (long) FileSignatureSamples.HTML.length, "e")));

        ApiException ex = assertThrows(ApiException.class, () -> service.preview(actor, 1, 2, 9));
        assertEquals(ErrorType.BAD_REQUEST, ex.getErrorType());
        assertTrue(closed.get());
    }

    @Test
    void xssP1b_staleHtmlMimeButRealPng_previewsAsPng() throws Exception {
        CourseWeek week = week();
        when(courseContentAccessService.requireWeekReadable(actor, 1, 2)).thenReturn(week);
        CourseMaterial material = material(9, "k.png");
        material.setContentType("text/html");
        material.setExtension("png");
        material.setDisplayName("test.png");
        when(courseMaterialMapper.selectById(9)).thenReturn(material);
        when(courseContentFilePolicy.isPreviewable("text/html", "png")).thenReturn(true);
        when(s3ObjectStorage.getObject("lms-uploads/k.png")).thenReturn(
                new S3ObjectPayload(new ByteArrayInputStream(FileSignatureSamples.PNG),
                        new S3ObjectMetadata("text/html", (long) FileSignatureSamples.PNG.length, "e")));

        ResponseEntity<InputStreamResource> response = service.preview(actor, 1, 2, 9);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("image/png", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
        assertFalse("text/html".equalsIgnoreCase(String.valueOf(response.getHeaders().getContentType())));
        assertSecurityHeaders(response);
        assertEquals(FileSignatureSamples.PNG.length, response.getBody().getInputStream().readAllBytes().length);
    }

    @Test
    void xssP2_pdfPreview_hasInlineCanonicalMimeAndCsp() throws Exception {
        CourseWeek week = week();
        when(courseContentAccessService.requireWeekReadable(actor, 1, 2)).thenReturn(week);
        when(courseMaterialMapper.selectById(9)).thenReturn(material(9, "k.pdf"));
        when(courseContentFilePolicy.isPreviewable("application/pdf", "pdf")).thenReturn(true);
        when(s3ObjectStorage.getObject("lms-uploads/k.pdf")).thenReturn(
                new S3ObjectPayload(new ByteArrayInputStream(FileSignatureSamples.PDF),
                        new S3ObjectMetadata("application/pdf", (long) FileSignatureSamples.PDF.length, "e")));

        ResponseEntity<InputStreamResource> response = service.preview(actor, 1, 2, 9);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
        assertSecurityHeaders(response);
    }

    private static void assertSecurityHeaders(ResponseEntity<?> response) {
        assertEquals("nosniff", response.getHeaders().getFirst("X-Content-Type-Options"));
        String csp = response.getHeaders().getFirst(FileDownloadHeaders.CONTENT_SECURITY_POLICY_HEADER);
        assertTrue(csp != null && csp.contains("sandbox") && csp.contains("default-src 'none'"));
        assertEquals(FileDownloadHeaders.CONTENT_SECURITY_POLICY, csp);
    }

    private void stubInsert(int id) {
        doAnswer(invocation -> {
            CourseMaterial material = invocation.getArgument(0);
            material.setId(id);
            return 1;
        }).when(courseMaterialMapper).insert(any());
    }

    private CourseWeek week() {
        CourseWeek week = new CourseWeek();
        week.setId(2);
        week.setCourseId(1);
        week.setTitle("W1");
        return week;
    }

    private CourseMaterial material(Integer id, String key) {
        CourseMaterial material = new CourseMaterial();
        material.setId(id);
        material.setWeekId(2);
        material.setCourseId(1);
        material.setMaterialType("FILE");
        material.setObjectKey(key);
        material.setContentType("application/pdf");
        material.setExtension("pdf");
        material.setOriginalFilename("a.pdf");
        material.setDisplayName("a.pdf");
        return material;
    }
}
