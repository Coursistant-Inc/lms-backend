package com.coursistant.lms.module.course.service;

import com.coursistant.lms.module.course.dto.CourseResponse;
import com.coursistant.lms.module.course.dto.CreateCourseRequest;
import com.coursistant.lms.module.course.dto.UpdateCourseRequest;
import com.coursistant.lms.module.course.entity.Course;
import com.coursistant.lms.module.course.repository.CourseMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class CourseService {

    private static final int DEFAULT_TENANT_ID = 1;
    private static final String STATE_ACTIVE = "Active";
    private static final String STATE_ARCHIVED = "Archived";

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private UserMapper userMapper;

    public CourseResponse create(Integer creatorId, CreateCourseRequest request) {
        validateCreate(request);
        requireUser(request.getInstructorId());

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
        return toResponse(requireCourse(course.getId()));
    }

    public CourseResponse getById(Integer id) {
        return toResponse(requireCourse(id));
    }

    public CourseResponse update(Integer id, UpdateCourseRequest request) {
        Course existing = requireCourse(id);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
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

        if (request.getInstructorId() != null) {
            requireUser(request.getInstructorId());
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
        patch.setInstructorId(request.getInstructorId());

        courseMapper.updateById(patch);
        return toResponse(requireCourse(id));
    }

    public void delete(Integer id) {
        requireCourse(id);
        try {
            courseMapper.deleteById(id);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(ErrorType.CONFLICT, "Course cannot be deleted because it is still referenced");
        }
    }

    public CourseResponse archive(Integer id) {
        Course course = requireCourse(id);
        if (STATE_ARCHIVED.equals(course.getState())) {
            return toResponse(course);
        }
        courseMapper.archiveById(id, LocalDateTime.now(ZoneOffset.UTC));
        return toResponse(requireCourse(id));
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

    private void requireUser(Integer userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.USER_NOT_FOUND);
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
