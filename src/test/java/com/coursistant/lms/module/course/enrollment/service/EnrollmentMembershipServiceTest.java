package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.course.service.CourseAuditActions;
import com.coursistant.lms.module.course.course.service.CourseAuditService;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.enrollment.dto.UpdateTaPermissionsRequest;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.group.service.GroupMembershipService;
import com.coursistant.lms.module.quiz.service.QuizLifecycleHooks;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentMembershipServiceTest {

    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private UserMapper userMapper;
    @Mock private TenantMapper tenantMapper;
    @Mock private CourseAuthorizationService courseAuthorizationService;
    @Mock private CourseAuditService courseAuditService;
    @Mock private EnrollmentBatchItemService enrollmentBatchItemService;
    @Mock private GroupMembershipService groupMembershipService;
    @Mock private QuizLifecycleHooks quizLifecycleHooks;

    @InjectMocks
    private EnrollmentMembershipService service;

    private Course course;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        course = new Course();
        course.setId(10);
        course.setTenantId(1);
        course.setState("Active");
        course.setInstructorId(7);
        tenant = new Tenant();
        tenant.setId(1);
        tenant.setStatus("ACTIVE");
    }

    @Test
    void addTa_reactivatesWithAllPermissionsFalse() {
        ActorContext actor = instructorActor();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(courseMapper.selectById(10)).thenReturn(course);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(tenant);

        User taUser = user(9, "INSTRUCTOR");
        when(userMapper.selectByIdForUpdate(9)).thenReturn(taUser);
        when(userMapper.selectById(9)).thenReturn(taUser);

        Enrollment inactive = enrollment(2, 9, "TA", false);
        inactive.setCanGrade(true);
        inactive.setCanPostAnnouncements(true);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9)).thenReturn(inactive);
        Enrollment after = enrollment(2, 9, "TA", true);
        after.setCanGrade(false);
        when(enrollmentMapper.selectById(2)).thenReturn(after);

        service.addTa(actor, 10, 9, "k");

        ArgumentCaptor<Enrollment> cap = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentMapper).updateById(cap.capture());
        Enrollment patch = cap.getValue();
        assertEquals(false, patch.getCanGrade());
        assertEquals(false, patch.getCanPostAnnouncements());
        assertEquals(false, patch.getCanManageGroups());
        assertEquals(false, patch.getCanManageCourseEvents());
        assertEquals(true, patch.getAssignmentSubmitFrozen());
        assertTrue(Boolean.TRUE.equals(patch.getClearWithdrawn()));
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.TA_REACTIVATED), any(), eq(9), any(), any(), eq("k"));
    }

    @Test
    void addTa_inactiveStudent_roleChanged() {
        ActorContext actor = instructorActor();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(courseMapper.selectById(10)).thenReturn(course);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(tenant);
        User u = user(9, "INSTRUCTOR");
        when(userMapper.selectByIdForUpdate(9)).thenReturn(u);
        when(userMapper.selectById(9)).thenReturn(u);
        Enrollment inactiveStudent = enrollment(3, 9, "Student", false);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9)).thenReturn(inactiveStudent);
        Enrollment after = enrollment(3, 9, "TA", true);
        when(enrollmentMapper.selectById(3)).thenReturn(after);

        service.addTa(actor, 10, 9, "k");
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.ENROLLMENT_ROLE_CHANGED), any(), eq(9), any(), any(), eq("k"));
    }

    @Test
    void addStudent_rejectsInactiveTaConversion() {
        ActorContext actor = instructorActor();
        when(courseMapper.selectById(10)).thenReturn(course);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(tenant);
        User student = user(3, "STUDENT");
        when(userMapper.selectByIdForUpdate(3)).thenReturn(student);
        Enrollment inactiveTa = enrollment(4, 3, "TA", false);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 3)).thenReturn(inactiveTa);

        ApiException ex = assertThrows(ApiException.class, () -> service.upsertStudentLocked(actor, 10, 3, "k"));
        assertEquals(ErrorType.CONFLICT, ex.getErrorType());
    }

    @Test
    void withdrawStudent_writesWithdrawnFieldsAndHooks() {
        ActorContext actor = instructorActor();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        User student = user(3, "STUDENT");
        when(userMapper.selectByIdForUpdate(3)).thenReturn(student);
        when(userMapper.selectById(3)).thenReturn(student);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        Enrollment active = enrollment(5, 3, "Student", true);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 3)).thenReturn(active);
        Enrollment after = enrollment(5, 3, "Student", false);
        after.setWithdrawnAt(java.time.LocalDateTime.now());
        when(enrollmentMapper.selectById(5)).thenReturn(after);

        service.withdrawStudent(actor, 10, 3, "k");

        ArgumentCaptor<Enrollment> cap = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentMapper).updateById(cap.capture());
        assertNotNull(cap.getValue().getWithdrawnAt());
        assertEquals(ActorContext.ACTOR_USER, cap.getValue().getWithdrawnByActorType());
        assertEquals(7, cap.getValue().getWithdrawnByActorId());
        verify(groupMembershipService).endGroupMembershipsOnEnrollmentDeactivated(eq(10), eq(3), any(), eq(7));
        verify(quizLifecycleHooks).onMembershipIneligible(10, 3);
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.STUDENT_WITHDRAWN), any(), eq(3), any(), any(), eq("k"));
    }

    @Test
    void batch_rejectsOver100() {
        ActorContext actor = instructorActor();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(courseMapper.selectById(10)).thenReturn(course);
        var ids = IntStream.rangeClosed(1, 101).boxed().toList();
        ApiException ex = assertThrows(ApiException.class,
                () -> service.batchAddStudents(actor, 10, ids, null, "k"));
        assertEquals(ErrorType.BAD_REQUEST, ex.getErrorType());
        verifyNoInteractions(enrollmentBatchItemService);
    }

    @Test
    void patchTa_nullKeepsExisting_andArchivedRejected() {
        ActorContext actor = instructorActor();
        Course archived = new Course();
        archived.setId(10);
        archived.setTenantId(1);
        archived.setState("Archived");
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(archived);
        when(courseMapper.selectById(10)).thenReturn(archived);
        ApiException ex = assertThrows(ApiException.class,
                () -> service.patchTaPermissions(actor, 10, 9, new UpdateTaPermissionsRequest(), "k"));
        assertEquals(ErrorType.COURSE_ARCHIVED, ex.getErrorType());
    }

    private ActorContext instructorActor() {
        return new ActorContext(ActorContext.ACTOR_USER, 7, RoleEnum.USER.name(), 1, "INSTRUCTOR", "ACTIVE");
    }

    private User user(int id, String level) {
        User u = new User();
        u.setId(id);
        u.setTenantId(1);
        u.setRole(RoleEnum.USER.name());
        u.setLevel(level);
        u.setStatus("ACTIVE");
        u.setName("n");
        u.setEmail(id + "@ex.com");
        return u;
    }

    private Enrollment enrollment(int id, int userId, String role, boolean active) {
        Enrollment e = new Enrollment();
        e.setId(id);
        e.setCourseId(10);
        e.setUserId(userId);
        e.setCourseRole(role);
        e.setActive(active);
        e.setCanGrade(false);
        e.setCanPostAnnouncements(false);
        e.setCanManageGroups(false);
        e.setCanManageCourseEvents(false);
        return e;
    }
}
