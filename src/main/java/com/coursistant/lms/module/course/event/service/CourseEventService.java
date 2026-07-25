package com.coursistant.lms.module.course.event.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.event.dto.CourseEventResponse;
import com.coursistant.lms.module.course.event.dto.CreateCourseEventRequest;
import com.coursistant.lms.module.course.event.dto.UpdateCourseEventRequest;
import com.coursistant.lms.module.course.event.entity.CourseEvent;
import com.coursistant.lms.module.course.event.repository.CourseEventMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseEventService {

    @Resource
    private CourseEventMapper courseEventMapper;

    @Resource
    private CourseMapper courseMapper;

    public List<CourseEventResponse> listByCourseId(Integer courseId) {
        requireCourse(courseId);
        return courseEventMapper.selectByCourseId(courseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CourseEventResponse getById(Integer courseId, Integer eventId) {
        return toResponse(requireEventInCourse(courseId, eventId));
    }

    public CourseEventResponse create(Integer courseId, CreateCourseEventRequest request) {
        requireCourseWritable(courseId);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }
        validateRequired(request.getName(), request.getDate(), request.getStartTime(), request.getEndTime());

        CourseEvent event = new CourseEvent();
        event.setCourseId(courseId);
        event.setName(request.getName().trim());
        event.setEventDate(request.getDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setLocation(blankToNull(request.getLocation()));
        event.setDescription(blankToNull(request.getDescription()));
        courseEventMapper.insert(event);
        return toResponse(requireEventInCourse(courseId, event.getId()));
    }

    public CourseEventResponse update(Integer courseId, Integer eventId, UpdateCourseEventRequest request) {
        requireCourseWritable(courseId);
        CourseEvent existing = requireEventInCourse(courseId, eventId);
        if (request == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Request body is required");
        }

        String name = request.getName() != null ? request.getName().trim() : existing.getName();
        LocalDate date = request.getDate() != null ? request.getDate() : existing.getEventDate();
        LocalTime start = request.getStartTime() != null ? request.getStartTime() : existing.getStartTime();
        LocalTime end = request.getEndTime() != null ? request.getEndTime() : existing.getEndTime();
        validateRequired(name, date, start, end);

        CourseEvent patch = new CourseEvent();
        patch.setId(eventId);
        if (request.getName() != null) {
            if (name.isEmpty()) {
                throw new ApiException(ErrorType.BAD_REQUEST, "name must not be blank");
            }
            patch.setName(name);
        }
        if (request.getDate() != null) {
            patch.setEventDate(date);
        }
        if (request.getStartTime() != null) {
            patch.setStartTime(start);
        }
        if (request.getEndTime() != null) {
            patch.setEndTime(end);
        }
        if (request.getLocation() != null) {
            patch.setLocation(blankToNull(request.getLocation()));
        }
        if (request.getDescription() != null) {
            patch.setDescription(blankToNull(request.getDescription()));
        }

        LocalTime effectiveStart = patch.getStartTime() != null ? patch.getStartTime() : existing.getStartTime();
        LocalTime effectiveEnd = patch.getEndTime() != null ? patch.getEndTime() : existing.getEndTime();
        if (!effectiveEnd.isAfter(effectiveStart)) {
            throw new ApiException(ErrorType.BAD_REQUEST, "endTime must be after startTime");
        }

        courseEventMapper.updateById(patch);
        return toResponse(requireEventInCourse(courseId, eventId));
    }

    public void delete(Integer courseId, Integer eventId) {
        requireCourseWritable(courseId);
        requireEventInCourse(courseId, eventId);
        courseEventMapper.deleteById(eventId);
    }

    private void validateRequired(String name, LocalDate date, LocalTime start, LocalTime end) {
        if (name == null || name.isBlank()) {
            throw new ApiException(ErrorType.PARAM_MISSING, "name is required");
        }
        if (date == null) {
            throw new ApiException(ErrorType.PARAM_MISSING, "date is required");
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
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
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

    private CourseEvent requireEventInCourse(Integer courseId, Integer eventId) {
        requireCourse(courseId);
        if (eventId == null) {
            throw new ApiException(ErrorType.BAD_REQUEST, "Event id is required");
        }
        CourseEvent event = courseEventMapper.selectById(eventId);
        if (event == null || !courseId.equals(event.getCourseId())) {
            throw new ApiException(ErrorType.COURSE_EVENT_NOT_FOUND);
        }
        return event;
    }

    private CourseEventResponse toResponse(CourseEvent event) {
        CourseEventResponse response = new CourseEventResponse();
        response.setId(event.getId());
        response.setCourseId(event.getCourseId());
        response.setName(event.getName());
        response.setDate(event.getEventDate());
        response.setStartTime(event.getStartTime());
        response.setEndTime(event.getEndTime());
        response.setLocation(event.getLocation());
        response.setDescription(event.getDescription());
        response.setCreatedAt(event.getCreatedAt());
        response.setUpdatedAt(event.getUpdatedAt());
        return response;
    }
}
