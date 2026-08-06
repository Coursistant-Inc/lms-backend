package com.coursistant.lms.module.course.course.service;

/** Canonical course_audit_log.action values for Part 2+. */
public final class CourseAuditActions {

    public static final String COURSE_CREATED = "COURSE_CREATED";
    public static final String COURSE_UPDATED = "COURSE_UPDATED";
    public static final String COURSE_ARCHIVED = "COURSE_ARCHIVED";
    public static final String COURSE_UNARCHIVED = "COURSE_UNARCHIVED";
    public static final String COURSE_DELETED = "COURSE_DELETED";
    public static final String PRIMARY_INSTRUCTOR_REASSIGNED = "PRIMARY_INSTRUCTOR_REASSIGNED";

    public static final String TARGET_COURSE = "COURSE";
    public static final String TARGET_ENROLLMENT = "ENROLLMENT";

    private CourseAuditActions() {
    }
}
