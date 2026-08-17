package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientResolverTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private NotificationRecipientResolver resolver;

    @Test
    void resolveActiveStudentRecipients_includesArchivedCourseStudents() {
        Course course = new Course();
        course.setId(1);
        course.setTenantId(1);
        course.setArchivedAt(LocalDateTime.now());
        when(courseMapper.selectById(1)).thenReturn(course);
        Enrollment student = new Enrollment();
        student.setUserId(10);
        when(enrollmentMapper.selectActiveStudentsByCourseId(1)).thenReturn(List.of(student));
        User ok = new User();
        ok.setId(10);
        ok.setTenantId(1);
        when(userMapper.selectUsersByIds(List.of(10))).thenReturn(List.of(ok));

        assertEquals(List.of(10), resolver.resolveActiveStudentRecipients(1));
    }

    @Test
    void filterCandidateRecipients_requiresActiveStudentAndMatchingTenant() {
        Course course = new Course();
        course.setId(1);
        course.setTenantId(1);
        course.setArchivedAt(null);

        Enrollment student = new Enrollment();
        student.setUserId(10);
        when(enrollmentMapper.selectActiveStudentsByCourseId(1)).thenReturn(List.of(student));

        User ok = new User();
        ok.setId(10);
        ok.setTenantId(1);
        User wrongTenant = new User();
        wrongTenant.setId(11);
        wrongTenant.setTenantId(2);
        when(userMapper.selectUsersByIds(List.of(10, 11, 12))).thenReturn(List.of(ok, wrongTenant));

        List<Integer> result = resolver.filterCandidateRecipients(course, List.of(10, 11, 12));
        assertEquals(List.of(10), result);
    }

    @Test
    void resolveActiveStudentRecipients_mapperThrows_propagates() {
        when(courseMapper.selectById(1)).thenThrow(new RuntimeException("db down"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> resolver.resolveActiveStudentRecipients(1));
        assertEquals("db down", ex.getMessage());
    }

    @Test
    void filterCandidateRecipients_mapperThrows_propagates() {
        Course course = new Course();
        course.setId(1);
        course.setTenantId(1);
        when(enrollmentMapper.selectActiveStudentsByCourseId(1)).thenThrow(new RuntimeException("db down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> resolver.filterCandidateRecipients(course, List.of(10)));
        assertEquals("db down", ex.getMessage());
    }
}
