package com.coursistant.lms.module.interaction.notification.email;

import com.coursistant.lms.module.interaction.notification.config.NotificationProperties;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationEmailTemplateFactoryTest {

    private NotificationEmailTemplateFactory factory;

    @BeforeEach
    void setUp() {
        NotificationProperties properties = new NotificationProperties();
        properties.getEmail().setBaseUrl("https://app.example.com");
        factory = new NotificationEmailTemplateFactory();
        org.springframework.test.util.ReflectionTestUtils.setField(factory, "notificationProperties", properties);
    }

    @Test
    void gradeTemplates_omitScoresAndContainCourseCode() {
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("courseCode", "CS101");
        vars.put("courseTitle", "Intro");
        vars.put("assignmentTitle", "HW1");
        vars.put("deepLink", "/courses/1/assignments/2/my-grade");
        RenderedEmail released = factory.renderImmediate(NotificationType.ASSIGNMENT_GRADE_RELEASED, vars);
        assertTrue(released.subject().startsWith("[CS101]"));
        assertFalse(released.textBody().contains("%"));
        assertFalse(released.subject().matches(".*\\d+\\.\\d+.*"));
        assertTrue(released.textBody().contains("CS101"));
        assertTrue(released.textBody().contains("https://app.example.com/courses/1/assignments/2/my-grade"));
    }

    @Test
    void submissionSubject_matchesSpec() {
        Map<String, String> vars = Map.of(
                "courseCode", "CS101",
                "assignmentTitle", "Essay",
                "submittedAt", "2026-08-16T10:00:00",
                "deepLink", "/x");
        RenderedEmail email = factory.renderImmediate(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED, vars);
        assertTrue(email.subject().contains("Submission received: Essay"));
    }

    @Test
    void crlf_isStrippedFromVariables() {
        Map<String, String> vars = Map.of(
                "courseCode", "CS\r\n101",
                "assignmentTitle", "HW",
                "deepLink", "/x");
        RenderedEmail email = factory.renderImmediate(NotificationType.ASSIGNMENT_GRADE_RELEASED, vars);
        assertFalse(email.subject().contains("\r"));
        assertFalse(email.subject().contains("\n"));
    }

    @Test
    void digest_groupsByCourse() {
        RenderedEmail email = factory.renderDigest(LocalDate.of(2026, 8, 16), List.of(
                new NotificationEmailTemplateFactory.DigestCourseGroup("CS101", "Intro", List.of("New assignment published: HW"))
        ));
        assertTrue(email.subject().contains("2026-08-16"));
        assertTrue(email.textBody().contains("CS101"));
    }
}
