package com.coursistant.lms.module.course.schedule.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.schedule.dto.CreateSessionRequest;
import com.coursistant.lms.module.course.schedule.dto.SessionResponse;
import com.coursistant.lms.module.course.schedule.dto.UpdateSessionRequest;
import com.coursistant.lms.module.course.schedule.entity.CourseSession;
import com.coursistant.lms.module.course.schedule.repository.CourseSessionMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import com.coursistant.lms.module.course.content.CourseContentAccessService;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CourseSessionService {

    private static final Set<String> TYPES = Set.of("Lecture", "Lab", "Tutorial");
    private static final Set<String> DAYS = Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    @Resource
    private CourseSessionMapper courseSessionMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private TenantTimezoneService tenantTimezoneService;

    @Resource
    private CourseContentAccessService courseContentAccessService;

    public List<SessionResponse> listByCourseId(Integer courseId) {
        requireCourse(courseId);
        return courseSessionMapper.selectByCourseId(courseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public SessionResponse getById(Integer courseId, Integer sessionId) {
        return toResponse(requireSessionInCourse(courseId, sessionId));
    }

    @Transactional
    public SessionResponse create(ActorContext actor, Integer courseId, CreateSessionRequest request) {
        courseContentAccessService.requireCourseManagerWritable(actor, courseId);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }
        validateRequired(request.getType(), request.getDayOfWeek(), request.getStartTime(),
                request.getEndTime(), request.getLocation());

        CourseSession session = new CourseSession();
        session.setCourseId(courseId);
        session.setType(request.getType().trim());
        session.setDayOfWeek(request.getDayOfWeek().trim().toUpperCase());
        session.setStartTime(request.getStartTime());
        session.setEndTime(request.getEndTime());
        session.setLocation(request.getLocation().trim());
        courseSessionMapper.insert(session);
        return toResponse(requireSessionInCourse(courseId, session.getId()));
    }

    @Transactional
    public SessionResponse update(ActorContext actor, Integer courseId, Integer sessionId, UpdateSessionRequest request) {
        courseContentAccessService.requireCourseManagerWritable(actor, courseId);
        CourseSession existing = requireSessionInCourse(courseId, sessionId);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }

        String type = request.getType() != null ? request.getType().trim() : existing.getType();
        String day = request.getDayOfWeek() != null
                ? request.getDayOfWeek().trim().toUpperCase()
                : existing.getDayOfWeek();
        LocalTime start = request.getStartTime() != null ? request.getStartTime() : existing.getStartTime();
        LocalTime end = request.getEndTime() != null ? request.getEndTime() : existing.getEndTime();
        String location = request.getLocation() != null ? request.getLocation().trim() : existing.getLocation();

        validateRequired(type, day, start, end, location);

        CourseSession patch = new CourseSession();
        patch.setId(sessionId);
        if (request.getType() != null) {
            patch.setType(type);
        }
        if (request.getDayOfWeek() != null) {
            patch.setDayOfWeek(day);
        }
        if (request.getStartTime() != null) {
            patch.setStartTime(start);
        }
        if (request.getEndTime() != null) {
            patch.setEndTime(end);
        }
        if (request.getLocation() != null) {
            if (location.isEmpty()) {
                throw new ApiException(ErrorType.BAD_REQUEST, "location must not be blank");
            }
            patch.setLocation(location);
        }
        // Ensure time consistency when only one side patched
        LocalTime effectiveStart = patch.getStartTime() != null ? patch.getStartTime() : existing.getStartTime();
        LocalTime effectiveEnd = patch.getEndTime() != null ? patch.getEndTime() : existing.getEndTime();
        if (!effectiveEnd.isAfter(effectiveStart)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "endTime must be after startTime");
        }

        courseSessionMapper.updateById(patch);
        return toResponse(requireSessionInCourse(courseId, sessionId));
    }

    @Transactional
    public void delete(ActorContext actor, Integer courseId, Integer sessionId) {
        courseContentAccessService.requireCourseManagerWritable(actor, courseId);
        requireSessionInCourse(courseId, sessionId);
        courseSessionMapper.deleteById(sessionId);
    }

    private void validateRequired(String type, String day, LocalTime start, LocalTime end, String location) {
        if (type == null || type.isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "type is required");
        }
        if (!TYPES.contains(type.trim())) {
            throw new ApiException(ErrorType.BAD_REQUEST, "type must be Lecture, Lab, or Tutorial");
        }
        if (day == null || day.isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "dayOfWeek is required");
        }
        String normalizedDay = day.trim().toUpperCase();
        if (!DAYS.contains(normalizedDay)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "dayOfWeek must be MON..SUN");
        }
        if (start == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "startTime is required");
        }
        if (end == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "endTime is required");
        }
        if (!end.isAfter(start)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "endTime must be after startTime");
        }
        if (location == null || location.isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "location is required");
        }
    }

    private static final String STATE_ARCHIVED = "Archived";

    private Course requireCourse(Integer courseId) {
        if (courseId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Course id is required");
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new ApiException(ErrorType.COURSE_NOT_FOUND);
        }
        return course;
    }

    private Course requireCourseWritable(Integer courseId) {
        Course course = requireCourse(courseId);
        if (STATE_ARCHIVED.equals(course.getState())) {
            throw new ApiException(ErrorType.COURSE_ARCHIVED);
        }
        return course;
    }

    private CourseSession requireSessionInCourse(Integer courseId, Integer sessionId) {
        requireCourse(courseId);
        if (sessionId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Session id is required");
        }
        CourseSession session = courseSessionMapper.selectById(sessionId);
        if (session == null || !courseId.equals(session.getCourseId())) {
            throw new ApiException(ErrorType.SESSION_NOT_FOUND);
        }
        return session;
    }

    private SessionResponse toResponse(CourseSession session) {
        SessionResponse response = new SessionResponse();
        response.setId(session.getId());
        response.setCourseId(session.getCourseId());
        response.setTimezone(tenantTimezoneService.requireTimezoneIdForCourse(session.getCourseId()));
        response.setType(session.getType());
        response.setDayOfWeek(session.getDayOfWeek());
        response.setStartTime(session.getStartTime());
        response.setEndTime(session.getEndTime());
        response.setLocation(session.getLocation());
        response.setCreatedAt(session.getCreatedAt());
        response.setUpdatedAt(session.getUpdatedAt());
        return response;
    }
}
