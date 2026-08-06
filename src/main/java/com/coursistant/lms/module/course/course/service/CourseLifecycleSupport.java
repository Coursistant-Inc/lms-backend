package com.coursistant.lms.module.course.course.service;

import com.coursistant.lms.module.course.course.entity.Course;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Archive grading-grace helpers for Assignment/Quiz modules.
 * Grace window = archivedAt + {@link #GRADING_GRACE_DAYS} days (UTC).
 */
public final class CourseLifecycleSupport {

    public static final int GRADING_GRACE_DAYS = 30;
    public static final String STATE_ACTIVE = "Active";
    public static final String STATE_ARCHIVED = "Archived";

    private CourseLifecycleSupport() {
    }

    public static boolean isArchived(Course course) {
        return course != null && STATE_ARCHIVED.equals(course.getState());
    }

    public static LocalDateTime gradingGraceEndsAt(Course course) {
        if (course == null || course.getArchivedAt() == null) {
            return null;
        }
        return course.getArchivedAt().plusDays(GRADING_GRACE_DAYS);
    }

    public static boolean isWithinGradingGrace(Course course, LocalDateTime nowUtc) {
        LocalDateTime ends = gradingGraceEndsAt(course);
        if (ends == null) {
            return false;
        }
        LocalDateTime now = nowUtc != null ? nowUtc : LocalDateTime.now(ZoneOffset.UTC);
        return !now.isAfter(ends);
    }

    public static boolean allowsGradingWrite(Course course, LocalDateTime nowUtc) {
        if (course == null) {
            return false;
        }
        if (STATE_ACTIVE.equals(course.getState())) {
            return true;
        }
        return isWithinGradingGrace(course, nowUtc);
    }
}
