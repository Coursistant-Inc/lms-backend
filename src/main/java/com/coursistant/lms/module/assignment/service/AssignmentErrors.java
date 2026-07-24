package com.coursistant.lms.module.assignment.service;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.slf4j.Logger;

/**
 * Builds the module's {@link ApiException}s while emitting a uniform ERROR log line that always
 * carries courseId / assignmentId / userId / errorType, so a failure can be traced without
 * having to correlate several log statements.
 */
public final class AssignmentErrors {

    private AssignmentErrors() {
    }

    public static ApiException fail(Logger log, Integer courseId, Integer assignmentId, Integer userId,
                                    ErrorType errorType, String message) {
        log.error("Assignment operation failed: courseId={}, assignmentId={}, userId={}, errorType={}, detail={}",
                courseId, assignmentId, userId, errorType, message);
        return message == null ? new ApiException(errorType) : new ApiException(errorType, message);
    }

    public static ApiException fail(Logger log, Integer courseId, Integer assignmentId, Integer userId,
                                    ErrorType errorType) {
        return fail(log, courseId, assignmentId, userId, errorType, null);
    }
}
