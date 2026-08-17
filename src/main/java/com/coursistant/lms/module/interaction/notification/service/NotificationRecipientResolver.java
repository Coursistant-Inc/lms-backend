package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.interaction.notification.enums.NotificationType;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import com.coursistant.lms.shared.enums.AccountStatus;
import com.coursistant.lms.shared.security.ActorContext;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves course-wide recipient snapshots at publish time.
 * {@link #resolveActiveStudentRecipients(Integer)} remains for Relay playback of leftover
 * Phase 1 {@code COURSE_ACTIVE_STUDENTS} outbox rows.
 */
@Component
public class NotificationRecipientResolver {

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private UserMapper userMapper;

    public static Integer userActorId(ActorContext actor) {
        if (actor == null || !actor.isUser()) {
            return null;
        }
        return actor.getActorId();
    }

    public List<Integer> resolveForType(NotificationType type, Integer courseId, Integer actorUserId) {
        NotificationPolicy.Mapping mapping = NotificationPolicy.forType(type);
        return switch (mapping.audience()) {
            case ACTIVE_STUDENTS -> resolveActiveStudentRecipients(courseId, actorUserId);
            case ALL_ACTIVE_COURSE_MEMBERS -> resolveAllActiveMemberRecipients(courseId, actorUserId);
            case PROVIDED_RECIPIENTS -> throw new IllegalArgumentException(
                    "Notification type " + type + " requires a producer-provided recipient snapshot");
        };
    }

    public List<Integer> resolveActiveStudentRecipients(Integer courseId) {
        return resolveActiveStudentRecipients(courseId, null);
    }

    public List<Integer> resolveActiveStudentRecipients(Integer courseId, Integer actorUserId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        Course course = courseMapper.selectById(courseId);
        if (!isResolvableCourse(course)) {
            return Collections.emptyList();
        }
        List<Enrollment> students = enrollmentMapper.selectActiveStudentsByCourseId(courseId);
        return filterEligible(course, collectUserIds(students), actorUserId, false);
    }

    public List<Integer> resolveAllActiveMemberRecipients(Integer courseId, Integer actorUserId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        Course course = courseMapper.selectById(courseId);
        if (!isResolvableCourse(course)) {
            return Collections.emptyList();
        }
        List<Enrollment> members = enrollmentMapper.selectActiveByCourseId(courseId);
        return filterEligible(course, collectUserIds(members), actorUserId, false);
    }

    /**
     * Relay-time filter for leftover COURSE_ACTIVE_STUDENTS rows: still requires current
     * active student enrollment plus matching tenant.
     */
    public List<Integer> filterCandidateRecipients(Course course, List<Integer> candidateIds) {
        return filterEligible(course, candidateIds, null, true);
    }

    private List<Integer> filterEligible(Course course, List<Integer> candidateIds, Integer actorUserId,
                                         boolean requireActiveStudent) {
        if (!isResolvableCourse(course) || candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> uniqueIds = new HashSet<>();
        List<Integer> orderedUnique = new ArrayList<>();
        for (Integer id : candidateIds) {
            if (id == null) {
                continue;
            }
            if (actorUserId != null && actorUserId.equals(id)) {
                continue;
            }
            if (uniqueIds.add(id)) {
                orderedUnique.add(id);
            }
        }
        if (orderedUnique.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> rosterIds = null;
        if (requireActiveStudent) {
            rosterIds = new HashSet<>();
            List<Enrollment> students = enrollmentMapper.selectActiveStudentsByCourseId(course.getId());
            if (students != null) {
                for (Enrollment enrollment : students) {
                    if (enrollment != null && enrollment.getUserId() != null) {
                        rosterIds.add(enrollment.getUserId());
                    }
                }
            }
        }

        List<User> users = userMapper.selectUsersByIds(orderedUnique);
        Set<Integer> eligible = new HashSet<>();
        if (users != null) {
            for (User user : users) {
                if (user == null || user.getId() == null) {
                    continue;
                }
                if (!course.getTenantId().equals(user.getTenantId())) {
                    continue;
                }
                if (user.getStatus() != null && !AccountStatus.ACTIVE.name().equals(user.getStatus())) {
                    continue;
                }
                eligible.add(user.getId());
            }
        }

        List<Integer> result = new ArrayList<>();
        for (Integer id : orderedUnique) {
            if (eligible.contains(id) && (rosterIds == null || rosterIds.contains(id))) {
                result.add(id);
            }
        }
        return result;
    }

    private List<Integer> collectUserIds(List<Enrollment> enrollments) {
        if (enrollments == null || enrollments.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> ids = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            if (enrollment != null && enrollment.getUserId() != null) {
                ids.add(enrollment.getUserId());
            }
        }
        return ids;
    }

    private boolean isResolvableCourse(Course course) {
        return course != null
                && course.getId() != null
                && course.getTenantId() != null;
    }
}
