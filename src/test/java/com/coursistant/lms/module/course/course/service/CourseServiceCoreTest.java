package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.course.dto.PatchCourseRequest;
import com.coursistant.lms.module.course.course.dto.ReassignPrimaryInstructorRequest;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseDependencyMapper;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceCoreTest {

    @Mock private CourseMapper courseMapper;
    @Mock private CourseDependencyMapper courseDependencyMapper;
    @Mock private EnrollmentMapper enrollmentMapper;
    @Mock private UserMapper userMapper;
    @Mock private TenantMapper tenantMapper;
    @Mock private EnrollmentService enrollmentService;
    @Mock private CourseAuthorizationService courseAuthorizationService;
    @Mock private CourseAuditService courseAuditService;
    @Mock private QuizLifecycleHooks quizLifecycleHooks;
    @Mock private com.coursistant.lms.module.course.group.service.GroupMembershipService groupMembershipService;

    @InjectMocks
    private CourseService courseService;

    private Tenant activeTenant;
    private User instructor;

    @BeforeEach
    void setUp() {
        activeTenant = new Tenant();
        activeTenant.setId(1);
        activeTenant.setStatus("ACTIVE");

        instructor = new User();
        instructor.setId(7);
        instructor.setTenantId(1);
        instructor.setRole(RoleEnum.USER.name());
        instructor.setLevel("INSTRUCTOR");
        instructor.setStatus("ACTIVE");
        instructor.setName("Inst");
        instructor.setEmail("i@example.com");
    }

    @Test
    void create_userInstructor_autoSelfPrimary() {
        when(tenantMapper.selectById(1)).thenReturn(activeTenant);
        when(userMapper.selectById(7)).thenReturn(instructor);
        when(courseMapper.insert(any())).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(100);
            return 1;
        });
        Course stored = baseCourse(100);
        when(courseMapper.selectById(100)).thenReturn(stored);

        CreateCourseRequest req = createRequest();
        req.setTenantId(1);
        req.setPrimaryInstructorUserId(99); // different → rejected

        ActorContext actor = userInstructor(7, 1);
        ApiException bad = assertThrows(ApiException.class, () -> courseService.create(actor, req, "k1"));
        assertEquals(ErrorType.BAD_REQUEST, bad.getErrorType());

        req.setPrimaryInstructorUserId(null);
        var resp = courseService.create(actor, req, "k2");
        assertEquals(100, resp.getId());

        ArgumentCaptor<Course> cap = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).insert(cap.capture());
        assertEquals(7, cap.getValue().getInstructorId());
        assertEquals(ActorContext.ACTOR_USER, cap.getValue().getCreatorActorType());
        verify(enrollmentService).createInstructorEnrollment(100, 7);
        verify(courseAuditService).write(eq(actor), eq(100), eq(1),
                eq(CourseAuditActions.COURSE_CREATED), any(), any(), isNull(), any(), eq("k2"));
    }

    @Test
    void create_tenantAdmin_requiresPrimaryInstructor() {
        when(tenantMapper.selectById(1)).thenReturn(activeTenant);
        when(userMapper.selectById(7)).thenReturn(instructor);
        when(courseMapper.insert(any())).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(101);
            return 1;
        });
        when(courseMapper.selectById(101)).thenReturn(baseCourse(101));

        CreateCourseRequest req = createRequest();
        ActorContext admin = tenantAdmin(5, 1);
        ApiException missing = assertThrows(ApiException.class, () -> courseService.create(admin, req, "k"));
        assertEquals(ErrorType.PARAM_MISSING, missing.getErrorType());

        req.setPrimaryInstructorUserId(7);
        courseService.create(admin, req, "k");
        verify(enrollmentService).createInstructorEnrollment(101, 7);
        ArgumentCaptor<Course> cap = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).insert(cap.capture());
        assertEquals(5, cap.getValue().getCreatorId());
        assertEquals(RoleEnum.TENANT_ADMIN.name(), cap.getValue().getCreatorRole());
    }

    @Test
    void create_systemAdmin_requiresTenantAndInstructor() {
        when(tenantMapper.selectById(2)).thenReturn(activeTenantWithId(2));
        User inst2 = copyInstructor(8, 2);
        when(userMapper.selectById(8)).thenReturn(inst2);
        when(courseMapper.insert(any())).thenAnswer(inv -> {
            Course c = inv.getArgument(0);
            c.setId(102);
            return 1;
        });
        Course stored = baseCourse(102);
        stored.setTenantId(2);
        stored.setInstructorId(8);
        when(courseMapper.selectById(102)).thenReturn(stored);

        CreateCourseRequest req = createRequest();
        ActorContext sys = systemAdmin(1);
        ApiException missingTenant = assertThrows(ApiException.class, () -> courseService.create(sys, req, "k"));
        assertEquals(ErrorType.PARAM_MISSING, missingTenant.getErrorType());

        req.setTenantId(2);
        req.setPrimaryInstructorUserId(8);
        courseService.create(sys, req, "k");
        ArgumentCaptor<Course> cap = ArgumentCaptor.forClass(Course.class);
        verify(courseMapper).insert(cap.capture());
        assertEquals(2, cap.getValue().getTenantId());
        assertEquals(8, cap.getValue().getInstructorId());
        assertEquals(8, cap.getValue().getCreatorId());
        assertEquals(ActorContext.ACTOR_ADMIN, cap.getValue().getCreatorActorType());
    }

    @Test
    void patch_rejectsTenantIdAndArchived() {
        Course active = baseCourse(10);
        when(courseAuthorizationService.requireCourseManager(any(), eq(10))).thenReturn(active);

        PatchCourseRequest withTenant = new PatchCourseRequest();
        withTenant.setTenantId(9);
        ApiException ex = assertThrows(ApiException.class,
                () -> courseService.patch(userInstructor(7, 1), 10, withTenant, "k"));
        assertEquals(ErrorType.BAD_REQUEST, ex.getErrorType());

        Course archived = baseCourse(11);
        archived.setState("Archived");
        when(courseAuthorizationService.requireCourseManager(any(), eq(11))).thenReturn(archived);
        ApiException archivedEx = assertThrows(ApiException.class,
                () -> courseService.patch(userInstructor(7, 1), 11, new PatchCourseRequest(), "k"));
        assertEquals(ErrorType.COURSE_ARCHIVED, archivedEx.getErrorType());
    }

    @Test
    void patch_clearDescription_andAudit() {
        Course existing = baseCourse(10);
        existing.setDescription("old");
        when(courseAuthorizationService.requireCourseManager(any(), eq(10))).thenReturn(existing);
        when(courseMapper.selectById(10)).thenReturn(existing);

        PatchCourseRequest req = new PatchCourseRequest();
        req.setClearDescription(true);
        courseService.patch(userInstructor(7, 1), 10, req, "k");
        verify(courseMapper).patchById(any(), eq(true), eq(false));
        verify(courseAuditService).write(any(), eq(10), eq(1),
                eq(CourseAuditActions.COURSE_UPDATED), any(), any(), any(), any(), eq("k"));
    }

    @Test
    void delete_nonEmpty_409() {
        Course existing = baseCourse(10);
        when(courseMapper.selectById(10)).thenReturn(existing);
        when(courseAuthorizationService.requireCourseManager(any(), eq(10))).thenReturn(existing);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(existing);
        CourseDependencyMapper.CourseDependencyCounts counts = new CourseDependencyMapper.CourseDependencyCounts();
        counts.setAssignments(1);
        when(courseDependencyMapper.countDependencies(10)).thenReturn(counts);

        ApiException ex = assertThrows(ApiException.class,
                () -> courseService.delete(userInstructor(7, 1), 10, "k"));
        assertEquals(ErrorType.CONFLICT, ex.getErrorType());
        verify(courseMapper, never()).deleteById(any());
    }

    @Test
    void delete_empty_removesInstructorAndCourse() {
        Course existing = baseCourse(10);
        when(courseMapper.selectById(10)).thenReturn(existing);
        when(courseAuthorizationService.requireCourseManager(any(), eq(10))).thenReturn(existing);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(existing);
        CourseDependencyMapper.CourseDependencyCounts empty = new CourseDependencyMapper.CourseDependencyCounts();
        when(courseDependencyMapper.countDependencies(10)).thenReturn(empty);
        Enrollment instructorEnroll = new Enrollment();
        instructorEnroll.setId(55);
        instructorEnroll.setUserId(7);
        instructorEnroll.setCourseRole("Instructor");
        instructorEnroll.setActive(true);
        when(enrollmentMapper.selectActiveInstructorByCourseIdForUpdate(10)).thenReturn(instructorEnroll);
        when(enrollmentMapper.countActiveInstructorsByCourseId(10)).thenReturn(1);

        courseService.delete(userInstructor(7, 1), 10, "k");
        verify(enrollmentMapper).deleteById(55);
        verify(courseMapper).deleteById(10);
        verify(courseAuditService).write(any(), eq(10), eq(1),
                eq(CourseAuditActions.COURSE_DELETED), any(), any(), any(), isNull(), eq("k"));
    }

    @Test
    void delete_missing_isIdempotent() {
        when(courseMapper.selectById(404)).thenReturn(null);
        assertDoesNotThrow(() -> courseService.delete(userInstructor(7, 1), 404, "k"));
        verify(courseAuthorizationService, never()).requireCourseManager(any(), any());
    }

    @Test
    void reassign_rejectsStudentTarget_andNonAdmin() {
        Course course = baseCourse(10);
        when(courseAuthorizationService.requireVisibleCourse(any(), eq(10))).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(activeTenant);

        User student = new User();
        student.setId(20);
        student.setTenantId(1);
        student.setRole(RoleEnum.USER.name());
        student.setLevel("STUDENT");
        student.setStatus("ACTIVE");
        when(userMapper.selectByIdForUpdate(20)).thenReturn(student);
        when(userMapper.selectById(20)).thenReturn(student);

        ReassignPrimaryInstructorRequest req = new ReassignPrimaryInstructorRequest();
        req.setPrimaryInstructorUserId(20);
        ApiException level = assertThrows(ApiException.class,
                () -> courseService.reassignPrimaryInstructor(tenantAdmin(5, 1), 10, req, "k"));
        assertEquals(ErrorType.LEVEL_ENROLLMENT_MISMATCH, level.getErrorType());

        ApiException forbidden = assertThrows(ApiException.class,
                () -> courseService.reassignPrimaryInstructor(userInstructor(7, 1), 10, req, "k"));
        assertEquals(ErrorType.FORBIDDEN, forbidden.getErrorType());
    }

    @Test
    void reassign_rejectsActiveTa() {
        Course course = baseCourse(10);
        when(courseAuthorizationService.requireVisibleCourse(any(), eq(10))).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(activeTenant);
        User target = copyInstructor(9, 1);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(target);
        when(userMapper.selectById(9)).thenReturn(target);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);

        Enrollment current = new Enrollment();
        current.setId(1);
        current.setUserId(7);
        current.setCourseRole("Instructor");
        current.setActive(true);
        when(enrollmentMapper.selectActiveInstructorByCourseIdForUpdate(10)).thenReturn(current);

        Enrollment ta = new Enrollment();
        ta.setId(2);
        ta.setUserId(9);
        ta.setCourseRole("TA");
        ta.setActive(true);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9)).thenReturn(ta);

        ReassignPrimaryInstructorRequest req = new ReassignPrimaryInstructorRequest();
        req.setPrimaryInstructorUserId(9);
        ApiException ex = assertThrows(ApiException.class,
                () -> courseService.reassignPrimaryInstructor(tenantAdmin(5, 1), 10, req, "k"));
        assertEquals(ErrorType.CONFLICT, ex.getErrorType());
        verify(enrollmentMapper, never()).updateById(any());
    }

    @Test
    void reassign_promotesInactiveEnrollment_andDeactivatesOldInstructor() {
        Course course = baseCourse(10);
        when(courseAuthorizationService.requireVisibleCourse(any(), eq(10))).thenReturn(course);
        when(tenantMapper.selectById(1)).thenReturn(activeTenant);
        User target = copyInstructor(9, 1);
        when(userMapper.selectByIdForUpdate(9)).thenReturn(target);
        when(userMapper.selectById(9)).thenReturn(target);
        when(courseMapper.selectByIdForUpdate(10)).thenReturn(course);
        when(courseMapper.selectById(10)).thenReturn(course);

        Enrollment current = new Enrollment();
        current.setId(1);
        current.setUserId(7);
        current.setCourseRole("Instructor");
        current.setActive(true);
        when(enrollmentMapper.selectActiveInstructorByCourseIdForUpdate(10)).thenReturn(current);

        Enrollment inactive = new Enrollment();
        inactive.setId(2);
        inactive.setUserId(9);
        inactive.setCourseRole("Student");
        inactive.setActive(false);
        when(enrollmentMapper.selectByCourseIdAndUserIdForUpdate(10, 9)).thenReturn(inactive);
        when(enrollmentMapper.countActiveInstructorsByCourseId(10)).thenReturn(1);

        ReassignPrimaryInstructorRequest req = new ReassignPrimaryInstructorRequest();
        req.setPrimaryInstructorUserId(9);
        courseService.reassignPrimaryInstructor(tenantAdmin(5, 1), 10, req, "k");

        verify(enrollmentMapper, atLeastOnce()).updateById(argThat(e ->
                e.getId() != null && e.getId() == 1 && Boolean.FALSE.equals(e.getActive())));
        verify(enrollmentMapper).updateById(argThat(e ->
                e.getId() != null && e.getId() == 2 && "Instructor".equals(e.getCourseRole())));
        verify(courseAuditService).write(any(), eq(10), eq(1),
                eq(CourseAuditActions.PRIMARY_INSTRUCTOR_REASSIGNED), any(), eq(9), any(), any(), eq("k"));
    }

    private CreateCourseRequest createRequest() {
        CreateCourseRequest req = new CreateCourseRequest();
        req.setCourseCode("CS101");
        req.setTitle("Intro");
        req.setTermStartDate(LocalDate.of(2026, 1, 1));
        req.setTermEndDate(LocalDate.of(2026, 6, 1));
        return req;
    }

    private Course baseCourse(int id) {
        Course c = new Course();
        c.setId(id);
        c.setTenantId(1);
        c.setCourseCode("CS101");
        c.setTitle("Intro");
        c.setInstructorId(7);
        c.setState("Active");
        c.setCreatorId(7);
        return c;
    }

    private Tenant activeTenantWithId(int id) {
        Tenant t = new Tenant();
        t.setId(id);
        t.setStatus("ACTIVE");
        return t;
    }

    private User copyInstructor(int id, int tenantId) {
        User u = new User();
        u.setId(id);
        u.setTenantId(tenantId);
        u.setRole(RoleEnum.USER.name());
        u.setLevel("INSTRUCTOR");
        u.setStatus("ACTIVE");
        return u;
    }

    private ActorContext userInstructor(int id, int tenantId) {
        return new ActorContext(ActorContext.ACTOR_USER, id, RoleEnum.USER.name(), tenantId, "INSTRUCTOR", "ACTIVE");
    }

    private ActorContext tenantAdmin(int id, int tenantId) {
        return new ActorContext(ActorContext.ACTOR_USER, id, RoleEnum.TENANT_ADMIN.name(), tenantId, "NOT_APPLICABLE", "ACTIVE");
    }

    private ActorContext systemAdmin(int id) {
        return new ActorContext(ActorContext.ACTOR_ADMIN, id, RoleEnum.SYSTEM_ADMIN.name(), null, null, "ACTIVE");
    }
}
