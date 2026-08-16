package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.module.assignment.entity.AssignmentAttachment;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionFile;
import com.coursistant.lms.module.assignment.entity.AssignmentSubmissionStagingFile;
import com.coursistant.lms.module.assignment.repository.AssignmentAttachmentMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionFileMapper;
import com.coursistant.lms.module.assignment.repository.AssignmentSubmissionStagingFileMapper;
import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.material.repository.CourseMaterialMapper;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabus;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabusVersion;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusMapper;
import com.coursistant.lms.module.course.content.syllabus.repository.CourseSyllabusVersionMapper;
import com.coursistant.lms.module.course.storage.service.MinioOutboxService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_S3_LIVE", matches = "true")
class S3BusinessLiveTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String INSTRUCTOR_EMAIL = "teachtest1@example.com";
    private static final String STUDENT_EMAIL = "regtest1@example.com";
    private static final String OUTSIDER_EMAIL = "regtest2@example.com";
    private static final String PASSWORD = "Test12345";

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CourseMaterialMapper courseMaterialMapper;
    @Autowired
    private AssignmentAttachmentMapper assignmentAttachmentMapper;
    @Autowired
    private AssignmentSubmissionStagingFileMapper stagingFileMapper;
    @Autowired
    private AssignmentSubmissionFileMapper submissionFileMapper;
    @Autowired
    private CourseSyllabusMapper courseSyllabusMapper;
    @Autowired
    private CourseSyllabusVersionMapper courseSyllabusVersionMapper;
    @Autowired
    private S3ObjectStorage s3ObjectStorage;
    @Autowired
    private S3ObjectKeyResolver s3ObjectKeyResolver;
    @Autowired
    private MinioOutboxService minioOutboxService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void avatarAssignmentMaterialsSyllabusZipAndOutbox_roundTrip() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Set<String> physicalKeys = new LinkedHashSet<>();
        Integer courseId = null;
        Integer weekId = null;
        Integer assignmentId = null;
        Integer studentId = null;
        String previousAvatar = null;
        boolean originalAvatarExisted = false;
        S3ObjectMetadata originalAvatarMeta = null;
        String originalAvatarPhysical = null;
        String adminToken = null;

        Throwable testFailure = null;
        try {
            adminToken = login(ADMIN_EMAIL, "ADMIN");
            String studentToken = login(STUDENT_EMAIL, "USER");
            String outsiderToken = login(OUTSIDER_EMAIL, "USER");

            User instructor = userMapper.selectByEmail(INSTRUCTOR_EMAIL);
            if (instructor == null || instructor.getTenantId() == null
                    || !"INSTRUCTOR".equals(instructor.getLevel())) {
                fail("Instructor " + INSTRUCTOR_EMAIL + " must exist with level=INSTRUCTOR and non-null tenantId");
            }
            String instructorToken = login(INSTRUCTOR_EMAIL, "USER");

            List<Integer> leftoverIds = jdbcTemplate.queryForList(
                    "SELECT id FROM course WHERE title LIKE 's3-live-%'", Integer.class);
            if (!leftoverIds.isEmpty()) {
                fail("Leftover s3-live-% courses exist; refusing blind delete. ids=" + leftoverIds);
            }

            studentId = json(exchangeJson(studentToken, HttpMethod.GET, "/v2/me/profile", null))
                    .path("data").path("userId").asInt();
            User student = userMapper.selectById(studentId);
            previousAvatar = student.getAvatar();
            if (previousAvatar != null && !previousAvatar.isBlank()) {
                originalAvatarPhysical = s3ObjectKeyResolver.resolve("avatar", previousAvatar);
                Optional<S3ObjectMetadata> originalHead = s3ObjectStorage.headObject(originalAvatarPhysical);
                originalAvatarExisted = originalHead.isPresent();
                originalAvatarMeta = originalHead.orElse(null);
            }
            userMapper.clearAvatar(studentId);

            JsonNode course = json(exchangeJson(adminToken, HttpMethod.POST, "/v2/courses",
                    """
                    {"tenantId":%d,"primaryInstructorUserId":%d,"courseCode":"S3%s","title":"s3-live-%s",
                     "termStartDate":"%s","termEndDate":"%s"}
                    """.formatted(instructor.getTenantId(), instructor.getId(), suffix, suffix,
                            LocalDate.now(), LocalDate.now().plusMonths(4))));
            courseId = course.path("data").path("id").asInt();
            assertTrue(courseId > 0);

            JsonNode week = json(exchangeJson(instructorToken, HttpMethod.POST, "/v2/courses/" + courseId + "/weeks",
                    "{\"title\":\"Week 1\"}"));
            weekId = week.path("data").path("id").asInt();

            byte[] pdfA = "%PDF-1.4 live-a+".getBytes(StandardCharsets.UTF_8);
            byte[] pdfB = "%PDF-1.4 live-b ".getBytes(StandardCharsets.UTF_8);
            byte[] syllabusV1 = "%PDF-1.4 syllabus-v1".getBytes(StandardCharsets.UTF_8);
            byte[] syllabusV2 = "%PDF-1.4 syllabus-v2".getBytes(StandardCharsets.UTF_8);
            byte[] attachPdf = "%PDF-1.4 attachment".getBytes(StandardCharsets.UTF_8);
            byte[] attachMissing = "%PDF-1.4 missing-attach".getBytes(StandardCharsets.UTF_8);
            byte[] submitPdf = "%PDF-1.4 提交+".getBytes(StandardCharsets.UTF_8);
            byte[] png1 = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
            byte[] png2 = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 5, 6, 7, 8};

            HttpHeaders materialHeaders = bearer(instructorToken);
            materialHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            materialHeaders.add("Idempotency-Key", "s3-live-mat-" + suffix);
            MultiValueMap<String, Object> materialBody = new LinkedMultiValueMap<>();
            materialBody.add("files", filePart("课程 资料.pdf", "application/pdf", pdfA));
            materialBody.add("files", filePart("课程+资料.pdf", "application/pdf", pdfB));
            ResponseEntity<String> materialResp = exchange(instructorToken, HttpMethod.POST,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials",
                    new HttpEntity<>(materialBody, materialHeaders), String.class);
            assertEquals(HttpStatus.OK, materialResp.getStatusCode(), materialResp.getBody());
            JsonNode materialData = json(materialResp).path("data");
            CourseMaterial firstMaterial = courseMaterialMapper.selectById(materialData.get(0).path("id").asInt());
            CourseMaterial secondMaterial = courseMaterialMapper.selectById(materialData.get(1).path("id").asInt());
            CourseMaterial materialA = "课程 资料.pdf".equals(firstMaterial.getOriginalFilename())
                    ? firstMaterial : secondMaterial;
            CourseMaterial materialB = materialA == firstMaterial ? secondMaterial : firstMaterial;
            int materialIdA = materialA.getId();
            int materialIdB = materialB.getId();
            assertDbFileRow(materialA.getObjectKey(), materialA.getOriginalFilename(),
                    materialA.getSizeBytes(), materialA.getContentType(), "课程 资料.pdf", pdfA.length, "application/pdf");
            assertDbFileRow(materialB.getObjectKey(), materialB.getOriginalFilename(),
                    materialB.getSizeBytes(), materialB.getContentType(), "课程+资料.pdf", pdfB.length, "application/pdf");
            assertFalse(materialA.getObjectKey().startsWith("staging/"));
            assertFalse(materialB.getObjectKey().startsWith("staging/"));
            String materialPhysicalA = track(physicalKeys, resolveUploads(materialA.getObjectKey()));
            String materialPhysicalB = track(physicalKeys, resolveUploads(materialB.getObjectKey()));
            assertHead(materialPhysicalA, "application/pdf", pdfA.length);
            assertHead(materialPhysicalB, "application/pdf", pdfB.length);

            List<String> stagingLogical = jdbcTemplate.queryForList(
                    "SELECT object_key FROM minio_object_outbox WHERE course_id = ? AND object_key LIKE 'staging/%'",
                    String.class, courseId);
            assertFalse(stagingLogical.isEmpty(), "expected staging outbox keys for idempotent material upload");
            List<String> stagingPhysical = new ArrayList<>();
            for (String logical : stagingLogical) {
                stagingPhysical.add(track(physicalKeys, resolveUploads(logical)));
            }

            ResponseEntity<String> outsiderDownload = exchangeString(outsiderToken, HttpMethod.GET,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialIdA + "/download", null);
            assertError(outsiderDownload, HttpStatus.NOT_FOUND, "COURSE_NOT_FOUND");

            exchangeJson(adminToken, HttpMethod.POST, "/v2/courses/" + courseId + "/students",
                    "{\"userId\":" + studentId + "}");

            for (int i = 0; i < stagingPhysical.size(); i++) {
                waitUntilGone(stagingPhysical.get(i));
                assertOutboxDone(courseId, stagingLogical.get(i));
            }
            assertNoAbnormalOutbox(courseId);

            ResponseEntity<byte[]> previewA = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialIdA + "/preview");
            assertEquals(HttpStatus.OK, previewA.getStatusCode());
            assertBytesEqual(pdfA, previewA.getBody());
            ResponseEntity<byte[]> downloadA = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialIdA + "/download");
            assertEquals(HttpStatus.OK, downloadA.getStatusCode());
            assertTrue(downloadA.getHeaders().getContentType().toString().startsWith("application/pdf"));
            assertBytesEqual(pdfA, downloadA.getBody());
            ResponseEntity<byte[]> downloadB = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialIdB + "/download");
            assertBytesEqual(pdfB, downloadB.getBody());

            ResponseEntity<String> syllabusUpload = exchangeMultipart(instructorToken,
                    "/v2/courses/" + courseId + "/syllabus", "file", "大纲 文件.pdf", "application/pdf", syllabusV1);
            assertEquals(HttpStatus.OK, syllabusUpload.getStatusCode());
            CourseSyllabus syllabus = courseSyllabusMapper.selectByCourseId(courseId);
            CourseSyllabusVersion v1 = courseSyllabusVersionMapper.selectById(syllabus.getCurrentVersionId());
            assertDbFileRow(v1.getObjectKey(), v1.getOriginalFilename(), v1.getSizeBytes(), v1.getContentType(),
                    "大纲 文件.pdf", syllabusV1.length, "application/pdf");
            String syllabusPhysicalV1 = track(physicalKeys, resolveUploads(v1.getObjectKey()));
            assertHead(syllabusPhysicalV1, "application/pdf", syllabusV1.length);

            ResponseEntity<String> syllabusReplace = exchangeMultipart(instructorToken,
                    "/v2/courses/" + courseId + "/syllabus", "file", "大纲 替换.pdf", "application/pdf", syllabusV2);
            assertEquals(HttpStatus.OK, syllabusReplace.getStatusCode());
            syllabus = courseSyllabusMapper.selectByCourseId(courseId);
            CourseSyllabusVersion v2 = courseSyllabusVersionMapper.selectById(syllabus.getCurrentVersionId());
            assertEquals(v1.getId(), syllabus.getPreviousVersionId());
            assertNotEquals(v1.getId(), v2.getId());
            String syllabusPhysicalV2 = track(physicalKeys, resolveUploads(v2.getObjectKey()));
            assertHead(syllabusPhysicalV1, "application/pdf", syllabusV1.length);
            assertHead(syllabusPhysicalV2, "application/pdf", syllabusV2.length);
            ResponseEntity<byte[]> syllabusDownload = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/syllabus/download");
            assertEquals(HttpStatus.OK, syllabusDownload.getStatusCode());
            assertBytesEqual(syllabusV2, syllabusDownload.getBody());

            exchangeJson(instructorToken, HttpMethod.POST, "/v2/courses/" + courseId + "/weeks/" + weekId + "/publish",
                    "{}");
            ResponseEntity<byte[]> zip = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/download.zip");
            assertEquals(HttpStatus.OK, zip.getStatusCode());
            assertZipContains(zip.getBody(), pdfA, pdfB);

            JsonNode assignment = json(exchangeJson(instructorToken, HttpMethod.POST,
                    "/v2/courses/" + courseId + "/assignments",
                    """
                    {"title":"S3 live","dueAt":"2027-12-31T23:59:00","pointsPossible":100,
                     "allowedFileTypes":["pdf"],"maxFileSizeBytes":10485760,"maxFileCount":3,
                     "submissionType":"Individual"}
                    """));
            assignmentId = assignment.path("data").path("id").asInt();
            ResponseEntity<String> attachResp = exchangeMultipart(instructorToken,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments",
                    "files", "作业 附件.pdf", "application/pdf", attachPdf);
            assertEquals(HttpStatus.OK, attachResp.getStatusCode());
            int attachmentId = json(attachResp).path("data").get(0).path("id").asInt();
            AssignmentAttachment attachment = assignmentAttachmentMapper.selectById(attachmentId);
            assertDbFileRow(attachment.getObjectKey(), attachment.getOriginalName(),
                    attachment.getSizeBytes(), attachment.getContentType(),
                    "作业 附件.pdf", attachPdf.length, "application/pdf");
            String attachPhysical = track(physicalKeys, resolveUploads(attachment.getObjectKey()));
            assertHead(attachPhysical, "application/pdf", attachPdf.length);
            ResponseEntity<byte[]> attachDownload = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/"
                            + attachmentId + "/download");
            assertBytesEqual(attachPdf, attachDownload.getBody());

            ResponseEntity<String> draftStudentDownload = exchangeString(studentToken, HttpMethod.GET,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/"
                            + attachmentId + "/download", null);
            assertError(draftStudentDownload, HttpStatus.NOT_FOUND, "ASSIGNMENT_NOT_FOUND");

            exchangeJson(instructorToken, HttpMethod.POST,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/publish", "{}");
            ResponseEntity<byte[]> publishedStudentDownload = exchangeBytes(studentToken,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/"
                            + attachmentId + "/download");
            assertEquals(HttpStatus.OK, publishedStudentDownload.getStatusCode());
            assertBytesEqual(attachPdf, publishedStudentDownload.getBody());

            exchangeJson(instructorToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/" + attachmentId, null);
            waitUntilGone(attachPhysical);
            ResponseEntity<String> deletedAttachGet = exchangeString(studentToken, HttpMethod.GET,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/"
                            + attachmentId + "/download", null);
            assertEquals(HttpStatus.NOT_FOUND, deletedAttachGet.getStatusCode());

            ResponseEntity<String> attachMissingResp = exchangeMultipart(instructorToken,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments",
                    "files", "作业 缺对象.pdf", "application/pdf", attachMissing);
            int missingAttachId = json(attachMissingResp).path("data").get(0).path("id").asInt();
            AssignmentAttachment missingAttach = assignmentAttachmentMapper.selectById(missingAttachId);
            String missingAttachPhysical = track(physicalKeys, resolveUploads(missingAttach.getObjectKey()));
            s3ObjectStorage.deleteObject(missingAttachPhysical);
            assertMissingOnS3(missingAttachPhysical);
            ResponseEntity<String> missingAttachGet = exchangeString(instructorToken, HttpMethod.GET,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/"
                            + missingAttachId + "/download", null);
            assertError(missingAttachGet, HttpStatus.SERVICE_UNAVAILABLE, "STORAGE_FAILURE");

            ResponseEntity<String> stagingResp = exchangeMultipart(studentToken,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/submission-staging-files",
                    "files", "提交 作业+.pdf", "application/pdf", submitPdf);
            assertEquals(HttpStatus.OK, stagingResp.getStatusCode());
            int stagingFileId = json(stagingResp).path("data").get(0).path("id").asInt();
            AssignmentSubmissionStagingFile staging = stagingFileMapper.selectById(stagingFileId);
            assertDbFileRow(staging.getObjectKey(), staging.getOriginalName(), staging.getSizeBytes(),
                    staging.getContentType(), "提交 作业+.pdf", submitPdf.length, "application/pdf");
            String stagingSubmitPhysical = track(physicalKeys, resolveUploads(staging.getObjectKey()));
            assertHead(stagingSubmitPhysical, "application/pdf", submitPdf.length);

            JsonNode submitted = json(exchangeJson(studentToken, HttpMethod.POST,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/submissions", "{}"));
            int submissionId = submitted.path("data").path("submissionId").asInt();
            JsonNode submittedFile = submitted.path("data").path("currentVersion").path("files").get(0);
            int submissionFileId = submittedFile.path("id").asInt();
            AssignmentSubmissionFile submissionFile = submissionFileMapper.selectById(submissionFileId);
            assertDbFileRow(submissionFile.getObjectKey(), submissionFile.getOriginalName(),
                    submissionFile.getSizeBytes(), submissionFile.getContentType(),
                    "提交 作业+.pdf", submitPdf.length, "application/pdf");
            track(physicalKeys, resolveUploads(submissionFile.getObjectKey()));

            String submissionFilePath = "/v2/courses/" + courseId + "/assignments/" + assignmentId
                    + "/submissions/" + submissionId + "/files/" + submissionFileId;
            ResponseEntity<byte[]> submissionDownload = exchangeBytes(studentToken, submissionFilePath + "/download");
            assertEquals(HttpStatus.OK, submissionDownload.getStatusCode());
            assertBytesEqual(submitPdf, submissionDownload.getBody());
            ResponseEntity<byte[]> submissionPreview = exchangeBytes(studentToken, submissionFilePath + "/preview");
            assertEquals(HttpStatus.OK, submissionPreview.getStatusCode());
            assertBytesEqual(submitPdf, submissionPreview.getBody());
            String previewDisposition = submissionPreview.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            if (previewDisposition != null) {
                assertTrue(previewDisposition.toLowerCase().contains("inline"));
            }

            exchangeJson(instructorToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialIdA, null);
            waitUntilGone(materialPhysicalA);
            ResponseEntity<String> deletedMaterialGet = exchangeString(instructorToken, HttpMethod.GET,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialIdA + "/download", null);
            assertEquals(HttpStatus.NOT_FOUND, deletedMaterialGet.getStatusCode());

            s3ObjectStorage.deleteObject(materialPhysicalB);
            assertMissingOnS3(materialPhysicalB);
            ResponseEntity<String> missingMaterialGet = exchangeString(instructorToken, HttpMethod.GET,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialIdB + "/download", null);
            assertError(missingMaterialGet, HttpStatus.NOT_FOUND, "NOT_FOUND");

            s3ObjectStorage.deleteObject(syllabusPhysicalV2);
            assertMissingOnS3(syllabusPhysicalV2);
            ResponseEntity<String> missingSyllabus = exchangeString(instructorToken, HttpMethod.GET,
                    "/v2/courses/" + courseId + "/syllabus/download", null);
            assertError(missingSyllabus, HttpStatus.NOT_FOUND, "SYLLABUS_NOT_FOUND");
            assertHead(syllabusPhysicalV1, "application/pdf", syllabusV1.length);

            ResponseEntity<String> avatarResp = exchangeMultipart(studentToken, HttpMethod.PUT, "/v2/me/profile/avatar",
                    "file", "头像.png", "image/png", png1);
            assertEquals(HttpStatus.OK, avatarResp.getStatusCode());
            String avatarKey1 = userMapper.selectById(studentId).getAvatar();
            String avatarPhysical1 = track(physicalKeys, s3ObjectKeyResolver.resolve("avatar", avatarKey1));
            assertTrue(avatarPhysical1.startsWith("avatar/"));
            assertHead(avatarPhysical1, "image/", png1.length);
            ResponseEntity<byte[]> avatarDownload1 = exchangeBytes(studentToken, "/v2/users/" + studentId + "/avatar");
            assertEquals(HttpStatus.OK, avatarDownload1.getStatusCode());
            assertTrue(avatarDownload1.getHeaders().getContentType().toString().startsWith("image/"));
            assertBytesEqual(png1, avatarDownload1.getBody());

            ResponseEntity<String> avatarReplace = exchangeMultipart(studentToken, HttpMethod.PUT, "/v2/me/profile/avatar",
                    "file", "头像 替换.png", "image/png", png2);
            assertEquals(HttpStatus.OK, avatarReplace.getStatusCode());
            String avatarKey2 = userMapper.selectById(studentId).getAvatar();
            assertNotEquals(avatarKey1, avatarKey2);
            String avatarPhysical2 = track(physicalKeys, s3ObjectKeyResolver.resolve("avatar", avatarKey2));
            assertHead(avatarPhysical2, "image/", png2.length);
            waitUntilGone(avatarPhysical1);
            ResponseEntity<byte[]> avatarDownload2 = exchangeBytes(studentToken, "/v2/users/" + studentId + "/avatar");
            assertBytesEqual(png2, avatarDownload2.getBody());

            s3ObjectStorage.deleteObject(avatarPhysical2);
            assertMissingOnS3(avatarPhysical2);
            ResponseEntity<String> missingAvatar = exchangeString(studentToken, HttpMethod.GET,
                    "/v2/users/" + studentId + "/avatar", null);
            assertError(missingAvatar, HttpStatus.NOT_FOUND, "NOT_FOUND");

            String missingKey = "lms-uploads/s3-live-missing-" + suffix;
            s3ObjectStorage.deleteObject(missingKey);
            assertMissingOnS3(missingKey);
            assertNoAbnormalOutbox(courseId);
        } catch (Throwable t) {
            testFailure = t;
        } finally {
            List<String> teardownErrors = teardown(
                    adminToken, courseId, studentId, previousAvatar, originalAvatarExisted,
                    originalAvatarPhysical, originalAvatarMeta, physicalKeys);
            if (testFailure != null) {
                for (String error : teardownErrors) {
                    testFailure.addSuppressed(new AssertionError(error));
                }
                if (testFailure instanceof Error error) {
                    throw error;
                }
                if (testFailure instanceof Exception exception) {
                    throw exception;
                }
                throw new AssertionError(testFailure);
            }
            if (!teardownErrors.isEmpty()) {
                fail("Teardown failed:\n" + String.join("\n", teardownErrors));
            }
        }
    }

    private List<String> teardown(String adminToken, Integer courseId, Integer studentId, String previousAvatar,
                                  boolean originalAvatarExisted, String originalAvatarPhysical,
                                  S3ObjectMetadata originalAvatarMeta, Set<String> physicalKeys) {
        List<String> errors = new ArrayList<>();
        restoreAvatar(errors, studentId, previousAvatar);
        if (originalAvatarExisted && originalAvatarPhysical != null) {
            try {
                Optional<S3ObjectMetadata> head = s3ObjectStorage.headObject(originalAvatarPhysical);
                if (head.isEmpty()) {
                    errors.add("original avatar object missing after teardown: " + originalAvatarPhysical);
                } else if (originalAvatarMeta != null) {
                    if (originalAvatarMeta.contentLength() != null
                            && !originalAvatarMeta.contentLength().equals(head.get().contentLength())) {
                        errors.add("original avatar contentLength changed: " + originalAvatarPhysical);
                    }
                }
            } catch (RuntimeException e) {
                errors.add("original avatar head failed: " + e.getMessage());
            }
        }
        for (String key : new ArrayList<>(physicalKeys)) {
            try {
                s3ObjectStorage.deleteObject(key);
                if (s3ObjectStorage.headObject(key).isPresent()) {
                    errors.add("S3 key still present after delete: " + key);
                }
            } catch (RuntimeException e) {
                errors.add("delete/head " + key + " -> " + e.getMessage());
            }
        }
        teardownCourse(errors, adminToken, courseId);
        if (courseId != null) {
            try {
                Integer remaining = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM course WHERE id = ?", Integer.class, courseId);
                if (remaining != null && remaining > 0) {
                    errors.add("course row still present: " + courseId);
                }
            } catch (RuntimeException e) {
                errors.add("course existence check failed: " + e.getMessage());
            }
            try {
                Integer abnormal = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM minio_object_outbox WHERE course_id = ? AND status IN ('PENDING','DEAD')",
                        Integer.class, courseId);
                if (abnormal != null && abnormal > 0) {
                    errors.add("outbox PENDING/DEAD leftover for course " + courseId + ": " + abnormal);
                }
            } catch (RuntimeException e) {
                errors.add("outbox leftover check failed: " + e.getMessage());
            }
        }
        try {
            List<Integer> leftover = jdbcTemplate.queryForList(
                    "SELECT id FROM course WHERE title LIKE 's3-live-%'", Integer.class);
            if (!leftover.isEmpty()) {
                errors.add("s3-live-% courses remain: " + leftover);
            }
        } catch (RuntimeException e) {
            errors.add("s3-live-% leftover check failed: " + e.getMessage());
        }
        return errors;
    }

    private void restoreAvatar(List<String> errors, Integer studentId, String previousAvatar) {
        if (studentId == null) {
            return;
        }
        try {
            if (previousAvatar == null || previousAvatar.isBlank()) {
                userMapper.clearAvatar(studentId);
            } else {
                User patch = new User();
                patch.setId(studentId);
                patch.setAvatar(previousAvatar);
                userMapper.updateById(patch);
            }
        } catch (RuntimeException e) {
            errors.add("restore avatar failed: " + e.getMessage());
        }
    }

    private void teardownCourse(List<String> errors, String adminToken, Integer courseId) {
        if (courseId == null) {
            return;
        }
        sql(errors, "DELETE asf FROM assignment_submission_file asf "
                + "INNER JOIN assignment_submission_version v ON v.id = asf.submission_version_id "
                + "INNER JOIN assignment a ON a.id = v.assignment_id WHERE a.course_id = ?", courseId);
        sql(errors, "DELETE r FROM assignment_submission_receipt r "
                + "INNER JOIN assignment_submission_version v ON v.id = r.submission_version_id "
                + "INNER JOIN assignment a ON a.id = v.assignment_id WHERE a.course_id = ?", courseId);
        sql(errors, "DELETE v FROM assignment_submission_version v "
                + "INNER JOIN assignment a ON a.id = v.assignment_id WHERE a.course_id = ?", courseId);
        sql(errors, "DELETE st FROM assignment_submission_staging_file st "
                + "INNER JOIN assignment a ON a.id = st.assignment_id WHERE a.course_id = ?", courseId);
        sql(errors, "DELETE s FROM assignment_submission s "
                + "INNER JOIN assignment a ON a.id = s.assignment_id WHERE a.course_id = ?", courseId);
        sql(errors, "DELETE aa FROM assignment_attachment aa "
                + "INNER JOIN assignment a ON a.id = aa.assignment_id WHERE a.course_id = ?", courseId);
        sql(errors, "DELETE FROM assignment_audit_log WHERE course_id = ?", courseId);
        sql(errors, "DELETE FROM assignment_grade WHERE assignment_id IN (SELECT id FROM assignment WHERE course_id = ?)",
                courseId);
        sql(errors, "DELETE FROM assignment WHERE course_id = ?", courseId);
        sql(errors, "DELETE cm FROM course_material cm "
                + "INNER JOIN course_week cw ON cw.id = cm.week_id WHERE cw.course_id = ?", courseId);
        sql(errors, "DELETE FROM course_week WHERE course_id = ?", courseId);
        sql(errors, "DELETE FROM course_syllabus WHERE course_id = ?", courseId);
        sql(errors, "DELETE FROM course_syllabus_version WHERE course_id = ?", courseId);
        sql(errors, "DELETE FROM minio_object_outbox WHERE course_id = ?", courseId);
        sql(errors, "DELETE FROM upload_operation WHERE course_id = ?", courseId);
        sql(errors, "DELETE FROM course_audit_log WHERE course_id = ?", courseId);
        sql(errors, "DELETE FROM enrollment WHERE course_id = ?", courseId);
        if (adminToken != null) {
            exchangeJsonQuiet(errors, adminToken, HttpMethod.DELETE, "/v2/courses/" + courseId, null);
        }
        sql(errors, "DELETE FROM course WHERE id = ?", courseId);
    }

    private void sql(List<String> errors, String sql, Object... args) {
        try {
            jdbcTemplate.update(sql, args);
        } catch (RuntimeException e) {
            errors.add(sql + " -> " + e.getMessage());
        }
    }

    private void waitUntilGone(String physicalKey) throws InterruptedException {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(30));
        while (Instant.now().isBefore(deadline)) {
            minioOutboxService.processBatch();
            if (s3ObjectStorage.headObject(physicalKey).isEmpty()) {
                return;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("S3 object still present after outbox wait: " + physicalKey);
    }

    private void assertOutboxDone(Integer courseId, String logicalKey) {
        List<String> statuses = jdbcTemplate.queryForList(
                "SELECT status FROM minio_object_outbox WHERE course_id = ? AND object_key = ?",
                String.class, courseId, logicalKey);
        assertFalse(statuses.isEmpty(), "missing outbox row for " + logicalKey);
        for (String status : statuses) {
            assertEquals("DONE", status, "outbox " + logicalKey + " status=" + status);
        }
    }

    private void assertNoAbnormalOutbox(Integer courseId) {
        Integer abnormal = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM minio_object_outbox WHERE course_id = ? AND status IN ('PENDING','DEAD')",
                Integer.class, courseId);
        assertEquals(0, abnormal, "outbox PENDING/DEAD for course " + courseId);
    }

    private void assertHead(String physicalKey, String contentTypePrefix, long contentLength) {
        assertTrue(physicalKey.startsWith("avatar/") || physicalKey.startsWith("lms-uploads/"), physicalKey);
        Optional<S3ObjectMetadata> head = s3ObjectStorage.headObject(physicalKey);
        assertTrue(head.isPresent(), "missing S3 object " + physicalKey);
        S3ObjectMetadata meta = head.get();
        assertNotNull(meta.contentType());
        assertTrue(meta.contentType().startsWith(contentTypePrefix),
                physicalKey + " contentType=" + meta.contentType());
        assertEquals(contentLength, meta.contentLength());
        assertNotNull(meta.etag());
        assertFalse(meta.etag().isBlank());
    }

    private void assertMissingOnS3(String physicalKey) {
        assertTrue(s3ObjectStorage.headObject(physicalKey).isEmpty(), "head still present: " + physicalKey);
        try {
            S3ObjectPayload payload = s3ObjectStorage.getObject(physicalKey);
            payload.content().close();
            fail("getObject should miss: " + physicalKey);
        } catch (S3ObjectNotFoundException expected) {
            // expected
        } catch (Exception e) {
            fail("getObject missing should be S3ObjectNotFoundException: " + e);
        }
    }

    private void assertDbFileRow(String objectKey, String filename, Long sizeBytes, String contentType,
                                 String expectedName, long expectedSize, String expectedTypePrefix) {
        assertNotNull(objectKey);
        assertFalse(objectKey.isBlank());
        assertEquals(expectedName, filename);
        assertEquals(expectedSize, sizeBytes);
        assertNotNull(contentType);
        assertTrue(contentType.startsWith(expectedTypePrefix), contentType);
    }

    private void assertBytesEqual(byte[] expected, byte[] actual) {
        assertNotNull(actual);
        assertArrayEquals(expected, actual);
    }

    private void assertZipContains(byte[] zipBytes, byte[]... expectedBodies) throws Exception {
        assertNotNull(zipBytes);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zin.getNextEntry()) != null) {
                entries.put(entry.getName(), zin.readAllBytes());
            }
        }
        assertTrue(entries.size() >= expectedBodies.length, "zip entries=" + entries.keySet());
        for (byte[] expected : expectedBodies) {
            boolean found = false;
            for (byte[] actual : entries.values()) {
                if (Arrays.equals(expected, actual)) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "zip missing expected entry bytes, names=" + entries.keySet());
        }
    }

    private void assertError(ResponseEntity<String> response, HttpStatus status, String code) throws Exception {
        assertEquals(status, response.getStatusCode(), response.getBody());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(code, body.path("code").asText(), response.getBody());
        String raw = response.getBody() == null ? "" : response.getBody();
        assertFalse(raw.contains("software.amazon"), raw);
        assertFalse(raw.contains("S3Exception"), raw);
    }

    private String login(String email, String role) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"" + role + "\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity("/v1/auth/login", new HttpEntity<>(body, headers), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        String token = json(response).path("data").path("accessToken").asText();
        assertFalse(token.isBlank());
        return token;
    }

    private ResponseEntity<String> exchangeJson(String token, HttpMethod method, String path, String jsonBody)
            throws Exception {
        ResponseEntity<String> response = exchangeString(token, method, path, jsonBody);
        assertTrue(response.getStatusCode().is2xxSuccessful(), path + " -> " + response.getStatusCode() + " " + response.getBody());
        return response;
    }

    private ResponseEntity<String> exchangeString(String token, HttpMethod method, String path, String jsonBody) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (method != HttpMethod.GET) {
            headers.add("Idempotency-Key", "s3-live-" + UUID.randomUUID());
        }
        return exchange(token, method, path, new HttpEntity<>(jsonBody, headers), String.class);
    }

    private ResponseEntity<String> exchangeMultipart(String token, String path, String partName,
                                                     String filename, String contentType, byte[] bytes) {
        return exchangeMultipart(token, HttpMethod.POST, path, partName, filename, contentType, bytes);
    }

    private ResponseEntity<String> exchangeMultipart(String token, HttpMethod method, String path, String partName,
                                                     String filename, String contentType, byte[] bytes) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("Idempotency-Key", "s3-live-" + UUID.randomUUID());
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add(partName, filePart(filename, contentType, bytes));
        ResponseEntity<String> response = exchange(token, method, path, new HttpEntity<>(body, headers), String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), path + " -> " + response.getStatusCode() + " " + response.getBody());
        return response;
    }

    private ResponseEntity<byte[]> exchangeBytes(String token, String path) {
        return exchange(token, HttpMethod.GET, path, new HttpEntity<>(bearer(token)), byte[].class);
    }

    private <T> ResponseEntity<T> exchange(String token, HttpMethod method, String path,
                                           HttpEntity<?> entity, Class<T> type) {
        try {
            return rest.exchange(path, method, entity, type);
        } catch (ResourceAccessException first) {
            return rest.exchange(path, method, entity, type);
        } catch (HttpStatusCodeException e) {
            @SuppressWarnings("unchecked")
            T body = type == byte[].class
                    ? (T) e.getResponseBodyAsByteArray()
                    : (T) e.getResponseBodyAsString();
            return ResponseEntity.status(e.getStatusCode()).headers(e.getResponseHeaders()).body(body);
        }
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set(HttpHeaders.CONNECTION, "close");
        return headers;
    }

    private static HttpEntity<ByteArrayResource> filePart(String filename, String contentType, byte[] bytes) {
        HttpHeaders partHeaders = new HttpHeaders();
        partHeaders.setContentType(MediaType.parseMediaType(contentType));
        ByteArrayResource resource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        return new HttpEntity<>(resource, partHeaders);
    }

    private void exchangeJsonQuiet(List<String> errors, String token, HttpMethod method, String path, String jsonBody) {
        try {
            HttpHeaders headers = bearer(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (method != HttpMethod.GET) {
                headers.add("Idempotency-Key", "s3-live-" + UUID.randomUUID());
            }
            rest.exchange(path, method, new HttpEntity<>(jsonBody, headers), String.class);
        } catch (RuntimeException e) {
            errors.add(method + " " + path + " -> " + e.getMessage());
        }
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }

    private String resolveUploads(String logicalKey) {
        String physical = s3ObjectKeyResolver.resolve("lms-uploads", logicalKey);
        assertTrue(physical.startsWith("lms-uploads/"), physical);
        return physical;
    }

    private String track(Set<String> physicalKeys, String physicalKey) {
        physicalKeys.add(physicalKey);
        return physicalKey;
    }
}
