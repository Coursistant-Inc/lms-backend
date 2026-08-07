package com.coursistant.lms.module.course.event.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.event.dto.CourseEventResponse;
import com.coursistant.lms.module.course.event.dto.CreateCourseEventRequest;
import com.coursistant.lms.module.course.event.dto.UpcomingCourseActivityResponse;
import com.coursistant.lms.module.course.event.dto.UpdateCourseEventRequest;
import com.coursistant.lms.module.course.event.entity.CourseEvent;
import com.coursistant.lms.module.course.event.repository.CourseEventMapper;
import com.coursistant.lms.module.course.schedule.dto.SessionWithCourseCode;
import com.coursistant.lms.module.course.schedule.repository.CourseSessionMapper;
import com.coursistant.lms.module.tenant.service.TenantTimezoneService;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.module.course.content.CourseContentAccessService;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.security.ActorContext;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CourseEventService {

    private static final Logger log = LoggerFactory.getLogger(CourseEventService.class);

    private static final Map<DayOfWeek, String> DAY_CODES = new EnumMap<>(DayOfWeek.class);

    static {
        DAY_CODES.put(DayOfWeek.MONDAY, "MON");
        DAY_CODES.put(DayOfWeek.TUESDAY, "TUE");
        DAY_CODES.put(DayOfWeek.WEDNESDAY, "WED");
        DAY_CODES.put(DayOfWeek.THURSDAY, "THU");
        DAY_CODES.put(DayOfWeek.FRIDAY, "FRI");
        DAY_CODES.put(DayOfWeek.SATURDAY, "SAT");
        DAY_CODES.put(DayOfWeek.SUNDAY, "SUN");
    }

    @Resource
    private CourseEventMapper courseEventMapper;

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private CourseSessionMapper courseSessionMapper;

    @Resource
    private TenantTimezoneService tenantTimezoneService;

    @Resource
    private CourseContentAccessService courseContentAccessService;

    public List<CourseEventResponse> listByCourseId(Integer courseId) {
        requireCourse(courseId);
        return courseEventMapper.selectByCourseId(courseId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Dashboard activities: expanded Course Sessions + Course Events for active enrollments
     * in {@code [today, today+days-1]} using the user's tenant timezone calendar date.
     * Finished sessions today are still included.
     */
    public List<UpcomingCourseActivityResponse> listUpcomingActivitiesForUser(Integer userId, Integer days) {
        if (userId == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        Integer userTenantId = tenantTimezoneService.requireUserTenantId(userId);
        ZoneId zone = tenantTimezoneService.requireZoneForUser(userId);
        int windowDays = normalizeDays(days, 7, 30);
        String timezoneId = zone.getId();
        LocalDate from = LocalDate.now(zone);
        LocalDate to = from.plusDays(windowDays - 1L);

        List<UpcomingCourseActivityResponse> result = new ArrayList<>();

        List<UpcomingCourseActivityResponse> events =
                courseEventMapper.selectUpcomingActivitiesForUser(userId, from, to);
        if (events != null) {
            for (UpcomingCourseActivityResponse event : events) {
                if (dropIfCrossTenant(userId, userTenantId, event.getCourseId(), "event")) {
                    continue;
                }
                event.setTimezone(tenantTimezoneService.requireTimezoneIdForCourse(event.getCourseId()));
                result.add(event);
            }
        }

        List<SessionWithCourseCode> sessions = courseSessionMapper.selectByUserActiveEnrollments(userId);
        if (sessions != null && !sessions.isEmpty()) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                String dayCode = DAY_CODES.get(d.getDayOfWeek());
                for (SessionWithCourseCode session : sessions) {
                    if (session.getDayOfWeek() == null || !session.getDayOfWeek().equals(dayCode)) {
                        continue;
                    }
                    if (dropIfCrossTenant(userId, userTenantId, session.getCourseId(), "session")) {
                        continue;
                    }
                    UpcomingCourseActivityResponse item = new UpcomingCourseActivityResponse();
                    item.setCourseId(session.getCourseId());
                    item.setCourseCode(session.getCourseCode());
                    item.setType(session.getType());
                    item.setTitle(session.getType());
                    item.setDate(d);
                    item.setStartTime(session.getStartTime());
                    item.setEndTime(session.getEndTime());
                    item.setLocation(session.getLocation());
                    item.setSource(UpcomingCourseActivityResponse.SOURCE_SESSION);
                    item.setSourceId(session.getId());
                    item.setTimezone(tenantTimezoneService.requireTimezoneIdForCourse(session.getCourseId()));
                    result.add(item);
                }
            }
        }

        result.sort(Comparator
                .comparing(UpcomingCourseActivityResponse::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(UpcomingCourseActivityResponse::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(UpcomingCourseActivityResponse::getCourseId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(UpcomingCourseActivityResponse::getSourceId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    private boolean dropIfCrossTenant(Integer userId, Integer userTenantId, Integer courseId, String kind) {
        if (courseId == null) {
            return true;
        }
        Course course = courseMapper.selectById(courseId);
        if (course == null || course.getTenantId() == null) {
            log.error("Dashboard {} dropped: missing course/tenant userId={} courseId={} userTenantId={}",
                    kind, userId, courseId, userTenantId);
            return true;
        }
        if (!userTenantId.equals(course.getTenantId())) {
            log.error("Dashboard {} cross-tenant filtered userId={} courseId={} userTenantId={} courseTenantId={}",
                    kind, userId, courseId, userTenantId, course.getTenantId());
            return true;
        }
        return false;
    }

    private int normalizeDays(Integer days, int defaultDays, int maxDays) {
        if (days == null || days < 1) {
            return defaultDays;
        }
        return Math.min(days, maxDays);
    }

    public CourseEventResponse getById(Integer courseId, Integer eventId) {
        return toResponse(requireEventInCourse(courseId, eventId));
    }

    @Transactional
    public CourseEventResponse create(ActorContext actor, Integer courseId, CreateCourseEventRequest request) {
        courseContentAccessService.requireCourseEventWritable(actor, courseId);
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

    @Transactional
    public CourseEventResponse update(ActorContext actor, Integer courseId, Integer eventId, UpdateCourseEventRequest request) {
        courseContentAccessService.requireCourseEventWritable(actor, courseId);
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

    @Transactional
    public void delete(ActorContext actor, Integer courseId, Integer eventId) {
        courseContentAccessService.requireCourseEventWritable(actor, courseId);
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
        response.setTimezone(tenantTimezoneService.requireTimezoneIdForCourse(event.getCourseId()));
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
