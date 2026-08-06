package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseAuthorizationServiceTest {

    @Mock
    private CourseMapper courseMapper;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private CourseAuthorizationService authz;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10);
        course.setTenantId(1);
    }

    @Test
    void systemAdmin_seesAllTenants() {
        when(courseMapper.selectById(10)).thenReturn(course);
        ActorContext actor = admin(99);
        assertSame(course, authz.requireVisibleCourse(actor, 10));
    }

    @Test
    void tenantAdmin_ownTenant_ok_otherTenant_404() {
        when(courseMapper.selectById(10)).thenReturn(course);
        assertSame(course, authz.requireVisibleCourse(tenantAdmin(5, 1), 10));

        Course other = new Course();
        other.setId(11);
        other.setTenantId(2);
        when(courseMapper.selectById(11)).thenReturn(other);
        ApiException ex = assertThrows(ApiException.class, () -> authz.requireVisibleCourse(tenantAdmin(5, 1), 11));
        assertEquals(ErrorType.COURSE_NOT_FOUND, ex.getErrorType());
    }

    @Test
    void primaryInstructor_isManager_taIsNot() {
        when(courseMapper.selectById(10)).thenReturn(course);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 7)).thenReturn(enrollment(7, "Instructor", true));
        assertSame(course, authz.requireCourseManager(user(7, 1, "INSTRUCTOR"), 10));

        when(enrollmentMapper.selectByCourseIdAndUserId(10, 8)).thenReturn(enrollment(8, "TA", true));
        ApiException ex = assertThrows(ApiException.class,
                () -> authz.requireCourseManager(user(8, 1, "INSTRUCTOR"), 10));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
        assertFalse(authz.isCourseManager(user(8, 1, "INSTRUCTOR"), 10));
    }

    @Test
    void activeStudent_visible_inactive_notFound() {
        when(courseMapper.selectById(10)).thenReturn(course);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 3)).thenReturn(enrollment(3, "Student", true));
        assertSame(course, authz.requireVisibleCourse(user(3, 1, "STUDENT"), 10));

        when(enrollmentMapper.selectByCourseIdAndUserId(10, 4)).thenReturn(enrollment(4, "Student", false));
        ApiException ex = assertThrows(ApiException.class,
                () -> authz.requireVisibleCourse(user(4, 1, "STUDENT"), 10));
        assertEquals(ErrorType.COURSE_NOT_FOUND, ex.getErrorType());
    }

    @Test
    void noEnrollment_returnsCourseNotFound() {
        when(courseMapper.selectById(10)).thenReturn(course);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 3)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class,
                () -> authz.requireVisibleCourse(user(3, 1, "STUDENT"), 10));
        assertEquals(ErrorType.COURSE_NOT_FOUND, ex.getErrorType());
    }

    @Test
    void sameTenantUser_crossTenant_404() {
        User target = new User();
        target.setId(20);
        target.setTenantId(2);
        when(userMapper.selectById(20)).thenReturn(target);
        ApiException ex = assertThrows(ApiException.class, () -> authz.requireSameTenantUser(course, 20));
        assertEquals(ErrorType.COURSE_NOT_FOUND, ex.getErrorType());
    }

    @Test
    void taPermission_canGrade() {
        when(courseMapper.selectById(10)).thenReturn(course);
        Enrollment ta = enrollment(8, "TA", true);
        ta.setCanGrade(true);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 8)).thenReturn(ta);
        assertSame(ta, authz.requireTaPermission(user(8, 1, "INSTRUCTOR"), 10,
                CourseAuthorizationService.PERM_CAN_GRADE));
        ta.setCanGrade(false);
        assertThrows(ApiException.class, () -> authz.requireTaPermission(user(8, 1, "INSTRUCTOR"), 10,
                CourseAuthorizationService.PERM_CAN_GRADE));
    }

    private static ActorContext admin(int id) {
        return new ActorContext(ActorContext.ACTOR_ADMIN, id, RoleEnum.SYSTEM_ADMIN.name(), null, null, "ACTIVE");
    }

    private static ActorContext tenantAdmin(int id, int tenantId) {
        return new ActorContext(ActorContext.ACTOR_USER, id, RoleEnum.TENANT_ADMIN.name(), tenantId, "NOT_APPLICABLE", "ACTIVE");
    }

    private static ActorContext user(int id, int tenantId, String level) {
        return new ActorContext(ActorContext.ACTOR_USER, id, RoleEnum.USER.name(), tenantId, level, "ACTIVE");
    }

    private static Enrollment enrollment(int userId, String role, boolean active) {
        Enrollment e = new Enrollment();
        e.setCourseId(10);
        e.setUserId(userId);
        e.setCourseRole(role);
        e.setActive(active);
        return e;
    }
}
