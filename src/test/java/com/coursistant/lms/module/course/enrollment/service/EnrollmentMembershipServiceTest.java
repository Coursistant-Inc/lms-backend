package com.coursistant.lms.module.course.enrollment.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.course.service.CourseAuditActions;
import com.coursistant.lms.module.course.course.service.CourseAuditService;
import com.coursistant.lms.module.course.course.service.CourseAuthorizationService;
import com.coursistant.lms.module.course.enrollment.dto.UpdateTaPermissionsRequest;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.group.entity.GroupMembershipAudit;
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
import static org.mockito.Mockito.lenient;

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
    void addTa_promotesActiveStudent_withDefaultsAndHooks() {
        ActorContext actor = instructorActor();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(courseMapper.selectById(10)).thenReturn(course);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(tenant);
        User student = user(9, "STUDENT");
        when(userMapper.selectByIdForUpdate(9)).thenReturn(student);
        when(userMapper.selectById(9)).thenReturn(student);
        Enrollment activeStudent = enrollment(2, 9, "Student", true);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9)).thenReturn(activeStudent);
        Enrollment after = enrollment(2, 9, "TA", true);
        after.setCanGrade(false);
        after.setAssignmentSubmitFrozen(true);
        when(enrollmentMapper.selectById(2)).thenReturn(after);

        var resp = service.addTa(actor, 10, 9, "k");
        assertEquals("TA", resp.getCourseRole());

        ArgumentCaptor<Enrollment> cap = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentMapper).updateById(cap.capture());
        Enrollment patch = cap.getValue();
        assertEquals("TA", patch.getCourseRole());
        assertEquals(false, patch.getCanGrade());
        assertEquals(false, patch.getCanPostAnnouncements());
        assertEquals(false, patch.getCanManageGroups());
        assertEquals(false, patch.getCanManageCourseEvents());
        assertEquals(true, patch.getAssignmentSubmitFrozen());
        verify(groupMembershipService).endGroupMemberships(
                eq(10), eq(9), any(), eq(7), eq(GroupMembershipAudit.END_ON_TA_PROMOTION));
        verify(quizLifecycleHooks).onMembershipIneligible(10, 9);
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.TA_ADDED), any(), eq(9), any(), any(), eq("k"));
    }

    @Test
    void addTa_rejectsInstructorLevel() {
        ActorContext actor = instructorActor();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(courseMapper.selectById(10)).thenReturn(course);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(user(9, "INSTRUCTOR"));

        ApiException ex = assertThrows(ApiException.class, () -> service.addTa(actor, 10, 9, "k"));
        assertEquals(ErrorType.LEVEL_ENROLLMENT_MISMATCH, ex.getErrorType());
    }

    @Test
    void addTa_missingEnrollment_notFound() {
        ActorContext actor = instructorActor();
        stubWritableCourse(actor);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(user(9, "STUDENT"));
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class, () -> service.addTa(actor, 10, 9, "k"));
        assertEquals(ErrorType.ENROLLMENT_NOT_FOUND, ex.getErrorType());
    }

    @Test
    void addTa_inactiveEnrollment_notActive() {
        ActorContext actor = instructorActor();
        stubWritableCourse(actor);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(user(9, "STUDENT"));
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9))
                .thenReturn(enrollment(2, 9, "Student", false));

        ApiException ex = assertThrows(ApiException.class, () -> service.addTa(actor, 10, 9, "k"));
        assertEquals(ErrorType.ENROLLMENT_NOT_ACTIVE, ex.getErrorType());
    }

    @Test
    void addTa_idempotentActiveTa_noAudit() {
        ActorContext actor = instructorActor();
        stubWritableCourse(actor);
        User student = user(9, "STUDENT");
        when(userMapper.selectByIdForUpdate(9)).thenReturn(student);
        when(userMapper.selectById(9)).thenReturn(student);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9))
                .thenReturn(enrollment(2, 9, "TA", true));

        service.addTa(actor, 10, 9, "k");
        verify(enrollmentMapper, never()).updateById(any());
        verify(courseAuditService, never()).write(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void removeTa_demotesToActiveStudent_keepsFrozen() {
        ActorContext actor = actorManager();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        User student = user(9, "STUDENT");
        when(userMapper.selectByIdForUpdate(9)).thenReturn(student);
        when(userMapper.selectById(9)).thenReturn(student);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        Enrollment ta = enrollment(2, 9, "TA", true);
        ta.setAssignmentSubmitFrozen(true);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9)).thenReturn(ta);
        Enrollment after = enrollment(2, 9, "Student", true);
        after.setAssignmentSubmitFrozen(true);
        when(enrollmentMapper.selectById(2)).thenReturn(after);

        var resp = service.removeTa(actor, 10, 9, "k");
        assertEquals("Student", resp.getCourseRole());

        ArgumentCaptor<Enrollment> cap = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentMapper).updateById(cap.capture());
        assertEquals("Student", cap.getValue().getCourseRole());
        assertEquals(true, cap.getValue().getAssignmentSubmitFrozen());
        assertEquals(false, cap.getValue().getCanGrade());
        verify(groupMembershipService, never()).endGroupMembershipsOnEnrollmentDeactivated(any(), any(), any(), any());
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.TA_REMOVED), any(), eq(9), any(), any(), eq("k"));
    }

    @Test
    void removeTa_activeStudent_idempotentNoAudit() {
        ActorContext actor = actorManager();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(user(9, "STUDENT"));
        when(userMapper.selectById(9)).thenReturn(user(9, "STUDENT"));
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9))
                .thenReturn(enrollment(2, 9, "Student", true));

        service.removeTa(actor, 10, 9, "k");
        verify(enrollmentMapper, never()).updateById(any());
        verify(courseAuditService, never()).write(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void removeTa_activeInstructor_conflict() {
        ActorContext actor = actorManager();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(userMapper.selectByIdForUpdate(7)).thenReturn(user(7, "INSTRUCTOR"));
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 7))
                .thenReturn(enrollment(1, 7, "Instructor", true));

        ApiException ex = assertThrows(ApiException.class, () -> service.removeTa(actor, 10, 7, "k"));
        assertEquals(ErrorType.CONFLICT, ex.getErrorType());
    }

    @Test
    void removeTa_archived_rejected() {
        ActorContext actor = actorManager();
        Course archived = new Course();
        archived.setId(10);
        archived.setTenantId(1);
        archived.setState("Archived");
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(archived);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(user(9, "STUDENT"));
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(archived);

        ApiException ex = assertThrows(ApiException.class, () -> service.removeTa(actor, 10, 9, "k"));
        assertEquals(ErrorType.COURSE_ARCHIVED, ex.getErrorType());
    }

    @Test
    void addStudent_inactiveTa_restoresStudent_keepsFrozen() {
        ActorContext actor = actorManager();
        when(courseMapper.selectById(10)).thenReturn(course);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(tenant);
        User student = user(3, "STUDENT");
        when(userMapper.selectByIdForUpdate(3)).thenReturn(student);
        when(userMapper.selectById(3)).thenReturn(student);
        Enrollment inactiveTa = enrollment(4, 3, "TA", false);
        inactiveTa.setAssignmentSubmitFrozen(true);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 3)).thenReturn(inactiveTa);
        Enrollment after = enrollment(4, 3, "Student", true);
        after.setAssignmentSubmitFrozen(true);
        when(enrollmentMapper.selectById(4)).thenReturn(after);

        service.upsertStudentLocked(actor, 10, 3, "k");

        ArgumentCaptor<Enrollment> cap = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentMapper).updateById(cap.capture());
        assertEquals("Student", cap.getValue().getCourseRole());
        assertEquals(true, cap.getValue().getAssignmentSubmitFrozen());
        assertEquals(true, cap.getValue().getClearWithdrawn());
        verify(courseAuditService).write(eq(actor), eq(10), eq(1),
                eq(CourseAuditActions.ENROLLMENT_ROLE_CHANGED), any(), eq(3), any(), any(), eq("k"));
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
    void patchTa_archivedRejected() {
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

    @Test
    void addTa_groupEndThrows_rollsBackTransactionally_atServiceBoundary() {
        // Unit-level: when GroupMembershipService throws, update must not be committed by caller;
        // here we assert the exception propagates before audit (service is @Transactional in Spring).
        ActorContext actor = actorManager();
        stubWritableCourse(actor);
        User student = user(9, "STUDENT");
        when(userMapper.selectByIdForUpdate(9)).thenReturn(student);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9))
                .thenReturn(enrollment(2, 9, "Student", true));
        doThrow(new RuntimeException("boom")).when(groupMembershipService)
                .endGroupMemberships(eq(10), eq(9), any(), any(), eq(GroupMembershipAudit.END_ON_TA_PROMOTION));

        assertThrows(RuntimeException.class, () -> service.addTa(actor, 10, 9, "k"));
        verify(courseAuditService, never()).write(any(), any(), any(), eq(CourseAuditActions.TA_ADDED),
                any(), any(), any(), any(), any());
    }

    @Test
    void patchTa_targetTenantMismatch() {
        ActorContext actor = instructorActor();
        when(courseAuthorizationService.requireCourseManager(actor, 10)).thenReturn(course);
        when(courseMapper.selectById(10)).thenReturn(course);
        User otherTenant = user(9, "STUDENT");
        otherTenant.setTenantId(2);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(otherTenant);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.patchTaPermissions(actor, 10, 9, new UpdateTaPermissionsRequest(), "k"));
        assertEquals(ErrorType.TENANT_MISMATCH, ex.getErrorType());
    }

    private void stubWritableCourse(ActorContext actor) {
        when(courseAuthorizationService.requireCourseManager(eq(actor), eq(10))).thenReturn(course);
        lenient().when(courseMapper.selectById(10)).thenReturn(course);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        lenient().when(tenantMapper.selectById(1)).thenReturn(tenant);
    }

    private ActorContext actorManager() {
        return instructorActor();
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
        u.setEmail("u" + id + "@example.com");
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
