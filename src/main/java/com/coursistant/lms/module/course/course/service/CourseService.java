package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.dto.CoursePageResponse;
import com.coursistant.lms.module.course.course.dto.CourseResponse;
import com.coursistant.lms.module.course.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.course.dto.TransferInstructorRequest;
import com.coursistant.lms.module.course.course.dto.UpdateCourseRequest;
import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.course.enrollment.service.EnrollmentService;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.LevelEnum;
import jakarta.annotation.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final int DEFAULT_TENANT_ID = 1;
    private static final String STATE_ACTIVE = "Active";
    private static final String STATE_ARCHIVED = "Archived";

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private EnrollmentService enrollmentService;

    @Resource
    private CoursePermissionService coursePermissionService;

    @Transactional
    public CourseResponse create(Integer creatorId, CreateCourseRequest request) {
        validateCreate(request);
        requireInstructorLevel(creatorId);
        requireInstructorLevel(request.getInstructorId());

        Course course = new Course();
        course.setTenantId(request.getTenantId() != null ? request.getTenantId() : DEFAULT_TENANT_ID);
        course.setCourseCode(request.getCourseCode().trim());
        course.setTitle(request.getTitle().trim());
        course.setTermStartDate(request.getTermStartDate());
        course.setTermEndDate(request.getTermEndDate());
        course.setDescription(request.getDescription());
        course.setLocation(request.getLocation());
        course.setInstructorId(request.getInstructorId());
        course.setState(STATE_ACTIVE);
        course.setArchivedAt(null);
        course.setCreatorId(creatorId);

        courseMapper.insert(course);
        enrollmentService.createInstructorEnrollment(course.getId(), request.getInstructorId());
        return toResponse(requireCourse(course.getId()));
    }

    public CourseResponse getById(Integer id) {
        return toResponse(requireCourse(id));
    }

    public CourseResponse update(Integer callerUserId, Integer id, UpdateCourseRequest request) {
        coursePermissionService.requireInstructor(id, callerUserId);
        Course existing = requireCourse(id);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }
        if (request.getInstructorId() != null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "instructorId cannot be changed via this API");
        }

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
            patch.setCourseCode(code);
        }
        if (request.getTitle() != null) {
            String title = request.getTitle().trim();
            if (title.isEmpty()) {
                throw new ApiException(ErrorType.BAD_REQUEST, "title must not be blank");
            }
            patch.setTitle(title);
        }
        patch.setTermStartDate(request.getTermStartDate());
        patch.setTermEndDate(request.getTermEndDate());
        patch.setDescription(request.getDescription());
        patch.setLocation(request.getLocation());

        courseMapper.updateById(patch);
        return toResponse(requireCourse(id));
    }

    public void delete(Integer callerUserId, Integer id) {
        coursePermissionService.requireInstructor(id, callerUserId);
        requireCourse(id);
        if (enrollmentService.countByCourseId(id) > 0) {
            throw new ApiException(ErrorType.CONFLICT, "Course cannot be deleted while it still has enrollments");
        }
        try {
            courseMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorType.CONFLICT, "Course cannot be deleted because it is still referenced");
        }
    }

    public CourseResponse archive(Integer callerUserId, Integer id) {
        coursePermissionService.requireInstructor(id, callerUserId);
        Course course = requireCourse(id);
        if (STATE_ARCHIVED.equals(course.getState())) {
            return toResponse(course);
        }
        courseMapper.archiveById(id, LocalDateTime.now(ZoneOffset.UTC));
        return toResponse(requireCourse(id));
    }

    public CourseResponse unarchive(Integer callerUserId, Integer id) {
        coursePermissionService.requireInstructor(id, callerUserId);
        Course course = requireCourse(id);
        if (STATE_ACTIVE.equals(course.getState())) {
            return toResponse(course);
        }
        courseMapper.unarchiveById(id);
        return toResponse(requireCourse(id));
    }

    @Transactional
    public CourseResponse transferInstructor(Integer callerUserId, Integer id, TransferInstructorRequest request) {
        coursePermissionService.requireInstructor(id, callerUserId);
        Course course = requireCourse(id);
        if (STATE_ARCHIVED.equals(course.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
        if (request == null || request.getNewInstructorId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "newInstructorId is required");
        }
        Integer newInstructorId = request.getNewInstructorId();
        enrollmentService.transferInstructorRole(id, callerUserId, newInstructorId, callerUserId);

        Course patch = new Course();
        patch.setId(id);
        patch.setInstructorId(newInstructorId);
        courseMapper.updateById(patch);
        return toResponse(requireCourse(id));
    }

    public CoursePageResponse listForBrowse(boolean admin,
                                            Integer callerUserId,
                                            String q,
                                            String state,
                                            Integer page,
                                            Integer size) {
        if (!admin) {
            User user = userMapper.selectById(callerUserId);
            boolean platformInstructor = user != null
                    && LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(user.getLevel());
            boolean courseInstructor = enrollmentService.hasActiveInstructorEnrollment(callerUserId);
            if (!platformInstructor && !courseInstructor) {
                throw new ApiException(ErrorType.ACCESS_DENIED, "Course browse requires Admin or Instructor");
            }
        }

        int pageNum = page == null || page < 0 ? 0 : page;
        int pageSize = size == null ? 20 : size;
        if (pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }

        String normalizedState = null;
        if (state != null && !state.isBlank()) {
            normalizedState = state.trim();
            if (!STATE_ACTIVE.equals(normalizedState) && !STATE_ARCHIVED.equals(normalizedState)) {
                throw new ApiException(ErrorType.BAD_REQUEST, "state must be Active or Archived");
            }
        }
        String normalizedQ = (q == null || q.isBlank()) ? null : q.trim();
        Integer scopeInstructorId = admin ? null : callerUserId;

        long total = courseMapper.countForBrowse(normalizedQ, normalizedState, scopeInstructorId);
        List<CourseResponse> items = courseMapper
                .selectForBrowse(normalizedQ, normalizedState, scopeInstructorId, pageNum * pageSize, pageSize)
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

    private void validateCreate(CreateCourseRequest request) {
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }
        if (request.getCourseCode() == null || request.getCourseCode().isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "courseCode is required");
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
        if (request.getInstructorId() == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "instructorId is required");
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

    /** Platform level must be INSTRUCTOR (not course Enrollment role). */
    private void requireInstructorLevel(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
        }
        if (!LevelEnum.INSTRUCTOR.level.equalsIgnoreCase(user.getLevel())) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Only users with level INSTRUCTOR can create a course");
        }
    }

    private CourseResponse toResponse(Course course) {
        CourseResponse response = new CourseResponse();
        response.setId(course.getId());
        response.setTenantId(course.getTenantId());
        response.setCourseCode(course.getCourseCode());
        response.setTitle(course.getTitle());
        response.setTermStartDate(course.getTermStartDate());
        response.setTermEndDate(course.getTermEndDate());
        response.setDescription(course.getDescription());
        response.setLocation(course.getLocation());
        response.setInstructorId(course.getInstructorId());
        response.setState(course.getState());
        response.setArchivedAt(course.getArchivedAt());
        response.setCreatorId(course.getCreatorId());
        response.setCreatedAt(course.getCreatedAt());
        response.setUpdatedAt(course.getUpdatedAt());
        return response;
    }
}
