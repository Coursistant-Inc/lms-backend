package com.coursistant.lms.module.quiz.service;

public final class QuizConstants {

    private QuizConstants() {
    }

    public static final String STATE_DRAFT = "Draft";
    public static final String STATE_PUBLISHED = "Published";

    public static final String TYPE_SINGLE_CHOICE = "SingleChoice";
    public static final String TYPE_MULTIPLE_SELECT = "MultipleSelect";
    public static final String TYPE_TRUE_FALSE = "TrueFalse";
    public static final String TYPE_SHORT_ANSWER = "ShortAnswer";

    public static final String VISIBILITY_AFTER_RELEASE = "AfterRelease";
    public static final String VISIBILITY_INSTANT_AUTO = "InstantAutoScore";

    public static final String ATTEMPT_IN_PROGRESS = "InProgress";
    public static final String ATTEMPT_FINALIZING = "Finalizing";
    public static final String ATTEMPT_SUBMITTED = "Submitted";

    public static final String CLOSE_MANUAL = "MANUAL";
    public static final String CLOSE_TIME_LIMIT = "TIME_LIMIT_REACHED";
    public static final String CLOSE_QUIZ_CLOSED = "QUIZ_CLOSED";
    public static final String CLOSE_COURSE_ARCHIVED = "COURSE_ARCHIVED";
    public static final String CLOSE_MEMBERSHIP_INELIGIBLE = "MEMBERSHIP_INELIGIBLE";

    public static final String GRADE_ENTERED = "Entered";
    public static final String GRADE_RELEASED = "Released";

    public static final String COURSE_ARCHIVED = "Archived";
}
