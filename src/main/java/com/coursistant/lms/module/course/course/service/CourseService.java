package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.dto.CoursePageResponse;
import com.coursistant.lms.module.course.course.dto.CourseResponse;
import com.coursistant.lms.module.course.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.course.dto.MyCoursePageResponse;
import com.coursistant.lms.module.course.course.dto.MyCourseResponse;
import com.coursistant.lms.module.course.course.dto.PatchCourseRequest;
import com.coursistant.lms.module.course.course.dto.PrimaryInstructorSummary;
import com.coursistant.lms.module.course.course.dto.ReassignPrimaryInstructorRequest;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseDependencyMapper;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
import com.coursistant.lms.module.quiz.service.QuizLifecycleHooks;
import com.coursistant.lms.module.tenant.entity.Tenant;
import com.coursistant.lms.module.tenant.repository.TenantMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.enums.RoleEnum;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final String STATE_ACTIVE = CourseLifecycleSupport.STATE_ACTIVE;
    private static final String STATE_ARCHIVED = CourseLifecycleSupport.STATE_ARCHIVED;
    private static final int COURSE_CODE_MAX = 32;

    @Resource
    private CourseMapper courseMapper;
    @Resource
    private CourseDependencyMapper courseDependencyMapper;
    @Resource
    private EnrollmentMapper enrollmentMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private TenantMapper tenantMapper;
    @Resource
    private EnrollmentService enrollmentService;
    @Resource
    private CourseAuthorizationService courseAuthorizationService;
    @Resource
    private CourseAuditService courseAuditService;
    @Resource
    private QuizLifecycleHooks quizLifecycleHooks;

    @Transactional
    public CourseResponse create(ActorContext actor, CreateCourseRequest request, String requestId) {
        validateCreateBasics(request);
        ResolvedCreate resolved = resolveCreateActors(actor, request);

        Course course = new Course();
        course.setTenantId(resolved.tenantId);
        course.setCourseCode(request.getCourseCode().trim());
        course.setTitle(request.getTitle().trim());
        course.setTermStartDate(request.getTermStartDate());
        course.setTermEndDate(request.getTermEndDate());
        course.setDescription(request.getDescription());
        course.setLocation(request.getLocation());
        course.setInstructorId(resolved.primaryInstructorUserId);
        course.setState(STATE_ACTIVE);
        course.setArchivedAt(null);
        // legacy creator_id FK → user table: prefer USER actor id, else instructor
        course.setCreatorId(resolved.legacyCreatorUserId);
        course.setCreatorActorType(actor.getActorType());
        course.setCreatorActorId(actor.getActorId());
        course.setCreatorRole(actor.getRole());

        courseMapper.insert(course);
        enrollmentService.createInstructorEnrollment(course.getId(), resolved.primaryInstructorUserId);

        Course created = requireCourse(course.getId());
        courseAuditService.write(actor, created.getId(), created.getTenantId(),
                CourseAuditActions.COURSE_CREATED, CourseAuditActions.TARGET_COURSE, created.getId(),
                null, snapshot(created), requestId);
        return toResponse(created);
    }

    public CourseResponse getById(Integer id) {
        return toResponse(requireCourse(id));
    }

    @Transactional
    public CourseResponse patch(ActorContext actor, Integer id, PatchCourseRequest request, String requestId) {
        Course existing = courseAuthorizationService.requireCourseManager(actor, id);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }
        if (request.getTenantId() != null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "tenantId cannot be changed");
        }
        if (request.getPrimaryInstructorUserId() != null || request.getInstructorId() != null) {
            throw new ApiException(ErrorType.BAD_REQUEST,
                    "primaryInstructorUserId cannot be changed via PATCH; use admin reassignment");
        }
        if (STATE_ARCHIVED.equals(existing.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }

        Map<String, Object> before = snapshot(existing);
        boolean clearDescription = Boolean.TRUE.equals(request.getClearDescription());
        boolean clearLocation = Boolean.TRUE.equals(request.getClearLocation());

        LocalDate start = request.getTermStartDate() != null
                ? request.getTermStartDate()
                : existing.getTermStartDate();
        LocalDate end = request.getTermEndDate() != null
                ? request.getTermEndDate()
                : existing.getTermEndDate();
        if (start != null && end != null && end.isBefore(start)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "termEndDate must be on or after termStartDate");
        }

        Course patch = new Course();
        patch.setId(id);
        if (request.getCourseCode() != null) {
            String code = request.getCourseCode().trim();
            if (code.isEmpty()) {
                throw new ApiException(ErrorType.BAD_REQUEST, "courseCode must not be blank");
            }
            if (code.length() > COURSE_CODE_MAX) {
                throw new ApiException(ErrorType.BAD_REQUEST, "courseCode must be at most 32 characters");
            }
            patch.setCourseCode(code);
        }
        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new ApiException(ErrorType.BAD_REQUEST, "title must not be blank");
            }
            patch.setTitle(title);
        }
        if (request.getTermStartDate() != null) {
            patch.setTermStartDate(request.getTermStartDate());
        }
        if (request.getTermEndDate() != null) {
            patch.setTermEndDate(request.getTermEndDate());
        }
        if (!clearDescription && request.getDescription() != null) {
            patch.setDescription(request.getDescription());
        }
        if (!clearLocation && request.getLocation() != null) {
            patch.setLocation(request.getLocation());
        }

        courseMapper.patchById(patch, clearDescription, clearLocation);
        Course after = requireCourse(id);
        courseAuditService.write(actor, id, after.getTenantId(),
                CourseAuditActions.COURSE_UPDATED, CourseAuditActions.TARGET_COURSE, id,
                before, snapshot(after), requestId);
        return toResponse(after);
    }

    @Transactional
    public void delete(ActorContext actor, Integer id, String requestId) {
        Course existing = courseMapper.selectById(id);
        if (existing == null) {
            return; // natural idempotency
        }
        courseAuthorizationService.requireCourseManager(actor, id);
        Course locked = courseMapper.selectByIdForUpdate(id);
        if (locked == null) {
            return;
        }
        CourseDependencyMapper.CourseDependencyCounts deps = courseDependencyMapper.countDependencies(id);
        if (deps != null && deps.hasAny()) {
            throw new ApiException(ErrorType.CONFLICT,
                    "Course is not empty; archive instead of delete");
        }
        Enrollment instructor = enrollmentMapper.selectActiveInstructorByCourseIdForUpdate(id);
        if (instructor == null || enrollmentMapper.countActiveInstructorsByCourseId(id) != 1) {
            throw new ApiException(ErrorType.CONFLICT,
                    "Course is not empty; archive instead of delete");
        }
        // re-check emptiness after lock
        deps = courseDependencyMapper.countDependencies(id);
        if (deps != null && deps.hasAny()) {
            throw new ApiException(ErrorType.CONFLICT,
                    "Course is not empty; archive instead of delete");
        }

        Map<String, Object> before = snapshot(locked);
        courseAuditService.write(actor, id, locked.getTenantId(),
                CourseAuditActions.COURSE_DELETED, CourseAuditActions.TARGET_COURSE, id,
                before, null, requestId);
        enrollmentMapper.deleteById(instructor.getId());
        try {
            courseMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorType.CONFLICT,
                    "Course is not empty; archive instead of delete");
        }
    }

    @Transactional
    public CourseResponse archive(ActorContext actor, Integer id, String requestId) {
        courseAuthorizationService.requireCourseManager(actor, id);
        Course locked = courseMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        if (STATE_ARCHIVED.equals(locked.getState())) {
            return toResponse(locked);
        }
        Map<String, Object> before = snapshot(locked);
        LocalDateTime archivedAt = LocalDateTime.now(ZoneOffset.UTC);
        courseMapper.archiveById(id, archivedAt, actor.getActorType(), actor.getActorId());
        quizLifecycleHooks.onCourseArchived(id);
        Course after = requireCourse(id);
        courseAuditService.write(actor, id, after.getTenantId(),
                CourseAuditActions.COURSE_ARCHIVED, CourseAuditActions.TARGET_COURSE, id,
                before, snapshot(after), requestId);
        return toResponse(after);
    }

    @Transactional
    public CourseResponse unarchive(ActorContext actor, Integer id, String requestId) {
        courseAuthorizationService.requireCourseManager(actor, id);
        Course locked = courseMapper.selectByIdForUpdate(id);
        if (locked == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        if (STATE_ACTIVE.equals(locked.getState())) {
            return toResponse(locked);
        }
        Map<String, Object> before = snapshot(locked);
        courseMapper.unarchiveById(id);
        Course after = requireCourse(id);
        courseAuditService.write(actor, id, after.getTenantId(),
                CourseAuditActions.COURSE_UNARCHIVED, CourseAuditActions.TARGET_COURSE, id,
                before, snapshot(after), requestId);
        return toResponse(after);
    }

    @Transactional
    public CourseResponse reassignPrimaryInstructor(ActorContext actor, Integer courseId,
                                                    ReassignPrimaryInstructorRequest request,
                                                    String requestId) {
        if (!actor.isSystemAdmin() && !actor.isTenantAdmin()) {
            throw new ApiException(ErrorType.FORBIDDEN);
        }
        Course visible = courseAuthorizationService.requireVisibleCourse(actor, courseId);
        if (STATE_ARCHIVED.equals(visible.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
        if (request == null || request.getPrimaryInstructorUserId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "primaryInstructorUserId is required");
        }
        Integer targetUserId = request.getPrimaryInstructorUserId();
        requireActiveTenant(visible.getTenantId());
        User target = requireEligiblePrimaryInstructor(targetUserId, visible.getTenantId());

        Course locked = courseMapper.selectByIdForUpdate(courseId);
        if (locked == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        Enrollment current = enrollmentMapper.selectActiveInstructorByCourseIdForUpdate(courseId);
        if (current == null) {
            throw new ApiException(ErrorType.CONFLICT, "Course has no active primary instructor");
        }
        Map<String, Object> before = new HashMap<>();
        before.put("course", snapshot(locked));
        before.put("primaryInstructorUserId", current.getUserId());

        if (current.getUserId().equals(targetUserId)) {
            return toResponse(locked);
        }

        Enrollment targetEnrollment = enrollmentMapper.selectByCourseIdAndUserIdForUpdate(courseId, targetUserId);
        if (targetEnrollment != null
                && CoursePermissionService.ROLE_STUDENT.equals(targetEnrollment.getCourseRole())
                && Boolean.TRUE.equals(targetEnrollment.getActive())) {
            throw new ApiException(ErrorType.CONFLICT,
                    "Cannot reassign to an active Student; withdraw Student enrollment first");
        }

        Enrollment deactivate = new Enrollment();
        deactivate.setId(current.getId());
        deactivate.setActive(false);
        enrollmentMapper.updateById(deactivate);

        if (targetEnrollment == null) {
            enrollmentService.createInstructorEnrollment(courseId, targetUserId);
        } else {
            Enrollment promote = new Enrollment();
            promote.setId(targetEnrollment.getId());
            promote.setCourseRole(CoursePermissionService.ROLE_INSTRUCTOR);
            promote.setCanGrade(true);
            promote.setCanPostAnnouncements(true);
            promote.setCanManageGroups(true);
            promote.setCanManageCourseEvents(true);
            promote.setActive(true);
            enrollmentMapper.updateById(promote);
        }

        if (enrollmentMapper.countActiveInstructorsByCourseId(courseId) != 1) {
            throw new ApiException(ErrorType.CONFLICT, "Primary instructor uniqueness violated");
        }

        Course patch = new Course();
        patch.setId(courseId);
        patch.setInstructorId(targetUserId);
        courseMapper.updateById(patch);

        Course after = requireCourse(courseId);
        Map<String, Object> afterSnap = new HashMap<>();
        afterSnap.put("course", snapshot(after));
        afterSnap.put("primaryInstructorUserId", targetUserId);
        afterSnap.put("previousPrimaryInstructorUserId", current.getUserId());
        courseAuditService.write(actor, courseId, after.getTenantId(),
                CourseAuditActions.PRIMARY_INSTRUCTOR_REASSIGNED,
                CourseAuditActions.TARGET_ENROLLMENT, targetUserId,
                before, afterSnap, requestId);
        return toResponse(after);
    }

    /**
     * Browse list scoped by {@link ActorContext}:
     * SYSTEM_ADMIN → all tenants (optional filterTenantId); TENANT_ADMIN → own tenant;
     * USER primary instructor → own courses. TA/Student cannot expand via this API.
     */
    public CoursePageResponse listForBrowse(ActorContext actor,
                                            String q,
                                            String state,
                                            Integer filterTenantId,
                                            Integer page,
                                            Integer size) {
        Integer scopeTenantId = null;
        Integer scopeInstructorId = null;
        if (actor.isSystemAdmin()) {
            scopeTenantId = filterTenantId;
        } else if (actor.isTenantAdmin()) {
            scopeTenantId = actor.getTenantId();
        } else if (actor.isUser()) {
            User user = userMapper.selectById(actor.getActorId());
            boolean platformInstructor = user != null
                    && LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(user.getLevel());
            boolean courseInstructor = enrollmentService.hasActiveInstructorEnrollment(actor.getActorId());
            if (!platformInstructor && !courseInstructor) {
                throw new ApiException(ErrorType.ACCESS_DENIED, "Course browse requires Admin or Instructor");
            }
            scopeInstructorId = actor.getActorId();
        } else {
            throw new ApiException(ErrorType.ACCESS_DENIED);
        }

        int pageNum = page == null || page < 0 ? 0 : page;
        int pageSize = normalizePageSize(size);

        String normalizedState = normalizeStateFilter(state);
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();

        long total = courseMapper.countForBrowse(normalizedQ, normalizedState, scopeTenantId, scopeInstructorId);
        List<CourseResponse> items = courseMapper
                .selectForBrowse(normalizedQ, normalizedState, scopeTenantId, scopeInstructorId,
                        pageNum * pageSize, pageSize)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        CoursePageResponse response = new CoursePageResponse();
        response.setItems(items);
        response.setPage(pageNum);
        response.setSize(pageSize);
        response.setTotal(total);
        return response;
    }

    public MyCoursePageResponse listMyCourses(ActorContext actor, String state, Integer page, Integer size) {
        if (!actor.isUser()) {
            throw new ApiException(ErrorType.FORBIDDEN, "Only USER accounts use /v2/me/courses");
        }
        Integer userId = actor.getActorId();
        Integer userTenantId = actor.getTenantId();
        String normalizedState = normalizeStateFilter(state);
        int pageNum = page == null || page < 0 ? 0 : page;
        int pageSize = normalizePageSize(size);

        List<Enrollment> enrollments = enrollmentMapper.selectActiveByUserId(userId);
        List<MyCourseResponse> all = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            Course course = courseMapper.selectById(enrollment.getCourseId());
            if (course == null) {
                continue;
            }
            if (course.getTenantId() == null || !userTenantId.equals(course.getTenantId())) {
                continue;
            }
            if (normalizedState != null && !normalizedState.equals(course.getState())) {
                continue;
            }
            all.add(toMyCourseResponse(enrollment, course));
        }
        all.sort((a, b) -> {
            LocalDateTime au = a.getUpdatedAt() != null ? a.getUpdatedAt() : LocalDateTime.MIN;
            LocalDateTime bu = b.getUpdatedAt() != null ? b.getUpdatedAt() : LocalDateTime.MIN;
            int cmp = bu.compareTo(au);
            return cmp != 0 ? cmp : Integer.compare(
                    b.getId() != null ? b.getId() : 0,
                    a.getId() != null ? a.getId() : 0);
        });

        long total = all.size();
        int from = Math.min(pageNum * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        MyCoursePageResponse response = new MyCoursePageResponse();
        response.setItems(all.subList(from, to));
        response.setPage(pageNum);
        response.setSize(pageSize);
        response.setTotal(total);
        return response;
    }

    private ResolvedCreate resolveCreateActors(ActorContext actor, CreateCourseRequest request) {
        Integer requestedInstructor = request.resolvePrimaryInstructorUserId();
        if (actor.isUser()
                && LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(actor.getUserLevel())) {
            requireActiveTenant(actor.getTenantId());
            if (request.getTenantId() != null && !request.getTenantId().equals(actor.getTenantId())) {
                throw new ApiException(ErrorType.TENANT_MISMATCH);
            }
            if (requestedInstructor != null && !requestedInstructor.equals(actor.getActorId())) {
                throw new ApiException(ErrorType.BAD_REQUEST,
                        "Instructor cannot assign a different primaryInstructorUserId");
            }
            requireEligiblePrimaryInstructor(actor.getActorId(), actor.getTenantId());
            return new ResolvedCreate(actor.getTenantId(), actor.getActorId(), actor.getActorId());
        }
        if (actor.isTenantAdmin()) {
            requireActiveTenant(actor.getTenantId());
            if (request.getTenantId() != null && !request.getTenantId().equals(actor.getTenantId())) {
                throw new ApiException(ErrorType.TENANT_MISMATCH);
            }
            if (requestedInstructor == null) {
                throw new ApiException(ErrorType.PARAM_MISSING, "primaryInstructorUserId is required");
            }
            requireEligiblePrimaryInstructor(requestedInstructor, actor.getTenantId());
            return new ResolvedCreate(actor.getTenantId(), requestedInstructor, actor.getActorId());
        }
        if (actor.isSystemAdmin()) {
            if (request.getTenantId() == null) {
                throw new ApiException(ErrorType.PARAM_MISSING, "tenantId is required");
            }
            if (requestedInstructor == null) {
                throw new ApiException(ErrorType.PARAM_MISSING, "primaryInstructorUserId is required");
            }
            requireActiveTenant(request.getTenantId());
            requireEligiblePrimaryInstructor(requestedInstructor, request.getTenantId());
            return new ResolvedCreate(request.getTenantId(), requestedInstructor, requestedInstructor);
        }
        throw new ApiException(ErrorType.FORBIDDEN, "Not allowed to create a course");
    }

    private User requireEligiblePrimaryInstructor(Integer userId, Integer tenantId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (!RoleEnum.USER.name().equals(user.getRole())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "primaryInstructor must have role USER");
        }
        if (!LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(user.getLevel())) {
            throw new ApiException(ErrorType.LEVEL_ENROLLMENT_MISMATCH);
        }
        if (user.getStatus() != null && !AccountStatus.ACTIVE.name().equals(user.getStatus())) {
            throw new ApiException(ErrorType.ACCOUNT_DISABLED);
        }
        if (user.getTenantId() == null || !user.getTenantId().equals(tenantId)) {
            throw new ApiException(ErrorType.TENANT_MISMATCH);
        }
        return user;
    }

    private void requireActiveTenant(Integer tenantId) {
        if (tenantId == null) {
            throw new ApiException(ErrorType.TENANT_NOT_FOUND);
        }
        Tenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ApiException(ErrorType.TENANT_NOT_FOUND);
        }
        if (tenant.getStatus() != null && !AccountStatus.ACTIVE.name().equals(tenant.getStatus())) {
            throw new ApiException(ErrorType.FORBIDDEN, "Tenant is not active");
        }
    }

    private void validateCreateBasics(CreateCourseRequest request) {
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }
        if (request.getCourseCode() == null || request.getCourseCode().isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "courseCode is required");
        }
        if (request.getCourseCode().trim().length() > COURSE_CODE_MAX) {
            throw new ApiException(ErrorType.BAD_REQUEST, "courseCode must be at most 32 characters");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "title is required");
        }
        if (request.getTermStartDate() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "termStartDate is required");
        }
        if (request.getTermEndDate() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "termEndDate is required");
        }
        if (request.getTermEndDate().isBefore(request.getTermStartDate())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "termEndDate must be on or after termStartDate");
        }
    }

    private Course requireCourse(Integer id) {
        if (id == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Course id is required");
        }
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return course;
    }

    private String normalizeStateFilter(String state) {
        if (state == null || state.isBlank()) {
            return null;
        }
        String normalized = state.trim();
        if (!STATE_ACTIVE.equals(normalized) && !STATE_ARCHIVED.equals(normalized)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "state must be Active or Archived");
        }
        return normalized;
    }

    private int normalizePageSize(Integer size) {
        int pageSize = size == null ? 20 : size;
        if (pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        return pageSize;
    }

    private Map<String, Object> snapshot(Course course) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", course.getId());
        map.put("tenantId", course.getTenantId());
        map.put("courseCode", course.getCourseCode());
        map.put("title", course.getTitle());
        map.put("termStartDate", course.getTermStartDate());
        map.put("termEndDate", course.getTermEndDate());
        map.put("description", course.getDescription());
        map.put("location", course.getLocation());
        map.put("instructorId", course.getInstructorId());
        map.put("state", course.getState());
        map.put("archivedAt", course.getArchivedAt());
        map.put("archivedByActorType", course.getArchivedByActorType());
        map.put("archivedByActorId", course.getArchivedByActorId());
        return map;
    }

    private CourseResponse toResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setCourseId(course.getId());
        response.setTenantId(course.getTenantId());
        response.setCourseCode(course.getCourseCode());
        response.setTitle(course.getTitle());
        response.setName(course.getTitle());
        response.setTermStartDate(course.getTermStartDate());
        response.setTermEndDate(course.getTermEndDate());
        response.setDescription(course.getDescription());
        response.setLocation(course.getLocation());
        response.setInstructorId(course.getInstructorId());
        response.setPrimaryInstructor(toPrimaryInstructorSummary(course.getInstructorId()));
        response.setState(course.getState());
        response.setStatus(course.getState());
        response.setArchivedAt(course.getArchivedAt());
        response.setGradingGraceEndsAt(CourseLifecycleSupport.gradingGraceEndsAt(course));
        response.setArchivedByActorType(course.getArchivedByActorType());
        response.setArchivedByActorId(course.getArchivedByActorId());
        response.setCreatorId(course.getCreatorId());
        response.setCreatorActorType(course.getCreatorActorType());
        response.setCreatorActorId(course.getCreatorActorId());
        response.setCreatorRole(course.getCreatorRole());
        response.setCreatedAt(course.getCreatedAt());
        response.setUpdatedAt(course.getUpdatedAt());
        return response;
    }

    private MyCourseResponse toMyCourseResponse(Enrollment enrollment, Course course) {
        MyCourseResponse response = new MyCourseResponse();
        response.setId(course.getId());
        response.setCourseId(course.getId());
        response.setCourseCode(course.getCourseCode());
        response.setTitle(course.getTitle());
        response.setName(course.getTitle());
        response.setDescription(course.getDescription());
        response.setTenantId(course.getTenantId());
        response.setState(course.getState());
        response.setStatus(course.getState());
        response.setCourseRole(enrollment.getCourseRole());
        response.setRole(enrollment.getCourseRole());
        if (CoursePermissionService.ROLE_TA.equals(enrollment.getCourseRole())) {
            response.setCanGrade(enrollment.getCanGrade());
            response.setCanPostAnnouncements(enrollment.getCanPostAnnouncements());
            response.setCanManageGroups(enrollment.getCanManageGroups());
            response.setCanManageCourseEvents(enrollment.getCanManageCourseEvents());
        }
        response.setPrimaryInstructor(toPrimaryInstructorSummary(course.getInstructorId()));
        response.setCreatedAt(course.getCreatedAt());
        response.setUpdatedAt(course.getUpdatedAt());
        response.setArchivedAt(course.getArchivedAt());
        return response;
    }

    private PrimaryInstructorSummary toPrimaryInstructorSummary(Integer userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            PrimaryInstructorSummary summary = new PrimaryInstructorSummary();
            summary.setUserId(userId);
            return summary;
        }
        PrimaryInstructorSummary summary = new PrimaryInstructorSummary();
        summary.setUserId(user.getId());
        summary.setName(user.getName());
        summary.setEmail(user.getEmail());
        return summary;
    }

    private record ResolvedCreate(Integer tenantId, Integer primaryInstructorUserId, Integer legacyCreatorUserId) {
    }
}
