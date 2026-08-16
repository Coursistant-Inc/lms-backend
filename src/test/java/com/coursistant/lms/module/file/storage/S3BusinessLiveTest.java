package com.coursistant.lms.module.file.storage;

import com.coursistant.lms.module.assignment.entity.AssignmentAttachment;
import com.coursistant.lms.module.assignment.repository.AssignmentAttachmentMapper;
import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.material.repository.CourseMaterialMapper;
import com.coursistant.lms.module.course.content.syllabus.entity.CourseSyllabus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_S3_LIVE", matches = "true")
class S3BusinessLiveTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String STUDENT_EMAIL = "regtest1@example.com";
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
        List<String> physicalKeys = new ArrayList<>();
        Integer courseId = null;
        Integer weekId = null;
        Integer assignmentId = null;
        Integer materialId = null;
        String previousAvatar = null;
        String newAvatarKey = null;
        Integer studentId = null;

        String adminToken = login(ADMIN_EMAIL, "ADMIN");
        String studentToken = login(STUDENT_EMAIL, "USER");
        studentId = json(exchangeJson(studentToken, HttpMethod.GET, "/v2/me/profile", null))
                .path("data").path("userId").asInt();
        User student = userMapper.selectById(studentId);
        previousAvatar = student.getAvatar();

        User instructor = userMapper.selectTeachers().stream()
                .filter(u -> u.getTenantId() != null)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Need an INSTRUCTOR user in local DB"));
        String instructorToken = login(instructor.getEmail(), "USER");

        for (Integer leftoverId : jdbcTemplate.queryForList(
                "SELECT id FROM course WHERE title LIKE 's3-live-%'", Integer.class)) {
            teardownCourse(adminToken, leftoverId, studentId, null, null, null);
        }

        try {
            JsonNode course = json(exchangeJson(adminToken, HttpMethod.POST, "/v2/courses",
                    """
                    {"tenantId":%d,"primaryInstructorUserId":%d,"courseCode":"S3%s","title":"s3-live-%s",
                     "termStartDate":"%s","termEndDate":"%s"}
                    """.formatted(instructor.getTenantId(), instructor.getId(), suffix, suffix,
                            LocalDate.now(), LocalDate.now().plusMonths(4))));
            courseId = course.path("data").path("id").asInt();
            assertTrue(courseId > 0);

            exchangeJson(adminToken, HttpMethod.POST, "/v2/courses/" + courseId + "/students",
                    "{\"userId\":" + studentId + "}");

            JsonNode week = json(exchangeJson(instructorToken, HttpMethod.POST, "/v2/courses/" + courseId + "/weeks",
                    "{\"title\":\"Week 1\"}"));
            weekId = week.path("data").path("id").asInt();

            byte[] pdf = "%PDF-1.4 live".getBytes(StandardCharsets.UTF_8);
            ResponseEntity<String> syllabusUpload = exchangeMultipart(instructorToken,
                    "/v2/courses/" + courseId + "/syllabus", "file", "大纲 文件.pdf", "application/pdf", pdf);
            assertEquals(HttpStatus.OK, syllabusUpload.getStatusCode());

            CourseSyllabus syllabus = courseSyllabusMapper.selectByCourseId(courseId);
            String syllabusLogical = courseSyllabusVersionMapper.selectById(syllabus.getCurrentVersionId()).getObjectKey();
            String syllabusPhysical = s3ObjectKeyResolver.resolve("lms-uploads", syllabusLogical);
            physicalKeys.add(syllabusPhysical);
            assertTrue(s3ObjectStorage.headObject(syllabusPhysical).isPresent());
            assertTrue(syllabusPhysical.startsWith("lms-uploads/"));

            ResponseEntity<byte[]> syllabusDownload = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/syllabus/download");
            assertEquals(HttpStatus.OK, syllabusDownload.getStatusCode());
            assertEquals(pdf.length, syllabusDownload.getBody().length);

            HttpHeaders materialHeaders = bearer(instructorToken);
            materialHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            materialHeaders.add("Idempotency-Key", "s3-live-" + suffix);
            MultiValueMap<String, Object> materialBody = new LinkedMultiValueMap<>();
            materialBody.add("files", filePart("课程 资料.pdf", "application/pdf", pdf));
            ResponseEntity<String> materialResp = rest.exchange(
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials",
                    HttpMethod.POST, new HttpEntity<>(materialBody, materialHeaders), String.class);
            assertEquals(HttpStatus.OK, materialResp.getStatusCode());
            materialId = json(materialResp).path("data").get(0).path("id").asInt();
            CourseMaterial material = courseMaterialMapper.selectById(materialId);
            String materialPhysical = s3ObjectKeyResolver.resolve("lms-uploads", material.getObjectKey());
            physicalKeys.add(materialPhysical);
            assertTrue(materialPhysical.startsWith("lms-uploads/"));
            assertTrue(s3ObjectStorage.headObject(materialPhysical).isPresent());
            if (material.getObjectKey().startsWith("staging/")) {
                throw new AssertionError("staging key was not copied to final: " + material.getObjectKey());
            }

            ResponseEntity<byte[]> materialDownload = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialId + "/download");
            assertEquals(HttpStatus.OK, materialDownload.getStatusCode());
            assertTrue(materialDownload.getHeaders().getContentType().toString().startsWith("application/pdf"));

            exchangeJson(instructorToken, HttpMethod.POST, "/v2/courses/" + courseId + "/weeks/" + weekId + "/publish", "{}");
            ResponseEntity<byte[]> zip = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/download.zip");
            assertEquals(HttpStatus.OK, zip.getStatusCode());
            assertTrue(zip.getBody() != null && zip.getBody().length > 0);

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
                    "files", "作业 附件.pdf", "application/pdf", pdf);
            assertEquals(HttpStatus.OK, attachResp.getStatusCode());
            int attachmentId = json(attachResp).path("data").get(0).path("id").asInt();
            AssignmentAttachment attachment = assignmentAttachmentMapper.selectById(attachmentId);
            String attachPhysical = s3ObjectKeyResolver.resolve("lms-uploads", attachment.getObjectKey());
            physicalKeys.add(attachPhysical);
            assertTrue(attachPhysical.startsWith("lms-uploads/"));
            assertTrue(s3ObjectStorage.headObject(attachPhysical).isPresent());

            ResponseEntity<byte[]> attachDownload = exchangeBytes(instructorToken,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/" + attachmentId + "/download");
            assertEquals(HttpStatus.OK, attachDownload.getStatusCode());
            assertEquals(pdf.length, attachDownload.getBody().length);

            exchangeJson(instructorToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId + "/attachments/" + attachmentId, null);
            waitUntilGone(attachPhysical);
            physicalKeys.remove(attachPhysical);

            byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 1, 2, 3};
            ResponseEntity<String> avatarResp = exchangeMultipart(studentToken, HttpMethod.PUT, "/v2/me/profile/avatar",
                    "file", "头像.png", "image/png", png);
            assertEquals(HttpStatus.OK, avatarResp.getStatusCode());
            newAvatarKey = userMapper.selectById(studentId).getAvatar();
            String avatarPhysical = s3ObjectKeyResolver.resolve("avatar", newAvatarKey);
            physicalKeys.add(avatarPhysical);
            assertTrue(avatarPhysical.startsWith("avatar/"));
            assertTrue(s3ObjectStorage.headObject(avatarPhysical).isPresent());

            ResponseEntity<byte[]> avatarDownload = exchangeBytes(studentToken, "/v2/users/" + studentId + "/avatar");
            assertEquals(HttpStatus.OK, avatarDownload.getStatusCode());
            assertTrue(avatarDownload.getHeaders().getContentType().toString().startsWith("image/"));

            s3ObjectStorage.deleteObject("lms-uploads/s3-live-missing-" + suffix);
            assertTrue(s3ObjectStorage.headObject("lms-uploads/s3-live-missing-" + suffix).isEmpty());

            exchangeJson(instructorToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialId, null);
            waitUntilGone(materialPhysical);
            physicalKeys.remove(materialPhysical);
        } finally {
            restoreAvatar(studentId, previousAvatar, newAvatarKey, physicalKeys);
            for (String key : new ArrayList<>(physicalKeys)) {
                try {
                    s3ObjectStorage.deleteObject(key);
                } catch (RuntimeException ignored) {
                    // best-effort teardown
                }
            }
            teardownCourse(adminToken, courseId, studentId, weekId, assignmentId, materialId);
        }
    }

    private void teardownCourse(String adminToken, Integer courseId, Integer studentId,
                                Integer weekId, Integer assignmentId, Integer materialId) {
        if (courseId == null) {
            return;
        }
        if (materialId != null && weekId != null) {
            exchangeJsonQuiet(adminToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/weeks/" + weekId + "/materials/" + materialId, null);
        }
        if (assignmentId != null) {
            exchangeJsonQuiet(adminToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/assignments/" + assignmentId, null);
        }
        if (weekId != null) {
            exchangeJsonQuiet(adminToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/weeks/" + weekId, null);
        }
        exchangeJsonQuiet(adminToken, HttpMethod.DELETE, "/v2/courses/" + courseId + "/syllabus", null);
        if (studentId != null) {
            exchangeJsonQuiet(adminToken, HttpMethod.DELETE,
                    "/v2/courses/" + courseId + "/students/" + studentId, null);
        }
        try {
            jdbcTemplate.update("DELETE FROM assignment_audit_log WHERE course_id = ?", courseId);
            jdbcTemplate.update("DELETE FROM minio_object_outbox WHERE course_id = ?", courseId);
            jdbcTemplate.update(
                    "DELETE aa FROM assignment_attachment aa INNER JOIN assignment a ON a.id = aa.assignment_id WHERE a.course_id = ?",
                    courseId);
            jdbcTemplate.update("DELETE FROM assignment WHERE course_id = ?", courseId);
            jdbcTemplate.update(
                    "DELETE cm FROM course_material cm INNER JOIN course_week cw ON cw.id = cm.week_id WHERE cw.course_id = ?",
                    courseId);
            jdbcTemplate.update("DELETE FROM course_week WHERE course_id = ?", courseId);
            jdbcTemplate.update("DELETE FROM course_syllabus_version WHERE course_id = ?", courseId);
            jdbcTemplate.update("DELETE FROM course_syllabus WHERE course_id = ?", courseId);
            jdbcTemplate.update("DELETE FROM enrollment WHERE course_id = ? AND course_role <> 'Instructor'", courseId);
        } catch (RuntimeException ignored) {
            // continue with HTTP/course row delete
        }
        exchangeJsonQuiet(adminToken, HttpMethod.DELETE, "/v2/courses/" + courseId, null);
        try {
            Integer remaining = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM course WHERE id = ?", Integer.class, courseId);
            if (remaining != null && remaining > 0) {
                jdbcTemplate.update("DELETE FROM enrollment WHERE course_id = ?", courseId);
                jdbcTemplate.update("DELETE FROM course WHERE id = ?", courseId);
            }
        } catch (RuntimeException ignored) {
            // leftover row is reported by the main assertions / next run cleanup
        }
    }

    private void restoreAvatar(Integer studentId, String previousAvatar, String newAvatarKey, List<String> physicalKeys) {
        if (studentId == null) {
            return;
        }
        try {
            User patch = new User();
            patch.setId(studentId);
            patch.setAvatar(previousAvatar);
            userMapper.updateById(patch);
            if (newAvatarKey != null && !newAvatarKey.equals(previousAvatar)) {
                String physical = s3ObjectKeyResolver.resolve("avatar", newAvatarKey);
                s3ObjectStorage.deleteObject(physical);
                physicalKeys.remove(physical);
            }
        } catch (RuntimeException ignored) {
            // best-effort teardown
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

    private ResponseEntity<String> exchangeJson(String token, HttpMethod method, String path, String jsonBody) {
        HttpHeaders headers = bearer(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (method != HttpMethod.GET) {
            headers.add("Idempotency-Key", "s3-live-" + UUID.randomUUID());
        }
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
        try {
            ResponseEntity<String> response = rest.exchange(path, method, entity, String.class);
            assertTrue(response.getStatusCode().is2xxSuccessful(), path + " -> " + response.getStatusCode() + " " + response.getBody());
            return response;
        } catch (org.springframework.web.client.ResourceAccessException first) {
            ResponseEntity<String> response = rest.exchange(path, method, entity, String.class);
            assertTrue(response.getStatusCode().is2xxSuccessful(), path + " -> " + response.getStatusCode() + " " + response.getBody());
            return response;
        }
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
        ResponseEntity<String> response = rest.exchange(path, method, new HttpEntity<>(body, headers), String.class);
        assertTrue(response.getStatusCode().is2xxSuccessful(), path + " -> " + response.getStatusCode() + " " + response.getBody());
        return response;
    }

    private ResponseEntity<byte[]> exchangeBytes(String token, String path) {
        HttpHeaders headers = bearer(token);
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
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

    private void exchangeJsonQuiet(String token, HttpMethod method, String path, String jsonBody) {
        try {
            HttpHeaders headers = bearer(token);
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (method != HttpMethod.GET) {
                headers.add("Idempotency-Key", "s3-live-" + UUID.randomUUID());
            }
            rest.exchange(path, method, new HttpEntity<>(jsonBody, headers), String.class);
        } catch (RuntimeException ignored) {
            // best-effort teardown
        }
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        assertNotNull(response.getBody());
        return objectMapper.readTree(response.getBody());
    }
}
