package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.course.enrollment.service.CoursePermissionService;
import com.coursistant.lms.module.interaction.notification.dto.NotificationResponse;
import com.coursistant.lms.module.interaction.notification.dto.NotificationSubjectRef;
import com.coursistant.lms.module.interaction.notification.enums.SubjectType;
import com.coursistant.lms.module.interaction.notification.repository.NotificationAvailabilityMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class NotificationAvailabilityChecker {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String NO_LONGER_AVAILABLE = "NO_LONGER_AVAILABLE";

    private static final String STATE_PUBLISHED = "Published";

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private NotificationAvailabilityMapper availabilityMapper;

    public void fill(Integer userId, List<NotificationResponse> items) {
        if (items == null || items.isEmpty() || userId == null) {
            return;
        }
        Map<Integer, Enrollment> enrollments = activeEnrollments(userId);
        Map<SubjectType, List<Integer>> idsByType = new HashMap<>();
        for (NotificationResponse item : items) {
            SubjectType type = parseSubjectType(item.getSubjectType());
            if (type == null || item.getSubjectId() == null) {
                continue;
            }
            idsByType.computeIfAbsent(type, ignored -> new ArrayList<>()).add(item.getSubjectId());
        }
        Map<SubjectType, Map<Integer, NotificationSubjectRef>> refs = loadRefs(idsByType);
        Set<Integer> userGroupIds = loadUserGroupIds(userId, idsByType.containsKey(SubjectType.ASSIGNMENT_SUBMISSION));

        for (NotificationResponse item : items) {
            item.setAvailability(availability(item, enrollments, refs, userGroupIds, userId));
        }
    }

    private String availability(NotificationResponse item, Map<Integer, Enrollment> enrollments,
                                Map<SubjectType, Map<Integer, NotificationSubjectRef>> refs,
                                Set<Integer> userGroupIds, Integer userId) {
        Enrollment enrollment = enrollments.get(item.getCourseId());
        if (enrollment == null) {
            return NO_LONGER_AVAILABLE;
        }
        SubjectType type = parseSubjectType(item.getSubjectType());
        if (type == null) {
            return NO_LONGER_AVAILABLE;
        }
        boolean staff = isStaff(enrollment);
        NotificationSubjectRef ref = refs.getOrDefault(type, Map.of()).get(item.getSubjectId());
        if (ref == null || ref.getCourseId() == null || !ref.getCourseId().equals(item.getCourseId())) {
            return NO_LONGER_AVAILABLE;
        }
        return switch (type) {
            case ANNOUNCEMENT, COURSE_EVENT, GROUP_SET -> AVAILABLE;
            case ASSIGNMENT, QUIZ, WEEK -> publishedOrStaff(ref, staff);
            case ASSIGNMENT_SUBMISSION -> submissionAvailable(ref, staff, userId, userGroupIds);
            case ASSIGNMENT_GRADE, QUIZ_GRADE -> NO_LONGER_AVAILABLE;
        };
    }

    private String publishedOrStaff(NotificationSubjectRef ref, boolean staff) {
        if (staff || STATE_PUBLISHED.equals(ref.getState())) {
            return AVAILABLE;
        }
        return NO_LONGER_AVAILABLE;
    }

    private String submissionAvailable(NotificationSubjectRef ref, boolean staff, Integer userId,
                                       Set<Integer> userGroupIds) {
        if (!staff && !STATE_PUBLISHED.equals(ref.getState())) {
            return NO_LONGER_AVAILABLE;
        }
        if (staff || userId.equals(ref.getOwnerUserId())
                || (ref.getGroupId() != null && userGroupIds.contains(ref.getGroupId()))) {
            return AVAILABLE;
        }
        return NO_LONGER_AVAILABLE;
    }

    private Map<SubjectType, Map<Integer, NotificationSubjectRef>> loadRefs(
            Map<SubjectType, List<Integer>> idsByType) {
        Map<SubjectType, Map<Integer, NotificationSubjectRef>> result = new HashMap<>();
        put(result, SubjectType.ANNOUNCEMENT, query(idsByType, SubjectType.ANNOUNCEMENT,
                availabilityMapper::selectAnnouncements));
        put(result, SubjectType.ASSIGNMENT, query(idsByType, SubjectType.ASSIGNMENT,
                availabilityMapper::selectAssignments));
        put(result, SubjectType.ASSIGNMENT_SUBMISSION, query(idsByType, SubjectType.ASSIGNMENT_SUBMISSION,
                availabilityMapper::selectSubmissions));
        put(result, SubjectType.QUIZ, query(idsByType, SubjectType.QUIZ, availabilityMapper::selectQuizzes));
        put(result, SubjectType.WEEK, query(idsByType, SubjectType.WEEK, availabilityMapper::selectWeeks));
        put(result, SubjectType.COURSE_EVENT, query(idsByType, SubjectType.COURSE_EVENT,
                availabilityMapper::selectCourseEvents));
        put(result, SubjectType.GROUP_SET, query(idsByType, SubjectType.GROUP_SET,
                availabilityMapper::selectGroupSets));
        return result;
    }

    private List<NotificationSubjectRef> query(Map<SubjectType, List<Integer>> idsByType, SubjectType type,
                                               java.util.function.Function<List<Integer>, List<NotificationSubjectRef>> fn) {
        List<Integer> ids = idsByType.get(type);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return fn.apply(ids);
    }

    private void put(Map<SubjectType, Map<Integer, NotificationSubjectRef>> target, SubjectType type,
                     List<NotificationSubjectRef> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<Integer, NotificationSubjectRef> map = new HashMap<>();
        for (NotificationSubjectRef row : rows) {
            if (row != null && row.getId() != null) {
                map.put(row.getId(), row);
            }
        }
        target.put(type, map);
    }

    private Map<Integer, Enrollment> activeEnrollments(Integer userId) {
        List<Enrollment> rows = enrollmentMapper.selectActiveByUserId(userId);
        Map<Integer, Enrollment> map = new HashMap<>();
        if (rows != null) {
            for (Enrollment enrollment : rows) {
                if (enrollment != null && enrollment.getCourseId() != null) {
                    map.put(enrollment.getCourseId(), enrollment);
                }
            }
        }
        return map;
    }

    private Set<Integer> loadUserGroupIds(Integer userId, boolean needed) {
        if (!needed) {
            return Set.of();
        }
        List<Integer> ids = availabilityMapper.selectGroupIdsForUser(userId);
        return ids == null ? Set.of() : new HashSet<>(ids);
    }

    private boolean isStaff(Enrollment enrollment) {
        String role = enrollment.getCourseRole();
        return CoursePermissionService.ROLE_INSTRUCTOR.equals(role)
                || CoursePermissionService.ROLE_TA.equals(role);
    }

    private SubjectType parseSubjectType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SubjectType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
