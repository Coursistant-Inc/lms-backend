package com.coursistant.lms.module.course.content;

import com.coursistant.lms.module.course.content.material.entity.CourseMaterial;
import com.coursistant.lms.module.course.content.week.entity.CourseWeek;
import com.coursistant.lms.module.course.content.week.repository.CourseWeekMapper;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseContentAccessServiceTest {

    @Mock private CourseMapper courseMapper;
    @Mock private CourseWeekMapper courseWeekMapper;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private CourseAuthorizationService courseAuthorizationService;
    @InjectMocks private CourseContentAccessService service;

    private Course course;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10);
        course.setTenantId(1);
        course.setState("Active");
    }

    @Test
    void taCanViewDraft_studentCannot() {
        when(courseAuthorizationService.requireVisibleCourse(any(), eq(10))).thenReturn(course);
        Enrollment ta = enrollment(CoursePermissionService.ROLE_TA);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 3)).thenReturn(ta);
        assertTrue(service.canViewDraftContent(user(3), 10));

        Enrollment student = enrollment(CoursePermissionService.ROLE_STUDENT);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 4)).thenReturn(student);
        assertFalse(service.canViewDraftContent(user(4), 10));
    }

    @Test
    void draftWeekHiddenFromStudentAsNotFound() {
        when(courseAuthorizationService.requireVisibleCourse(any(), eq(10))).thenReturn(course);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 4)).thenReturn(enrollment(CoursePermissionService.ROLE_STUDENT));
        CourseWeek draft = new CourseWeek();
        draft.setId(1);
        draft.setCourseId(10);
        draft.setState(CourseContentAccessService.WEEK_STATE_DRAFT);
        when(courseWeekMapper.selectById(1)).thenReturn(draft);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.requireWeekReadable(user(4), 10, 1));
        assertEquals(ErrorType.WEEK_NOT_FOUND, ex.getErrorType());
    }

    @Test
    void taCannotManageMaterial_butCanDeleteOwn() {
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(courseAuthorizationService.requireVisibleCourse(any(), eq(10))).thenReturn(course);
        when(courseAuthorizationService.isCourseManager(any(), eq(10))).thenReturn(false);
        when(enrollmentMapper.selectByCourseIdAndUserId(10, 3)).thenReturn(enrollment(CoursePermissionService.ROLE_TA));
        CourseWeek week = new CourseWeek();
        week.setId(1);
        week.setCourseId(10);
        when(courseWeekMapper.selectById(1)).thenReturn(week);

        CourseMaterial own = new CourseMaterial();
        own.setUploadedBy(3);
        service.requireMaterialDelete(user(3), 10, 1, own);

        CourseMaterial other = new CourseMaterial();
        other.setUploadedBy(99);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.requireMaterialDelete(user(3), 10, 1, other));
        assertEquals(ErrorType.FORBIDDEN, ex.getErrorType());
    }

    @Test
    void archivedCourseRejectsWrites() {
        course.setState("Archived");
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(courseAuthorizationService.requireCourseManager(any(), eq(10))).thenReturn(course);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.requireCourseManagerWritable(user(7), 10));
        assertEquals(ErrorType.COURSE_ARCHIVED, ex.getErrorType());
    }

    private static ActorContext user(int id) {
        return new ActorContext(ActorContext.ACTOR_USER, id, RoleEnum.USER.name(), 1, "INSTRUCTOR", "ACTIVE");
    }

    private static Enrollment enrollment(String role) {
        Enrollment e = new Enrollment();
        e.setCourseRole(role);
        e.setActive(true);
        e.setCanManageCourseEvents(false);
        return e;
    }
}
