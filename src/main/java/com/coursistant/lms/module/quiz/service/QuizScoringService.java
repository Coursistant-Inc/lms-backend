package com.coursistant.lms.module.quiz.service;

import com.coursistant.lms.module.quiz.entity.QuizQuestion;
import com.coursistant.lms.module.quiz.entity.QuizQuestionOption;
import com.coursistant.lms.shared.api.ApiException;
import com.coursistant.lms.shared.api.ErrorType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuizScoringService {

    public BigDecimal scoreObjective(QuizQuestion question, List<Integer> selectedOptionIds,
                                     List<QuizQuestionOption> options) {
        if (selectedOptionIds == null) {
            selectedOptionIds = List.of();
        }
        Set<Integer> selected = new HashSet<>(selectedOptionIds);
        Set<Integer> correct = options.stream()
                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                .map(QuizQuestionOption::getId)
                .collect(Collectors.toSet());

        if (QuizConstants.TYPE_SHORT_ANSWER.equals(question.getType())) {
            return null;
        }

        if (QuizConstants.TYPE_SINGLE_CHOICE.equals(question.getType())
                || QuizConstants.TYPE_TRUE_FALSE.equals(question.getType())) {
            if (selected.size() != 1) {
                return BigDecimal.ZERO;
            }
            return selected.equals(correct) ? question.getPoints() : BigDecimal.ZERO;
        }

        if (QuizConstants.TYPE_MULTIPLE_SELECT.equals(question.getType())) {
            return selected.equals(correct) ? question.getPoints() : BigDecimal.ZERO;
        }

        return BigDecimal.ZERO;
    }

    public void validateAnswerPayload(String type, List<Integer> selectedOptionIds, String textAnswer) {
        if (QuizConstants.TYPE_SHORT_ANSWER.equals(type)) {
            if (selectedOptionIds != null && !selectedOptionIds.isEmpty()) {
                throw new ApiException(ErrorType.QUIZ_ANSWER_INVALID, "Short answer cannot include option ids");
            }
            return;
        }
        if (textAnswer != null && !textAnswer.isBlank()) {
            throw new ApiException(ErrorType.QUIZ_ANSWER_INVALID, "Objective questions cannot include text answer");
        }
        if (selectedOptionIds == null || selectedOptionIds.isEmpty()) {
            throw new ApiException(ErrorType.QUIZ_ANSWER_INVALID, "Option selection required");
        }
    }
}
