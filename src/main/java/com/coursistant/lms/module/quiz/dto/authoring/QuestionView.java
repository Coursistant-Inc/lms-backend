package com.coursistant.lms.module.quiz.dto.authoring;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Role-shaped quiz question view.
 * Instructors receive {@link QuestionResponse} (includes answer keys);
 * students receive {@link StudentQuestionResponse} (no answer-key fields).
 */
@Schema(
        name = "QuestionView",
        description = "Role-shaped question payload: QuestionResponse (instructor) or StudentQuestionResponse (student)",
        oneOf = {QuestionResponse.class, StudentQuestionResponse.class}
)
public interface QuestionView {
}
