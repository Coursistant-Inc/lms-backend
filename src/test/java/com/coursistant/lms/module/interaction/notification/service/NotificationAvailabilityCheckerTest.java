package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.interaction.notification.dto.NotificationResponse;
import com.coursistant.lms.module.interaction.notification.dto.NotificationSubjectRef;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationAvailabilityMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationAvailabilityCheckerTest {

    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private NotificationAvailabilityMapper availabilityMapper;
    @InjectMocks private NotificationAvailabilityChecker checker;

    @Test
    void studentSeesPublishedAssignment_butNotDraft() {
        when(enrollmentMapper.selectActiveByUserId(10)).thenReturn(List.of(enrollment(2, "Student")));
        NotificationSubjectRef published = ref(5, 2, "Published");
        NotificationSubjectRef draft = ref(6, 2, "Draft");
        when(availabilityMapper.selectAssignments(anyList())).thenReturn(List.of(published, draft));

        NotificationResponse a = item(2, SubjectType.ASSIGNMENT.name(), 5);
        NotificationResponse b = item(2, SubjectType.ASSIGNMENT.name(), 6);
        checker.fill(10, List.of(a, b));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, a.getAvailability());
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, b.getAvailability());
    }

    @Test
    void staffSeesDraftWeek() {
        when(enrollmentMapper.selectActiveByUserId(9)).thenReturn(List.of(enrollment(2, "Instructor")));
        when(availabilityMapper.selectWeeks(anyList())).thenReturn(List.of(ref(3, 2, "Draft")));
        NotificationResponse item = item(2, SubjectType.WEEK.name(), 3);
        checker.fill(9, List.of(item));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, item.getAvailability());
    }

    @Test
    void droppedStudent_isUnavailable() {
        when(enrollmentMapper.selectActiveByUserId(10)).thenReturn(List.of());
        NotificationResponse item = item(2, SubjectType.ANNOUNCEMENT.name(), 1);
        checker.fill(10, List.of(item));
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, item.getAvailability());
    }

    @Test
    void crossCourseId_isUnavailable() {
        when(enrollmentMapper.selectActiveByUserId(10)).thenReturn(List.of(enrollment(2, "Student")));
        when(availabilityMapper.selectAnnouncements(anyList())).thenReturn(List.of(ref(1, 99, null)));
        NotificationResponse item = item(2, SubjectType.ANNOUNCEMENT.name(), 1);
        checker.fill(10, List.of(item));
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, item.getAvailability());
    }

    @Test
    void unknownSubjectType_failClosed() {
        when(enrollmentMapper.selectActiveByUserId(10)).thenReturn(List.of(enrollment(2, "Student")));
        NotificationResponse item = item(2, SubjectType.ASSIGNMENT_GRADE.name(), 8);
        checker.fill(10, List.of(item));
        assertEquals(NotificationAvailabilityChecker.NO_LONGER_AVAILABLE, item.getAvailability());
    }

    @Test
    void submissionOwner_isAvailable() {
        when(enrollmentMapper.selectActiveByUserId(10)).thenReturn(List.of(enrollment(2, "Student")));
        NotificationSubjectRef sub = ref(8, 2, "Published");
        sub.setOwnerUserId(10);
        when(availabilityMapper.selectSubmissions(anyList())).thenReturn(List.of(sub));
        NotificationResponse item = item(2, SubjectType.ASSIGNMENT_SUBMISSION.name(), 8);
        checker.fill(10, List.of(item));
        assertEquals(NotificationAvailabilityChecker.AVAILABLE, item.getAvailability());
    }

    private Enrollment enrollment(int courseId, String role) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourseId(courseId);
        enrollment.setCourseRole(role);
        enrollment.setActive(true);
        return enrollment;
    }

    private NotificationSubjectRef ref(int id, int courseId, String state) {
        NotificationSubjectRef ref = new NotificationSubjectRef();
        ref.setId(id);
        ref.setCourseId(courseId);
        ref.setState(state);
        return ref;
    }

    private NotificationResponse item(int courseId, String subjectType, int subjectId) {
        NotificationResponse item = new NotificationResponse();
        item.setCourseId(courseId);
        item.setSubjectType(subjectType);
        item.setSubjectId(subjectId);
        return item;
    }
}
