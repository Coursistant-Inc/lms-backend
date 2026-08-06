package com.coursistant.lms.module.course.course.service;

/** Canonical course_audit_log.action values for Part 2+. */
public final class CourseAuditActions {

    public static final String COURSE_CREATED = "COURSE_CREATED";
    public static final String COURSE_UPDATED = "COURSE_UPDATED";
    public static final String COURSE_ARCHIVED = "COURSE_ARCHIVED";
    public static final String COURSE_UNARCHIVED = "COURSE_UNARCHIVED";
    public static final String COURSE_DELETED = "COURSE_DELETED";
    public static final String PRIMARY_INSTRUCTOR_REASSIGNED = "PRIMARY_INSTRUCTOR_REASSIGNED";

    public static final String STUDENT_ADDED = "STUDENT_ADDED";
    public static final String STUDENT_REACTIVATED = "STUDENT_REACTIVATED";
    public static final String STUDENT_WITHDRAWN = "STUDENT_WITHDRAWN";
    public static final String TA_ADDED = "TA_ADDED";
    public static final String TA_REACTIVATED = "TA_REACTIVATED";
    public static final String TA_REMOVED = "TA_REMOVED";
    public static final String TA_PERMISSIONS_UPDATED = "TA_PERMISSIONS_UPDATED";
    public static final String ENROLLMENT_ROLE_CHANGED = "ENROLLMENT_ROLE_CHANGED";
    public static final String ENROLLMENT_WITHDRAWN_BY_ACCOUNT_DISABLE = "ENROLLMENT_WITHDRAWN_BY_ACCOUNT_DISABLE";

    public static final String TARGET_COURSE = "COURSE";
    public static final String TARGET_ENROLLMENT = "ENROLLMENT";

    private CourseAuditActions() {
    }
}
