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
    void resolveAllActiveMemberRecipients_includesStaffAndExcludesActor() {
        Course course = new Course();
        course.setId(1);
        course.setTenantId(1);
        when(courseMapper.selectById(1)).thenReturn(course);
        Enrollment instructor = new Enrollment();
        instructor.setUserId(9);
        Enrollment ta = new Enrollment();
        ta.setUserId(8);
        Enrollment student = new Enrollment();
        student.setUserId(10);
        when(enrollmentMapper.selectActiveByCourseId(1)).thenReturn(List.of(instructor, ta, student));
        User i = user(9, 1);
        User t = user(8, 1);
        User s = user(10, 1);
        when(userMapper.selectUsersByIds(List.of(8, 10))).thenReturn(List.of(t, s));

        assertEquals(List.of(8, 10), resolver.resolveAllActiveMemberRecipients(1, 9));
    }

    @Test
    void userActorId_onlyIncludesUserActors() {
        org.junit.jupiter.api.Assertions.assertNull(NotificationRecipientResolver.userActorId(null));
        org.junit.jupiter.api.Assertions.assertNull(NotificationRecipientResolver.userActorId(
                new com.coursistant.lms.shared.security.ActorContext(
                        com.coursistant.lms.shared.security.ActorContext.ACTOR_ADMIN, 9, "SYSTEM_ADMIN",
                        1, null, "ACTIVE")));
        org.junit.jupiter.api.Assertions.assertEquals(10, NotificationRecipientResolver.userActorId(
                new com.coursistant.lms.shared.security.ActorContext(
                        com.coursistant.lms.shared.security.ActorContext.ACTOR_USER, 10, "USER",
                        1, "STUDENT", "ACTIVE")));
    }

    @Test
    void resolveForType_providedRecipients_throws() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> resolver.resolveForType(
                        com.coursistant.lms.module.interaction.notification.enums.NotificationType.GROUP_MEMBER_ADDED,
                        1, 9));
    }

    private static User user(int id, int tenantId) {
        User user = new User();
        user.setId(id);
        user.setTenantId(tenantId);
        user.setStatus("ACTIVE");
        return user;
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
