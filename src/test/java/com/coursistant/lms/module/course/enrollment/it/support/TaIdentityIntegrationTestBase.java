package com.coursistant.lms.module.course.enrollment.it.support;

import com.coursistant.lms.module.auth.it.support.AuthIntegrationTestBase;
import com.coursistant.lms.module.auth.it.support.AuthTestDataFactory;
import com.coursistant.lms.module.auth.admin.entity.Admin;
import com.coursistant.lms.module.user.account.entity.User;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth IT base + course/enrollment/group/assignment/quiz schema for TA identity flows.
 */
public abstract class TaIdentityIntegrationTestBase extends AuthIntegrationTestBase {

    private static boolean courseSchemaApplied;

    @Override
    protected void wipeAuthTables() {
        ensureCourseSchema();
        wipeCourseModuleTables();
        wipeIdempotencyRedisKeys();
        super.wipeAuthTables();
    }

    @BeforeEach
    void resetTaIdentityExtras() {
        // Course wipe already happens in wipeAuthTables (before auth wipe).
        ensureCourseSchema();
    }

    private void ensureCourseSchema() {
        if (courseSchemaApplied) {
            return;
        }
        synchronized (TaIdentityIntegrationTestBase.class) {
            if (courseSchemaApplied) {
                return;
            }
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("ta-identity-it-schema.sql"));
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            courseSchemaApplied = true;
        }
    }

    protected void wipeCourseModuleTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        for (String table : new String[]{
                "quiz_score_audit", "quiz_attempt_answer", "quiz_grade", "quiz_audit_log", "quiz_attempt",
                "quiz_question_option", "quiz_question", "quiz",
                "assignment_grade_release_recipient", "assignment_grade",
                "assignment_submission_file", "assignment_submission_staging_file",
                "assignment_submission_receipt", "assignment_submission_version", "assignment_submission",
                "assignment_rubric_version", "assignment_attachment", "assignment_audit_log", "assignment",
                "group_membership_audit", "group_membership", "course_group", "group_set",
                "course_audit_log", "enrollment_audit_log", "enrollment", "course"
        }) {
            try {
                jdbcTemplate.update("DELETE FROM `" + table + "`");
            } catch (Exception ignored) {
                // table may not exist yet on first boot race
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
    }

    protected void wipeIdempotencyRedisKeys() {
        try {
            Set<String> keys = stringRedisTemplate.keys("idempotency:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception ignored) {
        }
    }

    protected String loginUser(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    protected String loginAdmin(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\""
                                + AuthTestDataFactory.PASSWORD_PLAIN + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    protected int seedCourseWithInstructorAndStudent(User instructor, User student) {
        jdbcTemplate.update(
                "INSERT INTO course (tenant_id, course_code, title, term_start_date, term_end_date, "
                        + "instructor_id, state, creator_id, creator_actor_type, creator_actor_id, creator_role) "
                        + "VALUES (1,'TA-FLOW','TA Flow','2026-01-01','2026-12-31',?, 'Active', ?, 'USER', ?, 'USER')",
                instructor.getId(), instructor.getId(), instructor.getId());
        Integer courseId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM course", Integer.class);
        jdbcTemplate.update(
                "INSERT INTO enrollment (course_id, user_id, course_role, can_grade, can_post_announcements, "
                        + "can_manage_groups, can_manage_course_events, active, assignment_submit_frozen, enrolled_at) "
                        + "VALUES (?, ?, 'Instructor', 1, 1, 1, 1, 1, 0, UTC_TIMESTAMP())",
                courseId, instructor.getId());
        jdbcTemplate.update(
                "INSERT INTO enrollment (course_id, user_id, course_role, can_grade, can_post_announcements, "
                        + "can_manage_groups, can_manage_course_events, active, assignment_submit_frozen, enrolled_at) "
                        + "VALUES (?, ?, 'Student', 0, 0, 0, 0, 1, 0, UTC_TIMESTAMP())",
                courseId, student.getId());
        return courseId;
    }

    protected int seedPublishedAssignment(int courseId, int createdBy, int studentUserId) {
        jdbcTemplate.update(
                "INSERT INTO assignment (course_id, title, description, points_possible, due_at, "
                        + "allowed_file_types, max_file_size_bytes, max_file_count, state, created_by) "
                        + "VALUES (?, 'HW1', 'desc', 100, DATE_ADD(UTC_TIMESTAMP(), INTERVAL 7 DAY), "
                        + "CAST('[\"pdf\"]' AS JSON), 1048576, 1, 'Published', ?)",
                courseId, createdBy);
        Integer assignmentId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM assignment", Integer.class);
        jdbcTemplate.update(
                "INSERT INTO assignment_submission (assignment_id, owner_user_id) VALUES (?, ?)",
                assignmentId, studentUserId);
        Integer submissionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM assignment_submission", Integer.class);
        jdbcTemplate.update(
                "INSERT INTO assignment_submission_version (submission_id, assignment_id, owner_user_id, "
                        + "actual_submitter_user_id, version_no, submitted_at) VALUES (?, ?, ?, ?, 1, UTC_TIMESTAMP())",
                submissionId, assignmentId, studentUserId, studentUserId);
        Integer versionId = jdbcTemplate.queryForObject(
                "SELECT MAX(id) FROM assignment_submission_version", Integer.class);
        jdbcTemplate.update("UPDATE assignment_submission SET current_version_id=? WHERE id=?",
                versionId, submissionId);
        jdbcTemplate.update(
                "INSERT INTO assignment_grade (assignment_id, student_user_id, submission_version_id, score, status, "
                        + "entered_by, entered_at, edited_by, released_at) "
                        + "VALUES (?, ?, ?, 88.0, 'Released', ?, UTC_TIMESTAMP(), ?, UTC_TIMESTAMP())",
                assignmentId, studentUserId, versionId, createdBy, createdBy);
        return assignmentId;
    }

    protected int seedInProgressQuizAttempt(int courseId, int createdBy, int studentUserId) {
        jdbcTemplate.update(
                "INSERT INTO quiz (course_id, title, opens_at, closes_at, attempts_allowed, result_visibility, "
                        + "state, version, created_by, created_at, updated_at) VALUES ("
                        + "?, 'Q1', DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 1 DAY), "
                        + "DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 7 DAY), 3, 'AfterSubmit', 'Published', 1, ?, "
                        + "UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))",
                courseId, createdBy);
        Integer quizId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM quiz", Integer.class);
        jdbcTemplate.update(
                "INSERT INTO quiz_attempt (quiz_id, user_id, attempt_number, status, started_at, deadline_at, "
                        + "version, created_at, updated_at) VALUES (?, ?, 1, 'InProgress', UTC_TIMESTAMP(3), "
                        + "DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 1 HOUR), 1, UTC_TIMESTAMP(3), UTC_TIMESTAMP(3))",
                quizId, studentUserId);
        return quizId;
    }

    protected void seedGroupMembership(int courseId, int studentUserId, int addedBy) {
        jdbcTemplate.update(
                "INSERT INTO group_set (course_id, name, default_capacity, locked) VALUES (?, 'Set1', 5, 0)",
                courseId);
        Integer setId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM group_set", Integer.class);
        jdbcTemplate.update(
                "INSERT INTO course_group (group_set_id, course_id, name) VALUES (?, ?, 'G1')",
                setId, courseId);
        Integer groupId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM course_group", Integer.class);
        jdbcTemplate.update(
                "INSERT INTO group_membership (group_id, group_set_id, course_id, user_id, joined_at, "
                        + "added_by_type, added_by_user_id) VALUES (?, ?, ?, ?, UTC_TIMESTAMP(), 'Staff', ?)",
                groupId, setId, courseId, studentUserId, addedBy);
    }

    protected record Fixture(Admin admin, User instructor, User student, int courseId, String instructorToken) {
    }

    protected Fixture newFixture() throws Exception {
        Admin admin = dataFactory.createSystemAdmin(dataFactory.uniqueEmail("ta-adm"));
        User instructor = dataFactory.createInstructor(1, dataFactory.uniqueEmail("ta-ins"));
        User student = dataFactory.createStudent(1, dataFactory.uniqueEmail("ta-stu"));
        int courseId = seedCourseWithInstructorAndStudent(instructor, student);
        String token = loginUser(instructor.getEmail());
        return new Fixture(admin, instructor, student, courseId, token);
    }
}
