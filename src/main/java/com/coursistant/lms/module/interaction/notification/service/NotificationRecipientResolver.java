package com.coursistant.lms.module.interaction.notification.service;

import com.coursistant.lms.module.course.course.entity.Course;
import com.coursistant.lms.module.course.course.repository.CourseMapper;
import com.coursistant.lms.module.course.enrollment.entity.Enrollment;
import com.coursistant.lms.module.course.enrollment.repository.EnrollmentMapper;
import com.coursistant.lms.module.user.account.entity.User;
import com.coursistant.lms.module.user.account.repository.UserMapper;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves / filters notification recipients in the business transaction.
 * Active Student only; course not archived; user exists; tenant matches course.
 * Fail-open: mapper/DB failures return empty recipients so core business TX still commits.
 */
@Component
public class NotificationRecipientResolver {

    private static final Logger log = LoggerFactory.getLogger(NotificationRecipientResolver.class);

    @Resource
    private CourseMapper courseMapper;

    @Resource
    private EnrollmentMapper enrollmentMapper;

    @Resource
    private UserMapper userMapper;

    public List<Integer> resolveActiveStudentRecipients(Integer courseId) {
        try {
            return resolveActiveStudentRecipientsInternal(courseId);
        } catch (Exception e) {
            log.warn("Notification recipient resolve failed; skipping notifications. courseId={}",
                    courseId, e);
            return Collections.emptyList();
        }
    }

    public List<Integer> filterCandidateRecipients(Course course, List<Integer> candidateIds) {
        try {
            return filterCandidateRecipientsInternal(course, candidateIds);
        } catch (Exception e) {
            Integer courseId = course != null ? course.getId() : null;
            int candidateSize = candidateIds == null ? 0 : candidateIds.size();
            log.warn("Notification recipient filter failed; skipping notifications. courseId={} candidateSize={}",
                    courseId, candidateSize, e);
            return Collections.emptyList();
        }
    }

    private List<Integer> resolveActiveStudentRecipientsInternal(Integer courseId) {
        if (courseId == null) {
            return Collections.emptyList();
        }
        Course course = courseMapper.selectById(courseId);
        if (!isEligibleCourse(course)) {
            return Collections.emptyList();
        }
        List<Enrollment> students = enrollmentMapper.selectActiveStudentsByCourseId(courseId);
        if (students == null || students.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> candidateIds = new ArrayList<>();
        for (Enrollment enrollment : students) {
            if (enrollment != null && enrollment.getUserId() != null) {
                candidateIds.add(enrollment.getUserId());
            }
        }
        return filterCandidateRecipientsInternal(course, candidateIds);
    }

    private List<Integer> filterCandidateRecipientsInternal(Course course, List<Integer> candidateIds) {
        if (!isEligibleCourse(course) || candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> uniqueIds = new HashSet<>();
        List<Integer> orderedUnique = new ArrayList<>();
        for (Integer id : candidateIds) {
            if (id == null) {
                continue;
            }
            if (uniqueIds.add(id)) {
                orderedUnique.add(id);
            }
        }
        if (orderedUnique.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Integer> activeStudentIds = new HashSet<>();
        List<Enrollment> students = enrollmentMapper.selectActiveStudentsByCourseId(course.getId());
        if (students != null) {
            for (Enrollment enrollment : students) {
                if (enrollment != null && enrollment.getUserId() != null) {
                    activeStudentIds.add(enrollment.getUserId());
                }
            }
        }

        List<User> users = userMapper.selectUsersByIds(orderedUnique);
        Set<Integer> tenantMatched = new HashSet<>();
        if (users != null) {
            for (User user : users) {
                if (user == null || user.getId() == null) {
                    continue;
                }
                if (course.getTenantId().equals(user.getTenantId())) {
                    tenantMatched.add(user.getId());
                }
            }
        }

        List<Integer> result = new ArrayList<>();
        for (Integer id : orderedUnique) {
            if (activeStudentIds.contains(id) && tenantMatched.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    private boolean isEligibleCourse(Course course) {
        return course != null
                && course.getId() != null
                && course.getTenantId() != null
                && course.getArchivedAt() == null;
    }
}
