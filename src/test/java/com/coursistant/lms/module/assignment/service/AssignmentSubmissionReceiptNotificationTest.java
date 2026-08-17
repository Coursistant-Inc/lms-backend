package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.module.assignment.entity.Assignment;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationDispatchPayload;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.interaction.notification.enums.RecipientMode;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.event.NotificationPublisher;
import com.coursistant.lms.module.interaction.notification.service.NotificationMessageFactory;
import com.coursistant.lms.module.interaction.notification.service.NotificationTimeSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentSubmissionReceiptNotificationTest {

    @Mock private CourseMapper courseMapper;
    @Mock private NotificationMessageFactory notificationMessageFactory;
    @Mock private NotificationPublisher notificationPublisher;
    @Mock private NotificationTimeSupport notificationTimeSupport;
    @InjectMocks private AssignmentNotificationService service;

    @Test
    void recordSubmissionReceived_keepsActorAndUsesVersionEventKey() {
        Assignment assignment = assignment();
        Course course = course();
        when(courseMapper.selectById(2)).thenReturn(course);
        when(notificationTimeSupport.nowUtc()).thenReturn(LocalDateTime.of(2026, 8, 16, 1, 0));
        when(notificationMessageFactory.submissionReceived(any(), any())).thenReturn("Submission received: HW");
        when(notificationPublisher.publishInTransaction(any(NotificationDispatchPayload.class))).thenReturn(11L);

        service.recordSubmissionReceived(assignment, List.of(50, 51), 88, 2, 9001,
                LocalDateTime.of(2026, 8, 16, 1, 0));

        ArgumentCaptor<NotificationDispatchPayload> captor =
                ArgumentCaptor.forClass(NotificationDispatchPayload.class);
        verify(notificationPublisher).publishInTransaction(captor.capture());
        NotificationDispatchPayload payload = captor.getValue();
        assertEquals(NotificationType.ASSIGNMENT_SUBMISSION_RECEIVED, payload.getNotificationType());
        assertEquals(SubjectType.ASSIGNMENT_SUBMISSION, payload.getSubjectType());
        assertEquals(88, payload.getSubjectId());
        assertEquals("submission:9001", payload.getEventKey());
        assertEquals(RecipientMode.EXPLICIT, payload.getRecipientMode());
        assertEquals(List.of(50, 51), payload.getRecipientIds());
        assertTrue(payload.getRecipientIds().contains(50));
        assertEquals("CS101", payload.getTemplateVars().get("courseCode"));
        assertEquals("2", payload.getTemplateVars().get("versionNo"));
    }

    @Test
    void submit_snapshotsGroupMembersInBusinessTransaction() throws Exception {
        String src = Files.readString(Path.of(
                "src/main/java/com/coursistant/lms/module/assignment/service/AssignmentSubmissionService.java"));
        int submit = src.indexOf("public SubmissionResponse submit(");
        int nextMethod = src.indexOf("public SubmissionResponse getMySubmission(", submit + 1);
        String body = src.substring(submit, nextMethod);
        assertTrue(body.contains("groupMembershipMapper.selectByGroupId(groupId)"));
        assertTrue(body.contains("recordSubmissionReceived("));
        assertTrue(!body.contains("notifyGroupSubmissionReplaced("));
        assertTrue(!body.contains("afterCommit("));
    }

    private Assignment assignment() {
        Assignment assignment = new Assignment();
        assignment.setId(9);
        assignment.setCourseId(2);
        assignment.setTitle("HW");
        return assignment;
    }

    private Course course() {
        Course course = new Course();
        course.setId(2);
        course.setTenantId(1);
        course.setCourseCode("CS101");
        course.setTitle("Intro");
        return course;
    }
}
