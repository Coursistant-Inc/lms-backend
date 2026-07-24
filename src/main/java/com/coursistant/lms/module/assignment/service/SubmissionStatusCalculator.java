package com.coursistant.lms.module.assignment.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * Derives submission state from timestamps alone. There is deliberately no {@code late} column
 * and no {@code submission_status} column in the schema: everything here is recomputed from
 * {@code dueAt}, {@code lateUntil}, the submitted version, and the student's active staging files.
 *
 * <p>Grace buffer: a student who had uploaded (staged) at least one file before the due date may
 * still press Submit for up to {@value #GRACE_MINUTES} minutes after it, and the resulting version
 * is <em>not</em> late. The grace buffer applies to the {@code dueAt} boundary only — it never
 * extends a configured {@code lateUntil} window.</p>
 */
@Component
public class SubmissionStatusCalculator {

    public static final int GRACE_MINUTES = 5;

    public static final String NOT_SUBMITTED = "NotSubmitted";
    public static final String SUBMITTED = "Submitted";
    public static final String SUBMITTED_LATE = "SubmittedLate";
    public static final String NOT_SUBMITTED_CLOSED = "NotSubmittedClosed";

    /**
     * Status of a student who has no submitted version yet.
     */
    public String calculateForNoSubmission(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now,
                                           Collection<LocalDateTime> activeStagingCreatedAts) {
        if (isWindowOpen(dueAt, lateUntil, now) || isGraceEligible(dueAt, lateUntil, now, activeStagingCreatedAts)) {
            return NOT_SUBMITTED;
        }
        return NOT_SUBMITTED_CLOSED;
    }

    /**
     * Status of an existing submitted version.
     */
    public String calculateForVersion(LocalDateTime dueAt, LocalDateTime submittedAt, Boolean usedGraceBuffer) {
        if (submittedAt == null) {
            return NOT_SUBMITTED;
        }
        if (Boolean.TRUE.equals(usedGraceBuffer)) {
            return SUBMITTED;
        }
        if (dueAt == null || !submittedAt.isAfter(dueAt)) {
            return SUBMITTED;
        }
        return SUBMITTED_LATE;
    }

    /**
     * Single entry point used by the assemblers: {@code submittedAt} null means "no version yet".
     */
    public String calculate(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now,
                            LocalDateTime submittedAt, Boolean usedGraceBuffer,
                            Collection<LocalDateTime> activeStagingCreatedAts) {
        if (submittedAt == null) {
            return calculateForNoSubmission(dueAt, lateUntil, now, activeStagingCreatedAts);
        }
        return calculateForVersion(dueAt, submittedAt, usedGraceBuffer);
    }

    /**
     * The normal submission window: up to {@code lateUntil} when configured, otherwise {@code dueAt}.
     */
    public boolean isWindowOpen(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now) {
        LocalDateTime windowEnd = lateUntil != null ? lateUntil : dueAt;
        if (windowEnd == null || now == null) {
            return false;
        }
        return !now.isAfter(windowEnd);
    }

    /**
     * True while the clock is inside the post-due grace buffer, regardless of what any student has
     * staged. Callers use this to skip per-student staging lookups outside the buffer.
     */
    public boolean isWithinGraceWindow(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now) {
        if (dueAt == null || now == null || lateUntil != null) {
            return false;
        }
        return now.isAfter(dueAt) && !now.isAfter(dueAt.plusMinutes(GRACE_MINUTES));
    }

    /**
     * True while the student is inside the post-due grace buffer and had staged a file before the
     * due date. Never applies when {@code lateUntil} is configured.
     */
    public boolean isGraceEligible(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now,
                                   Collection<LocalDateTime> activeStagingCreatedAts) {
        if (dueAt == null || now == null || lateUntil != null) {
            return false;
        }
        if (!now.isAfter(dueAt)) {
            return false;
        }
        if (now.isAfter(dueAt.plusMinutes(GRACE_MINUTES))) {
            return false;
        }
        LocalDateTime earliestStaging = earliest(activeStagingCreatedAts);
        return earliestStaging != null && !earliestStaging.isAfter(dueAt);
    }

    /**
     * Whether a Submit call should be accepted right now.
     */
    public boolean acceptSubmit(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now,
                                Collection<LocalDateTime> activeStagingCreatedAts) {
        return isWindowOpen(dueAt, lateUntil, now)
                || isGraceEligible(dueAt, lateUntil, now, activeStagingCreatedAts);
    }

    /**
     * Whether the version created by a Submit accepted at {@code now} must be flagged as having
     * consumed the grace buffer.
     */
    public boolean consumesGraceBuffer(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now,
                                       Collection<LocalDateTime> activeStagingCreatedAts) {
        return !isWindowOpen(dueAt, lateUntil, now)
                && isGraceEligible(dueAt, lateUntil, now, activeStagingCreatedAts);
    }

    /**
     * Remaining seconds of the grace buffer, or 0 when it is not active.
     */
    public long graceSecondsRemaining(LocalDateTime dueAt, LocalDateTime lateUntil, LocalDateTime now,
                                      Collection<LocalDateTime> activeStagingCreatedAts) {
        if (!isGraceEligible(dueAt, lateUntil, now, activeStagingCreatedAts)) {
            return 0L;
        }
        return Math.max(0L, Duration.between(now, dueAt.plusMinutes(GRACE_MINUTES)).getSeconds());
    }

    private LocalDateTime earliest(Collection<LocalDateTime> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        LocalDateTime min = null;
        for (LocalDateTime value : values) {
            if (value == null) {
                continue;
            }
            if (min == null || value.isBefore(min)) {
                min = value;
            }
        }
        return min;
    }
}
