package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class QuizErrors {

    private static final Logger LOG = LoggerFactory.getLogger(QuizErrors.class);

    private QuizErrors() {
    }

    public static ApiException fail(Integer courseId, Integer quizId, Integer userId,
                                    ErrorType errorType, String message) {
        LOG.error("Quiz operation failed: courseId={}, quizId={}, userId={}, errorType={}, detail={}",
                courseId, quizId, userId, errorType, message);
        return message == null ? new ApiException(errorType) : new ApiException(errorType, message);
    }
}
