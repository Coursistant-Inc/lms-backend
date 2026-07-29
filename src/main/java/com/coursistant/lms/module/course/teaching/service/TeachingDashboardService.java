package com.coursistant.lms.module.course.teaching.service;

import com.coursistant.lms.module.course.teaching.dto.TeachingActivityResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingCourseResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingCourseRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingDeadlineResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingDeadlineRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingGradingQueueItemResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingGradingQueueRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingRecentActivityResponse;
import com.coursistant.lms.module.course.teaching.dto.TeachingRecentActivityRow;
import com.coursistant.lms.module.course.teaching.dto.TeachingSessionRow;
import com.coursistant.lms.module.course.teaching.repository.TeachingDashboardMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import com.coursistant.lms.shared.enums.LevelEnum;
import com.coursistant.lms.shared.util.TimeZoneUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class TeachingDashboardService {

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

    @Value("${lms.institution-timezone}")
    private String institutionTimezone;

    private ZoneId institutionZone;

    @Resource
    private UserMapper userMapper;

    @Resource
    private TeachingDashboardMapper teachingDashboardMapper;

    @PostConstruct
    void initInstitutionZone() {
        if (institutionTimezone == null || institutionTimezone.isBlank()) {
            throw new IllegalStateException("lms.institution-timezone must be a non-blank IANA timezone");
        }
        try {
            institutionZone = ZoneId.of(institutionTimezone.trim());
        } catch (DateTimeException ex) {
            throw new IllegalStateException(
                    "lms.institution-timezone is not a valid IANA timezone: " + institutionTimezone, ex);
        }
    }

    public List<TeachingCourseResponse> listCourses(Integer userId) {
        requireInstructorLevel(userId);
        return loadTeachingCourses(userId).stream().map(this::toCourseResponse).collect(Collectors.toList());
    }

    public List<TeachingGradingQueueItemResponse> listGradingQueue(Integer userId) {
        requireInstructorLevel(userId);
        List<Integer> courseIds = teachingCourseIds(userId);
        if (courseIds.isEmpty()) {
            return List.of();
        }

        List<TeachingGradingQueueRow> rows = new ArrayList<>();
        addAll(rows, teachingDashboardMapper.selectAssignmentUngradedIndividual(courseIds));
        addAll(rows, teachingDashboardMapper.selectAssignmentUngradedGroup(courseIds));
        addAll(rows, teachingDashboardMapper.selectAssignmentAwaitingRelease(courseIds));
        addAll(rows, teachingDashboardMapper.selectQuizManualPending(courseIds));
        addAll(rows, teachingDashboardMapper.selectQuizAwaitingRelease(courseIds));

        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        String tz = institutionZone.getId();
        List<TeachingGradingQueueItemResponse> result = new ArrayList<>();
        for (TeachingGradingQueueRow row : rows) {
            if (row.getPendingCount() == null || row.getPendingCount() <= 0) {
                continue;
            }
            TeachingGradingQueueItemResponse item = new TeachingGradingQueueItemResponse();
            item.setKind(row.getKind());
            item.setCourseId(row.getCourseId());
            item.setCourseCode(row.getCourseCode());
            item.setTitle(row.getTitle());
            item.setPendingCount(row.getPendingCount());
            LocalDateTime oldestLocal = toInstitution(row.getOldestWaitingAt());
            item.setOldestWaitingAt(oldestLocal);
            item.setWaitingMinutes(waitingMinutes(row.getOldestWaitingAt(), nowUtc));
            item.setTimezone(tz);
            item.setAssignmentId(row.getAssignmentId());
            item.setQuizId(row.getQuizId());
            result.add(item);
        }
        result.sort(Comparator
                .comparing(TeachingGradingQueueItemResponse::getOldestWaitingAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TeachingGradingQueueItemResponse::getCourseId,
                        Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public List<TeachingActivityResponse> listUpcomingActivities(Integer userId, Integer days) {
        requireInstructorLevel(userId);
        List<Integer> courseIds = teachingCourseIds(userId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        int windowDays = normalizeDays(days, 7, 30);
        LocalDate from = LocalDate.now(institutionZone);
        LocalDate to = from.plusDays(windowDays - 1L);
        String tz = institutionZone.getId();

        List<TeachingActivityResponse> result = new ArrayList<>();
        List<TeachingActivityResponse> events =
                teachingDashboardMapper.selectEventsInWindow(courseIds, from, to);
        if (events != null) {
            for (TeachingActivityResponse event : events) {
                event.setTimezone(tz);
                result.add(event);
            }
        }

        List<TeachingSessionRow> sessions = teachingDashboardMapper.selectSessionsByCourseIds(courseIds);
        if (sessions != null) {
            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                String dayCode = DAY_CODES.get(d.getDayOfWeek());
                for (TeachingSessionRow session : sessions) {
                    if (session.getDayOfWeek() == null || !session.getDayOfWeek().equals(dayCode)) {
                        continue;
                    }
                    if (session.getTermStartDate() != null && d.isBefore(session.getTermStartDate())) {
                        continue;
                    }
                    if (session.getTermEndDate() != null && d.isAfter(session.getTermEndDate())) {
                        continue;
                    }
                    TeachingActivityResponse item = new TeachingActivityResponse();
                    item.setCourseId(session.getCourseId());
                    item.setCourseCode(session.getCourseCode());
                    item.setType(session.getType());
                    item.setTitle(session.getType());
                    item.setDate(d);
                    item.setStartTime(session.getStartTime());
                    item.setEndTime(session.getEndTime());
                    item.setLocation(session.getLocation());
                    item.setSource(TeachingActivityResponse.SOURCE_SESSION);
                    item.setSourceId(session.getId());
                    item.setTimezone(tz);
                    result.add(item);
                }
            }
        }

        result.sort(Comparator
                .comparing(TeachingActivityResponse::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TeachingActivityResponse::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TeachingActivityResponse::getCourseId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TeachingActivityResponse::getSourceId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public List<TeachingDeadlineResponse> listUpcomingDeadlines(Integer userId, Integer days) {
        requireInstructorLevel(userId);
        List<Integer> courseIds = teachingCourseIds(userId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        int windowDays = normalizeDays(days, 14, 30);
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime toUtc = nowUtc.plusDays(windowDays);
        String tz = institutionZone.getId();

        List<TeachingDeadlineRow> rows = new ArrayList<>();
        addAll(rows, teachingDashboardMapper.selectAssignmentDeadlines(courseIds, nowUtc, toUtc));
        addAll(rows, teachingDashboardMapper.selectQuizDeadlines(courseIds, nowUtc, toUtc));

        List<TeachingDeadlineResponse> result = new ArrayList<>();
        for (TeachingDeadlineRow row : rows) {
            TeachingDeadlineResponse item = new TeachingDeadlineResponse();
            item.setKind(row.getKind());
            item.setCourseId(row.getCourseId());
            item.setCourseCode(row.getCourseCode());
            item.setTitle(row.getTitle());
            item.setAtLocal(toInstitution(row.getAtUtc()));
            item.setTimezone(tz);
            item.setSubmittedCount(row.getSubmittedCount() == null ? 0 : row.getSubmittedCount());
            item.setTotalStudents(row.getTotalStudents() == null ? 0 : row.getTotalStudents());
            item.setAssignmentId(row.getAssignmentId());
            item.setQuizId(row.getQuizId());
            result.add(item);
        }
        result.sort(Comparator
                .comparing(TeachingDeadlineResponse::getAtLocal, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TeachingDeadlineResponse::getCourseId, Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }

    public List<TeachingRecentActivityResponse> listRecentActivity(Integer userId, Integer limit) {
        requireInstructorLevel(userId);
        List<Integer> courseIds = teachingCourseIds(userId);
        if (courseIds.isEmpty()) {
            return List.of();
        }
        int lim = normalizeLimit(limit, 10, 50);
        // Fetch enough from each source then merge (each query uses lim).
        List<TeachingRecentActivityRow> rows = new ArrayList<>();
        addAll(rows, teachingDashboardMapper.selectGroupMembershipChanges(courseIds, lim));
        addAll(rows, teachingDashboardMapper.selectLateSubmissions(courseIds, lim));

        String tz = institutionZone.getId();
        List<TeachingRecentActivityResponse> result = new ArrayList<>();
        for (TeachingRecentActivityRow row : rows) {
            TeachingRecentActivityResponse item = new TeachingRecentActivityResponse();
            item.setKind(row.getKind());
            item.setCourseId(row.getCourseId());
            item.setCourseCode(row.getCourseCode());
            item.setSummary(row.getSummary());
            item.setOccurredAt(toInstitution(row.getOccurredAt()));
            item.setTimezone(tz);
            item.setAssignmentId(row.getAssignmentId());
            item.setGroupSetId(row.getGroupSetId());
            item.setGroupId(row.getGroupId());
            item.setTargetUserId(row.getTargetUserId());
            result.add(item);
        }
        result.sort(Comparator
                .comparing(TeachingRecentActivityResponse::getOccurredAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        if (result.size() > lim) {
            return new ArrayList<>(result.subList(0, lim));
        }
        return result;
    }

    private void requireInstructorLevel(Integer userId) {
        if (userId == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new ApiException(ErrorType.UNAUTHORIZED);
        }
        if (!LevelEnum.INSTRUCTOR.level.equals(user.getLevel())) {
            throw new ApiException(ErrorType.ACCESS_DENIED, "Teacher Dashboard requires INSTRUCTOR level");
        }
    }

    private List<TeachingCourseRow> loadTeachingCourses(Integer userId) {
        List<TeachingCourseRow> rows = teachingDashboardMapper.selectTeachingCourses(userId);
        return rows == null ? List.of() : rows;
    }

    private List<Integer> teachingCourseIds(Integer userId) {
        return loadTeachingCourses(userId).stream()
                .map(TeachingCourseRow::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TeachingCourseResponse toCourseResponse(TeachingCourseRow row) {
        TeachingCourseResponse response = new TeachingCourseResponse();
        response.setId(row.getId());
        response.setCourseCode(row.getCourseCode());
        response.setTitle(row.getTitle());
        response.setRole(row.getRole());
        return response;
    }

    private LocalDateTime toInstitution(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return TimeZoneUtils.fromUtcLocalDateTime(utc, institutionZone);
    }

    private long waitingMinutes(LocalDateTime oldestUtc, LocalDateTime nowUtc) {
        if (oldestUtc == null || nowUtc == null) {
            return 0L;
        }
        long minutes = Duration.between(oldestUtc, nowUtc).toMinutes();
        return Math.max(0L, minutes);
    }

    private int normalizeDays(Integer days, int defaultDays, int maxDays) {
        if (days == null || days < 1) {
            return defaultDays;
        }
        return Math.min(days, maxDays);
    }

    private int normalizeLimit(Integer limit, int defaultLimit, int maxLimit) {
        if (limit == null || limit < 1) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }

    private static <T> void addAll(List<T> target, List<T> source) {
        if (source != null && !source.isEmpty()) {
            target.addAll(source);
        }
    }
}
