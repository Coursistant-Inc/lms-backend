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

    public static final String WEEK_CREATED = "WEEK_CREATED";
    public static final String WEEK_UPDATED = "WEEK_UPDATED";
    public static final String WEEK_REORDERED = "WEEK_REORDERED";
    public static final String WEEK_PUBLISHED = "WEEK_PUBLISHED";
    public static final String WEEK_UNPUBLISHED = "WEEK_UNPUBLISHED";
    public static final String WEEK_DELETED = "WEEK_DELETED";

    public static final String MATERIAL_CREATED = "MATERIAL_CREATED";
    public static final String MATERIAL_UPDATED = "MATERIAL_UPDATED";
    public static final String MATERIAL_MOVED = "MATERIAL_MOVED";
    public static final String MATERIAL_REORDERED = "MATERIAL_REORDERED";
    public static final String MATERIAL_DELETED = "MATERIAL_DELETED";

    public static final String SYLLABUS_UPLOADED = "SYLLABUS_UPLOADED";
    public static final String SYLLABUS_RESTORED = "SYLLABUS_RESTORED";
    public static final String SYLLABUS_DELETED = "SYLLABUS_DELETED";

    public static final String SESSION_CREATED = "SESSION_CREATED";
    public static final String SESSION_UPDATED = "SESSION_UPDATED";
    public static final String SESSION_DELETED = "SESSION_DELETED";

    public static final String COURSE_EVENT_CREATED = "COURSE_EVENT_CREATED";
    public static final String COURSE_EVENT_UPDATED = "COURSE_EVENT_UPDATED";
    public static final String COURSE_EVENT_DELETED = "COURSE_EVENT_DELETED";

    public static final String TARGET_COURSE = "COURSE";
    public static final String TARGET_ENROLLMENT = "ENROLLMENT";
    public static final String TARGET_WEEK = "WEEK";
    public static final String TARGET_MATERIAL = "MATERIAL";
    public static final String TARGET_SYLLABUS = "SYLLABUS";
    public static final String TARGET_SESSION = "SESSION";
    public static final String TARGET_COURSE_EVENT = "COURSE_EVENT";

    private CourseAuditActions() {
    }
}
