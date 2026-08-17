package com.coursistant.lms.module.interaction.notification.it;

import com.coursistant.lms.module.interaction.notification.it.support.NotificationPhase2SpringITBase;
import com.coursistant.lms.module.interaction.notification.service.NotificationAvailabilityChecker;
import com.coursistant.lms.module.interaction.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationAvailabilityIT extends NotificationPhase2SpringITBase {

    @Autowired private NotificationService notificationService;

    @Test
    void list_marksAvailabilityForPublishedDraftDropArchiveAndCrossCourse() {
        int instructorId = insertInstructor();
        int studentId = insertUser("av-s-" + uuid() + "@example.com", true, "ACTIVE");
        int otherStudent = insertUser("av-o-" + uuid() + "@example.com", true, "ACTIVE");
        int courseId = insertCourse(instructorId);
        int otherCourse = insertCourse(instructorId);
        enrollRole(courseId, instructorId, "Instructor");
        enrollStudent(courseId, studentId);
        enrollStudent(otherCourse, otherStudent);

        int publishedAssignment = insertAssignment(courseId, instructorId, "Published");
        int draftAssignment = insertAssignment(courseId, instructorId, "Draft");
        int otherAssignment = insertAssignment(otherCourse, instructorId, "Published");
        int announcementId = insertAnnouncement(courseId, instructorId);
        int weekId = insertWeek(courseId, "Published");
        int quizId = insertQuiz(courseId, instructorId, "Published", 1);
        int eventId = insertEvent(courseId);
        int groupSetId = insertGroupSet(courseId);
        int submissionId = insertSubmission(publishedAssignment, studentId);

        insertInApp(studentId, courseId, "ASSIGNMENT_PUBLISHED", "ASSIGNMENT", publishedAssignment, "a-pub");
        insertInApp(studentId, courseId, "ASSIGNMENT_PUBLISHED", "ASSIGNMENT", draftAssignment, "a-draft");
        insertInApp(studentId, courseId, "ASSIGNMENT_PUBLISHED", "ASSIGNMENT", otherAssignment, "a-cross");
        insertInApp(studentId, courseId, "ANNOUNCEMENT_POSTED", "ANNOUNCEMENT", announcementId, "ann");
        insertInApp(studentId, courseId, "WEEK_PUBLISHED", "WEEK", weekId, "week");
        insertInApp(studentId, courseId, "QUIZ_PUBLISHED", "QUIZ", quizId, "quiz");
        insertInApp(studentId, courseId, "COURSE_EVENT_CREATED", "COURSE_EVENT", eventId, "evt");
        insertInApp(studentId, courseId, "GROUP_MEMBER_ADDED", "GROUP_SET", groupSetId, "grp");
        insertInApp(studentId, courseId, "ASSIGNMENT_SUBMISSION_RECEIVED", "ASSIGNMENT_SUBMISSION",
                submissionId, "sub");
        insertInApp(studentId, courseId, "ASSIGNMENT_GRADE_RELEASED", "ASSIGNMENT_GRADE", publishedAssignment, "grade");

        var page = notificationService.list(studentId, 1, 50);
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "a-pub"));
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, availability(page, "a-draft"));
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, availability(page, "a-cross"));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "ann"));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "week"));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "quiz"));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "evt"));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "grp"));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "sub"));
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, availability(page, "grade"));

        jdbcTemplate.update("DELETE FROM course_announcement WHERE id = ?", announcementId);
        jdbcTemplate.update("UPDATE assignment SET state = 'Draft' WHERE id = ?", publishedAssignment);
        archiveCourse(courseId);
        page = notificationService.list(studentId, 1, 50);
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, availability(page, "ann"));
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, availability(page, "a-pub"));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, availability(page, "week"));

        deactivateEnrollment(courseId, studentId);
        page = notificationService.list(studentId, 1, 50);
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, availability(page, "week"));

        notificationService.markRead(studentId, notificationId(page, "week"));
    }

    private String availability(com.coursistant.lms.module.interaction.notification.dto.NotificationPageResponse page,
                                String eventKey) {
        return page.getItems().stream()
                .filter(item -> eventKey.equals(item.getDeepLink()))
                .findFirst()
                .orElseThrow()
                .getAvailability();
    }

    private int notificationId(com.coursistant.lms.module.interaction.notification.dto.NotificationPageResponse page,
                               String eventKey) {
        return page.getItems().stream()
                .filter(item -> eventKey.equals(item.getDeepLink()))
                .findFirst()
                .orElseThrow()
                .getNotificationId();
    }

    private void insertInApp(int userId, int courseId, String type, String subjectType, int subjectId, String key) {
        jdbcTemplate.update("""
                INSERT INTO user_notification (
                  tenant_id, recipient_user_id, course_id, notification_type, message, subject_type, subject_id,
                  event_key, deep_link, created_at)
                VALUES (1, ?, ?, ?, 'msg', ?, ?, ?, ?, UTC_TIMESTAMP(3))
                """, userId, courseId, type, subjectType, subjectId, key, key);
    }

    private int insertAnnouncement(int courseId, int authorId) {
        jdbcTemplate.update("""
                INSERT INTO course_announcement (course_id, title, body_html, author_user_id, author_name, posted_at)
                VALUES (?, 'Hello', 'Body', ?, 'Teacher', UTC_TIMESTAMP())
                """, courseId, authorId);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM course_announcement", Integer.class);
        return id == null ? -1 : id;
    }

    private int insertEvent(int courseId) {
        jdbcTemplate.update("""
                INSERT INTO course_event (course_id, name, event_date, start_time, end_time)
                VALUES (?, 'Lab', '2026-09-01', '10:00:00', '11:00:00')
                """, courseId);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM course_event", Integer.class);
        return id == null ? -1 : id;
    }

    private int insertGroupSet(int courseId) {
        jdbcTemplate.update("""
                INSERT INTO group_set (course_id, name, default_capacity, locked)
                VALUES (?, 'Set', 10, 0)
                """, courseId);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM group_set", Integer.class);
        return id == null ? -1 : id;
    }

    private int insertSubmission(int assignmentId, int ownerUserId) {
        jdbcTemplate.update("""
                INSERT INTO assignment_submission (assignment_id, owner_user_id)
                VALUES (?, ?)
                """, assignmentId, ownerUserId);
        Integer id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM assignment_submission", Integer.class);
        return id == null ? -1 : id;
    }
}
