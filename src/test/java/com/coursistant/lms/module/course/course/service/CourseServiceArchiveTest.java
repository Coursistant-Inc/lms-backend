package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseDependencyMapper;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
import com.coursistant.lms.module.quiz.service.QuizLifecycleHooks;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceArchiveTest {

    @Mock private CourseMapper courseMapper;
    @Mock private CourseDependencyMapper courseDependencyMapper;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private TenantMapper tenantMapper;
    @Mock private EnrollmentService enrollmentService;
    @Mock private CourseAuthorizationService courseAuthorizationService;
    @Mock private CourseAuditService courseAuditService;
    @Mock private QuizLifecycleHooks quizLifecycleHooks;

    @InjectMocks
    private CourseService courseService;

    @Test
    void archive_idempotentAndWritesAudit() {
        Course active = course(10, "Active");
        Course archived = course(10, "Archived");
        ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 7, RoleEnum.USER.name(), 1, "INSTRUCTOR", "ACTIVE");
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(active);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(active);
        when(courseMapper.selectById(10)).thenReturn(archived);
        when(userMapper.selectById(7)).thenReturn(user(7));

        courseService.archive(actor, 10, "k");
        verify(courseMapper).archiveById(eq(10), any(), eq(ActorContext.ACTOR_USER), eq(7));
        verify(quizLifecycleHooks).onCourseArchived(10);
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.COURSE_ARCHIVED), any(), any(), any(), any(), eq("k"));

        when(courseMapper.selectByIdForUpdate(10)).thenReturn(archived);
        courseService.archive(actor, 10, "k2");
        verify(courseMapper, times(1)).archiveById(anyInt(), any(), any(), any());
    }

    @Test
    void unarchive_idempotent() {
        Course archived = course(10, "Archived");
        Course active = course(10, "Active");
        ActorContext actor = new ActorContext(ActorContext.ACTOR_USER, 7, RoleEnum.USER.name(), 1, "INSTRUCTOR", "ACTIVE");
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(archived);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(archived);
        when(courseMapper.selectById(10)).thenReturn(active);
        when(userMapper.selectById(7)).thenReturn(user(7));

        courseService.unarchive(actor, 10, "k");
        verify(courseMapper).unarchiveById(10);
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.COURSE_UNARCHIVED), any(), any(), any(), any(), eq("k"));
    }

    private Course course(int id, String state) {
        Course c = new Course();
        c.setId(id);
        c.setTenantId(1);
        c.setInstructorId(7);
        c.setTitle("T");
        c.setCourseCode("C");
        c.setState(state);
        return c;
    }

    private User user(int id) {
        User u = new User();
        u.setId(id);
        u.setName("n");
        u.setEmail("e@ex.com");
        return u;
    }
}
